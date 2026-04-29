# Formatter Implementation Instructions

How to implement formatter changes. The style itself — what the output should look like — lives in [docs/FORMATTER_STYLE.md](docs/FORMATTER_STYLE.md). The formatter pipeline and builder ownership rules live in [docs/FORMATTER_ARCHITECTURE.md](docs/FORMATTER_ARCHITECTURE.md). This file covers workflow and the implementation rules that aren't about output (test conventions, AST-over-strings, safety invariants).

If the style document is silent on a construct, use isolated one-file IntelliJ formatting with this repo's `Airlift.xml` as the reference. If Airstyle should intentionally differ, write that down in the style document before writing formatter code.

## Core principles

1. Fixes are generic and construct-based. No project-specific or method-name-specific hacks.
2. Detect structure with AST/tokens. No regex or raw string matching for structural decisions.
3. Preserve source shape where possible, but not when it violates formatter intent.
4. Treat Checkstyle as a floor. Formatter behavior can go beyond it.
5. Prefer robust behavior over clever heuristics.

## Workflow

1. Add tests first.
2. Verify the new tests fail before code changes.
3. Implement the fix.
4. Re-run tests, confirm green.
5. Run `mvn clean install`.
6. Commit each logical fix separately.

## Architecture hand-offs

Before choosing normalizer vs engine, follow [FORMATTER_ARCHITECTURE.md](docs/FORMATTER_ARCHITECTURE.md)'s placement rules. In particular, indentation and alignment belong in the FORMAT engine, and multiline expressions or child-owning constructs need structural blocks instead of token-run shortcuts.

## Test rules

1. Formatter tests use old/new assertions via `FormatterAssertions.assertFormatsOldToNew(...)` or `assertCanonicalFormatting(...)`. No Checkstyle-based assertions.
2. Name tests `testFormatterFixes...` or `testFormatterKeeps...`. No `violation` language.
3. Existing tests are assumed correct. Ask before changing them; prefer adding edge-case tests.
4. Use anonymized, minimal test cases. Never private / business-specific names.
5. Text blocks are fine for readability.
6. Test classes use the `Test` prefix.

## Safety invariants

Violations of these have caused data-loss or checkstyle-regressing bugs in the past. They are invariants, not guidelines.

1. Never delete imports that appear to be unused but are referenced from Javadoc (`@link`, `@see`, etc.).
2. Never regress `else if` into `else { if (...) ... }`.
3. Never add trailing whitespace; never change tabs inside string literals or comment content.
4. Normalize line endings to LF early so downstream phases can assume LF.
5. Every `buildX` that emits child blocks must cover the full parent range. A gap between the last child's end and the parent's end silently drops source tokens (comments, `;`, commas) because `ApplyChangesState.apply` only emits leaf text plus computed whitespace. See [FORMATTER_ARCHITECTURE.md](docs/FORMATTER_ARCHITECTURE.md)'s "data-loss hazard" note.

## Style-policy hand-offs

When a fix changes *what* the output looks like — not just how we get there — update [docs/FORMATTER_STYLE.md](docs/FORMATTER_STYLE.md) in the same commit. If the style document and a test disagree, the test is probably stale.
