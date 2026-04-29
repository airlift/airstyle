/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.airlift.airstyle.engine.java;

import io.airlift.airstyle.engine.Spacing;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;

/// Produces [Spacing] for token pairs based on their JDT terminal type.
/// Port of the essential rules from `TokenSpacingFormatter` /
/// `JavaSpacePropertyProcessor`, sufficient to round-trip simple Java
/// code.
///
/// This is NOT a complete port — it covers the cases needed by the current
/// block builder and grows alongside it.
public final class JavaSpacingRules
{
    private JavaSpacingRules() {}

    private static final Spacing NO_SPACE = Spacing.none();
    /// Like NO_SPACE but does not preserve source line breaks — collapses `{\n}` to `{}`.
    private static final Spacing NO_SPACE_COLLAPSE = Spacing.createSpacing(0, 0, 0, false, 0);
    private static final Spacing ONE_SPACE = Spacing.oneSpace();
    private static final Spacing REQUIRE_LINE_FEED = Spacing.createSpacing(0, 0, 1, true, 0);
    // keepBlankLines=1 — IntelliJ default for KEEP_BLANK_LINES_IN_CODE. When
    // source has a blank line between two tokens (e.g. `/** ... */\n\n@Ann`),
    // preserve it.
    private static final Spacing KEEP_LINE_OR_SPACE = Spacing.createSpacing(1, 1, 0, true, 1);

