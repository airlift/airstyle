# Formatter Style

This document describes the code layout Airstyle should produce.

It is about output, not implementation. For formatter internals, see [FORMATTER_ARCHITECTURE.md](FORMATTER_ARCHITECTURE.md). For workflow and safety rules when changing the formatter, see [AGENTS.md](../AGENTS.md).

If this document does not say otherwise, use isolated one-file IntelliJ formatting with this repo's `Airlift.xml` style file as the compatibility reference. If Airstyle intentionally differs, that difference should be written down here in plain language.

## Wrapped Lists

This section covers invocation arguments, constructor arguments, parameters, record components, and similar parenthesized lists.

- Preserve one blank line between wrapped list items when it is present in the source.
- Collapse repeated blank lines between wrapped list items to a single blank line.

### Wrapped Multi-Argument Calls

```java
assertExpectedValues(
        first,

        buildExpected(
                left,
                right),
        enabled);
```

### Short Call Exception

Short outer call names keep the first argument on the opening line, even when the call is wrapped.

```java
super(createConfig(
                name,
                value),
        enabled);
```

### One-Argument Calls

- A one-argument call may keep that argument on the opening line even when the argument itself is multiline.

```java
assertUpdate("CREATE TABLE " + tableName
        + " AS SELECT * FROM t");
```

### Nested Calls

- The outer call decides whether the inner call starts on the opening line or on the next line.
- Once the inner call has started, format it using the normal call rules from that indentation level.

```java
assertThat(
        buildExpected(
                left,
                right),
        enabled);
```

## Method Chains

A method chain is a sequence of calls on the same value, such as `.map(...)`, `.filter(...)`, and `.toList()`.

- When a chain wraps, indent each wrapped method call by the same amount.
- If a method chain contains a nested call, format the nested call with its normal indentation. Do not push it farther right just because it appears inside the chain.
- If a chain is already consistently one wrapped call per line, keep it that way.
- When the chain sits inside a conditional expression's then- or else-branch, wrapped selectors align with the chain's own column (the receiver) rather than getting a continuation indent. Outside a conditional branch, wrapped selectors land at the receiver + continuation indent.

```java
return stream()
        .map(this::toRow)
        .filter(this::isValid)
        .toList();
```

```java
return builder()
        .addAll(createValues(
                left,
                right))
        .build();
```

In the second example, the arguments inside `createValues(...)` are formatted as a normal wrapped call. They are not pushed farther right just because the call appears inside a wrapped method chain.

## Conditional Expressions

- When a conditional expression wraps, `?` and `:` arms use continuation indent.
- When a conditional expression is nested inside a `?` or `:` arm, its wrapped operators align to the nested condition start.

```java
JsonNode statsNode = !fieldNode.isMissingNode() ? fieldNode
        : !objectNode.isMissingNode() ? objectNode
          : !arrayNode.isMissingNode() ? arrayNode
            : fieldNode;
```

## Lambdas

- Expression lambdas follow the same wrapping rules as any other expression.
- Single untyped lambda parameters do not use parentheses.
- A block lambda with a body that is only a single `throw` statement may stay inline if it is already inline.
- If that same single-`throw` lambda is already wrapped, leave it wrapped.
- Any other non-empty block lambda uses normal multiline block formatting.
- Wrapping an enclosing call does not, by itself, add another indentation level inside the lambda body.

```java
value -> value
        .trim()
        .toLowerCase()
```

```java
exec(() -> { throw new RuntimeException(); });
```

```java
value -> {
    if (value == null) {
        return defaultValue;
    }
    return normalize(value);
}
```

## Wrapped Expressions

- Continuation lines in a multiline expression indent from the start of that expression.
- This applies to string concatenation, boolean expressions, ternaries, and similar wrapped expressions.
- A one-argument call may still keep that expression on the opening line.
- In a wrapped multi-argument call, a multiline expression argument usually moves onto its own line with the rest of the list.

Single argument:

```java
assertUpdate("CREATE TABLE " + tableName
        + " AS SELECT * FROM t");
```

