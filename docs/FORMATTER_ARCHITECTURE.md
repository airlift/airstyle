# Formatter Architecture

Airstyle is a single fixed Java code style with zero configuration. The formatter is an IntelliJ-inspired block-tree layout engine plus a small set of pre- and post-format normalizers that handle concerns the engine doesn't own (imports, modifier order, brace collapse, etc.).

For the style itself — what the output looks like — see [FORMATTER_STYLE.md](FORMATTER_STYLE.md). For workflow rules, see [AGENTS.md](../AGENTS.md).

## Pipeline

`AirstyleFormatter.format()` runs up to three stabilization rounds of:

```
source
  → PRE_FORMAT  (imports, modifiers, braces, literals, wrapped-list argument canonicalization)
  → FORMAT      (engine: single phase, produces final indent + spacing)
  → POST_FORMAT (brace/layout fixups, cleanup, comment/Javadoc normalization)
  → formatted source
```

Each round goes through one full pipeline. The loop stops when a round's output equals its input, or after three rounds. Any round that produces unparseable output triggers a fallback: `format()` returns the original source unchanged.

## Engine (FORMAT stage)

The engine lives in `io.airlift.airstyle.engine`. It's a simplified port of IntelliJ's `com.intellij.formatting` package.

1. **`JavaBlockBuilder`** owns compilation-unit orchestration, shared source/token helpers, and delegation into construct builders. Each builder produces a `Block` tree whose nodes declare their source range, `Indent`, and pairwise `Spacing`.
2. **`InitialInfoBuilder`** flattens the tree into a doubly-linked list of `LeafBlockWrapper`s plus a parallel tree of `CompositeBlockWrapper`s.
3. **`AdjustWhiteSpacesState`** sweeps leaves left-to-right: applies each leaf's `Spacing`, then asks `IndentAdjuster` to re-place it.
4. **`IndentAdjuster`** calls the leaf's parent `getChildOffset(leaf, leafStart)`, which recursively walks up the wrapper tree summing indent contributions until it hits an anchor.
5. **`ExpandChildrenIndentState`** resolves any `ExpandableIndent` groups that depend on other groups' final columns.
6. **`ApplyChangesState`** emits the final string — for each leaf, `whitespace.render() + leafText`.

Airstyle has no right margin and no `Wrap` type. The engine never chops for line-length; wrapping is entirely source-shape-driven. If a user had two arguments on one line, the engine keeps them inline; if they were on separate lines, the engine keeps them separate. The only line-break decisions the engine inserts are the ones a construct demands (a brace opening on its own line, a new `throws` clause on a continuation line, etc.), and those are encoded as `Spacing.minLineFeeds`.

## `Indent` vocabulary

| Factory | Behavior |
|---|---|
| `Indent.noneIndent()` | Inherits parent column unchanged. Default for composite wrappers. |
| `Indent.normalIndent()` | Parent column + 4. Method bodies, class members, statement blocks. |
| `Indent.continuationIndent()` | Parent column + 8. Wrapped argument items, chain selectors, continuation lines. |
| `Indent.absoluteSpaceIndent(col)` | Place the child at exactly column `col`, bypassing the parent walk. |
| `Indent.relativeSpaceIndent(n)` / `normalIndent(true)` / `continuationIndent(true)` | Anchor to the *direct* parent block's column rather than the recursive walk (used for chain selectors and lambda anchors). |

`INDENTATION_SIZE = 4` and `CONTINUATION_INDENT_SIZE = 8` on `AirstyleFormatter`.

## `Spacing`

Every pair of sibling blocks has a `Spacing(minSpaces, maxSpaces, minLineFeeds, keepLineBreaks, keepBlankLines)`. A leaf's effective whitespace is computed from its spacing-before and the state of its predecessor. Source whitespace is clamped to the spacing's envelope: if source had 3 blank lines and `keepBlankLines=1`, the output gets 1; if `minLineFeeds=1` and source had none, a newline is inserted.

## Normalizer Boundaries

Normalizers are for source canonicalization, not missing indentation rules. If a phase only compensates for a missing block-tree shape, move that behavior into the engine. If a phase inserts, deletes, reorders, or canonicalizes tokens before layout, keep it before the engine and make the engine responsible for final spacing and indentation.

Pre-format normalizers handle source-level rewrites: imports (redundant, unused, star, static rule, organize), modifier order, declaration cleanups, explicit initialization, literal case, brace-on-own-line insertion (`NeedBraces`), empty statement removal, array/list canonicalization, enum canonicalization, and other AST-preserving source-shape changes. They must produce valid Java that the engine then lays out.

These phases intentionally remain pre-format source canonicalizers rather than builder logic:

- `WrappedListNormalizer` decides which list items are standalone and emits valid source for the engine to indent.
- `ArrayInitializerBlankLineNormalizer` canonicalizes array separators and required trailing commas before literal layout.
- `SwitchRuleNormalizer` canonicalizes source-order cases that require token movement, such as standalone comments between `->` and the rule body.
- `TypeHierarchyClauseNormalizer`, `GenericTypeArgumentNormalizer`, and `EnumDeclarationNormalizer` canonicalize declaration source shape before structural layout.

Post-format normalizers must stay narrow and comment/literal-specific. Text-block margin alignment is engine-owned: text block tokens are split into structural line leaves in `JavaTokenRunBuilder`, mirroring IntelliJ's `TextBlockBlock`, so content and closing-delimiter indentation is handled by normal `Indent` and `Spacing` rules. Javadoc and markdown rewrites belong in comment formatting code, not `JavaBlockBuilder`. `formatCleanup` remains a safety net for trailing whitespace, tabs in whitespace, repeated spaces, block-boundary blank lines, and final newline handling until each concern is covered structurally.