    /// Returns the spacing between two adjacent token leaves.
    ///
    /// @param prev type of the previous token (JDT [ITerminalSymbols])
    /// @param next type of the next token
    public static Spacing between(int prev, int next)
    {
        // Line comments consume the trailing `\n` — anything after MUST
        // go on the next line, or the subsequent token becomes part of
        // the comment. Force at least one line feed.
        if (prev == ITerminalSymbols.TokenNameCOMMENT_LINE) {
            return REQUIRE_LINE_FEED;
        }
        // When the next token is a comment (line/block/javadoc/markdown),
        // preserve source line breaks and up to one blank line. Otherwise
        // rules like `prev == COMMA → ONE_SPACE` would collapse blank lines
        // intentionally placed before a section-marker comment.
        if (next == ITerminalSymbols.TokenNameCOMMENT_LINE
                || next == ITerminalSymbols.TokenNameCOMMENT_BLOCK
                || next == ITerminalSymbols.TokenNameCOMMENT_JAVADOC
                || next == ITerminalSymbols.TokenNameCOMMENT_MARKDOWN) {
            return KEEP_LINE_OR_SPACE;
        }
        // `{` followed by `}` in an empty block body — collapse to `{}`,
        // ignoring any source line break. KEEP_SIMPLE_*_IN_ONE_LINE.
        if (isOpenBrace(prev) && isCloseBrace(next)) {
            return NO_SPACE_COLLAPSE;
        }
        // No space inside parens or brackets — except `; )` in empty
        // for-loop iterators where a space is required before `)`.
        if (prev == ITerminalSymbols.TokenNameLPAREN || next == ITerminalSymbols.TokenNameRPAREN) {
            if (prev == ITerminalSymbols.TokenNameSEMICOLON) {
                return ONE_SPACE;
            }
            return NO_SPACE;
        }
        if (next == ITerminalSymbols.TokenNameELLIPSIS) {
            return NO_SPACE;
        }
        if (prev == ITerminalSymbols.TokenNameLBRACKET || next == ITerminalSymbols.TokenNameLBRACKET
                || next == ITerminalSymbols.TokenNameRBRACKET) {
            return NO_SPACE;
        }
        // No space around . and ::
        if (prev == ITerminalSymbols.TokenNameDOT || next == ITerminalSymbols.TokenNameDOT) {
            return NO_SPACE;
        }
        if (prev == ITerminalSymbols.TokenNameCOLON_COLON || next == ITerminalSymbols.TokenNameCOLON_COLON) {
            return NO_SPACE;
        }
        // No space before `;` — except after `;;` in for-loops. Block
        // comments before `;` use KEEP_LINE_OR_SPACE so adjacent `/**/;`
        // stays compact while `/* no test */ ;` keeps its space.
        if (next == ITerminalSymbols.TokenNameSEMICOLON) {
            if (prev == ITerminalSymbols.TokenNameSEMICOLON) {
                return ONE_SPACE;
            }
            if (prev == ITerminalSymbols.TokenNameCOMMENT_BLOCK) {
                return KEEP_LINE_OR_SPACE;
            }
            return NO_SPACE;
        }
        // Space after `;` except before `}` (end of block).
        if (prev == ITerminalSymbols.TokenNameSEMICOLON) {
            if (next == ITerminalSymbols.TokenNameRBRACE) {
                return KEEP_LINE_OR_SPACE;
            }
            return ONE_SPACE;
        }
        // No space before `,`; space after (unless before `}`).
        if (next == ITerminalSymbols.TokenNameCOMMA) {
            return NO_SPACE;
        }
        if (prev == ITerminalSymbols.TokenNameCOMMA) {
            if (next == ITerminalSymbols.TokenNameRBRACE) {
                return KEEP_LINE_OR_SPACE;
            }
            return ONE_SPACE;
        }
        // No space after `@` in annotations.
        if (prev == ITerminalSymbols.TokenNameAT) {
            return NO_SPACE;
        }
        // `(` — no space when preceded by an identifier (method call /
        // record declaration / method declaration / cast target / `this(`
        // or `super(` constructor call). Space otherwise (statement
        // keywords like `if`/`while`, assignment operators like `=`/`==`,
        // arithmetic like `+`, etc.).
        if (next == ITerminalSymbols.TokenNameLPAREN) {
            if (isIdentifierLikeForCall(prev)) {
                return NO_SPACE;
            }
            // `{(` in an array initializer (`{(byte) 1}`): no space — `(` is
            // an expression cast/group, not a control-flow keyword's `(`.
            if (prev == ITerminalSymbols.TokenNameLBRACE) {
                return NO_SPACE;
            }
            // `!(` and `~(` — unary prefix before parenthesized expression.
            if (prev == ITerminalSymbols.TokenNameNOT || prev == ITerminalSymbols.TokenNameTWIDDLE) {
                return NO_SPACE;
            }
            return ONE_SPACE;
        }
        // Space before `{` (but not right after another `{`; `({` is already
        // handled above by the "no space inside parens" rule).
        if (next == ITerminalSymbols.TokenNameLBRACE) {
            if (prev == ITerminalSymbols.TokenNameLBRACE) {
                return NO_SPACE;
            }
            return ONE_SPACE;
        }
        // Space around `->` (lambda arrow).
        if (next == ITerminalSymbols.TokenNameARROW || prev == ITerminalSymbols.TokenNameARROW) {
            return ONE_SPACE;
        }
        // Space around `?` and `:` in ternary. A bare `:` also appears in
        // labels and case labels — those are handled by dedicated block
        // handlers with explicit spacing overrides.
        if (next == ITerminalSymbols.TokenNameQUESTION || prev == ITerminalSymbols.TokenNameQUESTION) {
            return ONE_SPACE;
        }
        // `++` / `--` increment/decrement — no space between operator and
        // operand (both prefix `--x` and postfix `x--` / `x++`). EXCEPTIONS:
        // when the other side is a binary operator or assignment, use the
        // binary rule instead so `x-- > 0` doesn't become `x-->0` (which
        // produces the `-->` token). When the other side is `:`, the `:` is
        // a ternary colon (a label colon is kept inside a leaf and would not
        // be visible here), so require a space to avoid `x++:` / `:++x`.
        if (isIncrementDecrement(prev) && !isBinaryOperator(next) && !isAssignmentOperator(next)
                && next != ITerminalSymbols.TokenNameCOLON) {
            return NO_SPACE;
        }
        if (isIncrementDecrement(next) && !isBinaryOperator(prev) && !isAssignmentOperator(prev)
                && prev != ITerminalSymbols.TokenNameCOLON) {
            return NO_SPACE;
        }
        // Unary `!` and `~` — no space to operand.
        if (prev == ITerminalSymbols.TokenNameNOT || prev == ITerminalSymbols.TokenNameTWIDDLE) {
            return NO_SPACE;
        }
        // Binary operators: always require one space on each side.
        if (isBinaryOperator(prev) || isBinaryOperator(next)) {
            return ONE_SPACE;
        }
        // Assignment operators: always one space.
        if (isAssignmentOperator(prev) || isAssignmentOperator(next)) {
            return ONE_SPACE;
        }
        return KEEP_LINE_OR_SPACE;
    }