Wrapped multi-argument call:

```java
assertUpdate(
        "CREATE TABLE " + tableName
                + " AS SELECT * FROM t",
        25);
```

Another example:

```java
return enabled
        && matches(left)
        && matches(right);
```

## Extends / Implements / Permits

- When the formatter rewrites the clause, sort items alphabetically.
- Two-item clauses stay on one line unless the source already has them wrapped.
- If two items are already wrapped, keep them wrapped.
- If two items are already inline and no rewrite is needed, keep the source order.

## Enums

- Wrapped simple enums with two or more constants use a trailing comma.
- Compact simple enums preserve the existing trailing-comma choice.
- Single-constant enums do not use a trailing comma.

## Map Factory Calls

- `Map.of(...)`, `ImmutableMap.of(...)`, `ImmutableListMultimap.of(...)`, and OpenTelemetry `Attributes.of(...)` treat wrapped arguments as key/value pairs.
- A one-line call stays one line.
- In a wrapped call, keep each key/value pair together.

```java
Map.of("a", 1,
        "b", 2);
```

## Text Blocks

- Text blocks always start on the next line.
- If the surrounding call or assignment wraps, wrap the text block with it.
- If the expression continues after the text block, format the rest of the expression using the normal rules for calls, wrapped lists, and method chains.
- When a text block opens inline, align its content and closing delimiter under the opening `"""`, preserving the literal value by shifting the text-block margin together.

```java
return Optional.of(
        """
        [Unit]
        WantedBy=multi-user.target
        """).orElseThrow();
```

## Comments

- Do not move `//` comments at column 1.
- Do not move block comments that start at column 1.
- When reindenting a block comment, keep the relative indentation inside the comment.
- Leave a block comment alone if any content line starts to the left of the opening `/*`.
- A closing `*/` that is too far left may be fixed when the rest of the comment can still be shifted safely.
- A trailing `//` comment after code keeps (or gets) exactly one space before it. Ensure at least one space after the `//`.
- Preserve IntelliJ directives like `//noinspection` verbatim — do not insert a space after `//` for those.
- Preserve existing indentation inside Javadoc and markdown doc comment description text. This includes list items and similar structured prose; do not collapse author-provided indentation there.
- Preserve indentation inside Javadoc `<pre>` blocks and markdown fenced code blocks verbatim.
- Classic one-line Javadoc comments are expanded to multiline Javadoc. Markdown doc comments are not changed by this rule.
- A declaration Javadoc belongs before declaration annotations. If a Javadoc is placed between annotations and the declaration, move it before the annotation block.
- In markdown doc comment prose, compact bracketed comma-separated spans to IntelliJ's form: `[from, to]` becomes `[from,to]`.
- A line comment between a multiline declaration annotation and the method signature is indented one normal level deeper than the declaration, matching IntelliJ's method-prefix block handling. Comments after marker or single-line annotations stay at declaration indent.

## Other Rules

- Package and import declarations are never line-wrapped.
- Keep empty methods, constructors, types, and similar empty blocks on one line as `{}`.
- Reformat non-empty one-line blocks into normal multiline blocks.
- In `do` statements, put the trailing `while (...);` on the line after the body block.
- In switch `case` guards, preserve whether `when` starts on the case line or the next line; wrapped guard operands use one continuation indent from the case label.
- In try-with-resources, omit the optional semicolon after the final resource.
- Unused lambda parameters are written as `_`.

```java
void run() {}
```

```java
void run()
{
    work();
}
```

**Exception:** when a method or constructor header wraps across multiple
lines, place the empty body `{}` on its own line at the declaration's
indent rather than trailing the last header line.

```java
void run(String input)
        throws IOException
{}
```

```java
void run(
        String first,
        String second)
{}
```

## Blank Lines and Whitespace

- Do not leave a blank line immediately after an opening brace.
- Do not leave a blank line immediately before a closing brace.
- Use at most one blank line between adjacent declarations or statement groups.
- Do not leave trailing whitespace.
- Blank lines should be truly blank.