Comment indentation is engine-owned; do not add a post-format comment indentation cleanup pass.

### Where a change belongs

| Change type | Home |
|---|---|
| Token insertion, deletion, reordering, or canonicalization before layout | `PRE_FORMAT` normalizer, backed by AST/tokens and valid-Java output |
| Indentation, alignment, comments between Java constructs, or construct-local line breaks | `FORMAT` engine block tree |
| Pairwise token spacing that applies generally | `JavaSpacingPolicy` or `JavaSpacingRules` |
| Pairwise spacing that depends on a specific construct's child shape | The smallest relevant construct builder |
| Text-block content margin | Structural text-block line leaves in `JavaTokenRunBuilder` |
| Javadoc content normalization or final whitespace cleanup | Narrow `POST_FORMAT` normalizer or leaf rendering |

Before adding a normalizer, check whether the desired output can be represented as children, `Indent`, and `Spacing`. If yes, build that structure in the engine. A normalizer is appropriate only when it changes the source token stream or protects literal/comment content that the block tree cannot safely rewrite.

## Data-loss hazard: uncovered source ranges

If a `buildX` method emits children whose combined ranges don't cover the full parent range, any non-whitespace gap is outside the indent model and can drop comments or punctuation. `InitialInfoBuilder` validates block trees before formatting: child ranges must be ordered, inside the parent, and any uncovered gap must be whitespace-only. This makes uncovered non-whitespace ranges fail fast during tests.

When writing a new `buildX`, verify every non-whitespace token in the range is emitted by some leaf. Whitespace-only childless blocks are treated as phantom leaves and skipped by `InitialInfoBuilder`; do not rely on them for layout.

## Builder Organization

The Java formatter is split by construct family:

- `JavaBlockBuilder` handles compilation-unit assembly, imports, top-level comments, compatibility delegation, and root orchestration. New construct-specific formatting logic should not be added here.
- `JavaFormatMetadata` performs AST pre-scans that are shared across builders, such as generic opening brackets, switch guards, and wrapped annotations.
- `JavaSourceContext` owns source text, token lookup, and range helpers shared by construct builders.
- `JavaSourceRange` names owned source spans so token-run calls distinguish deliberate flattened fragments from true leaf ranges.
- `JavaBlockFactory` and `JavaConstructBuilder` provide the small abstraction layer used by extracted builders.
- `JavaDeclarationBuilder` handles type, record, enum, annotation type, field, initializer, method, method-header, throws, and record-component structure.
- `JavaStatementBuilder` handles statement blocks, local types, control-flow statements, try/catch/finally, switch statements/expressions, and statement-level wrappers around expressions.
- `JavaExpressionBuilder` handles expression dispatch, call/list arguments, annotation values, array initializers, operators, lambdas, and text-block RHS layout.
- `JavaChainExpressionBuilder` handles method-chain selector detection, constructor selector chunks, and chain-specific selector indentation helpers. Chain block assembly still lives in `JavaExpressionBuilder`; moving that assembly into the chain builder is a remaining cleanup target.
- `JavaLoopConstructBuilder` is the extracted loop/header builder used by statement formatting.
- `JavaConstructSpacing` contains reusable construct-local spacing helpers.
- `JavaSpacingPolicy` applies metadata-aware spacing overrides on top of `JavaSpacingRules`.
- `JavaTokenRunBuilder` is the leaf-only token-run builder. It is safe for punctuation, keywords, and single-line token sequences, but multiline expressions or child-owning constructs should route through structural builders so indentation has a block model.

Token runs must be created from explicit source ranges. Use owned structural ranges when a token sequence is a deliberately flattened fragment of an AST node, and leaf ranges only for punctuation, keywords, comments, or other true leaf token sequences. Do not pass arbitrary parent spans to the token-run builder just because a structural builder is missing.

`JavaExpressionBuilder` is intentionally the next cleanup target: it owns expression dispatch plus call arguments, operator expressions, array initializers, literals, lambdas, text-block RHS layout, and the remaining chain assembly. Split it further before adding more expression behavior, with separate builders for call/list arguments, operator expressions, chain block assembly, and literal/lambda constructs.

## Architecture Direction

Airstyle's formatter should continue moving toward IntelliJ's separation of responsibilities without taking IntelliJ as a dependency or copying its settings surface.

- New formatter logic should go into the smallest relevant construct builder, not into `JavaBlockBuilder`.
- Chain formatting is still hand-rolled. Keep chain decisions aligned with a receiver/selector block shape: split receivers/selectors into explicit chunks, keep selector indentation on structural anchors, and avoid source-column heuristics where a real anchor exists.
- Spacing policy is split between `JavaSpacingRules`, construct-specific spacing helpers, and ad hoc overrides in builders. Prefer moving repeated adjacency decisions toward `JavaSpacingPolicy`.
- Keep design docs and repository fixtures focused on Airstyle itself. Do not add tracked reference fixtures or CI dependencies for another formatter.

## Testing

Authoritative layouts live in [FORMATTER_STYLE.md](FORMATTER_STYLE.md). When the style document and a test disagree, the test is usually wrong.

- Formatter tests use `FormatterAssertions.assertFormatsOldToNew(oldCode, newCode)` or `assertCanonicalFormatting(source)`. Both run the full pipeline and enforce idempotence (formatting the expected output again produces itself).
- For tests whose input is deliberately unparseable, use `assertUnparseableInputUnchanged(source)` — it skips the validator's parseability check, which `assertFormatsOldToNew` would fail on.