    private static boolean isIncrementDecrement(int token)
    {
        return token == ITerminalSymbols.TokenNamePLUS_PLUS
                || token == ITerminalSymbols.TokenNameMINUS_MINUS;
    }

    /// Tokens that, when followed by `(`, mean "this is a method call
    /// or record/class declaration or constructor invocation" — `foo(`,
    /// `this(`, `super(`, `new Foo(`, etc.
    private static boolean isIdentifierLikeForCall(int token)
    {
        return token == ITerminalSymbols.TokenNameIdentifier
                || token == ITerminalSymbols.TokenNamethis
                || token == ITerminalSymbols.TokenNamesuper
                || token == ITerminalSymbols.TokenNameGREATER
                || token == ITerminalSymbols.TokenNameRBRACKET;
    }

    public static Spacing noSpace()
    {
        return NO_SPACE;
    }

    public static Spacing oneSpace()
    {
        return ONE_SPACE;
    }

    public static Spacing keepLineOrSpace()
    {
        return KEEP_LINE_OR_SPACE;
    }

    private static boolean isBinaryOperator(int token)
    {
        return token == ITerminalSymbols.TokenNamePLUS
                || token == ITerminalSymbols.TokenNameMINUS
                || token == ITerminalSymbols.TokenNameMULTIPLY
                || token == ITerminalSymbols.TokenNameDIVIDE
                || token == ITerminalSymbols.TokenNameREMAINDER
                || token == ITerminalSymbols.TokenNameAND_AND
                || token == ITerminalSymbols.TokenNameOR_OR
                || token == ITerminalSymbols.TokenNameAND
                || token == ITerminalSymbols.TokenNameOR
                || token == ITerminalSymbols.TokenNameXOR
                // TokenNameLESS/TokenNameGREATER double as generic brackets
                // but buildTokensRange overrides to NO_SPACE for generic
                // positions (via genericOpens/genericCloses sets which now
                // also cover type parameter declarations on classes/methods).
                || token == ITerminalSymbols.TokenNameLESS
                || token == ITerminalSymbols.TokenNameGREATER
                || token == ITerminalSymbols.TokenNameLESS_EQUAL
                || token == ITerminalSymbols.TokenNameGREATER_EQUAL
                || token == ITerminalSymbols.TokenNameEQUAL_EQUAL
                || token == ITerminalSymbols.TokenNameNOT_EQUAL
                || token == ITerminalSymbols.TokenNameLEFT_SHIFT
                || token == ITerminalSymbols.TokenNameRIGHT_SHIFT
                || token == ITerminalSymbols.TokenNameUNSIGNED_RIGHT_SHIFT
                || token == ITerminalSymbols.TokenNameinstanceof;
    }

    private static boolean isAssignmentOperator(int token)
    {
        return token == ITerminalSymbols.TokenNameEQUAL
                || token == ITerminalSymbols.TokenNamePLUS_EQUAL
                || token == ITerminalSymbols.TokenNameMINUS_EQUAL
                || token == ITerminalSymbols.TokenNameMULTIPLY_EQUAL
                || token == ITerminalSymbols.TokenNameDIVIDE_EQUAL
                || token == ITerminalSymbols.TokenNameAND_EQUAL
                || token == ITerminalSymbols.TokenNameOR_EQUAL
                || token == ITerminalSymbols.TokenNameXOR_EQUAL
                || token == ITerminalSymbols.TokenNameREMAINDER_EQUAL
                || token == ITerminalSymbols.TokenNameLEFT_SHIFT_EQUAL
                || token == ITerminalSymbols.TokenNameRIGHT_SHIFT_EQUAL
                || token == ITerminalSymbols.TokenNameUNSIGNED_RIGHT_SHIFT_EQUAL;
    }

    private static boolean isOpenBrace(int token)
    {
        return token == ITerminalSymbols.TokenNameLBRACE;
    }

    private static boolean isCloseBrace(int token)
    {
        return token == ITerminalSymbols.TokenNameRBRACE;
    }
}
