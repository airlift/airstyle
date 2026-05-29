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

import io.airlift.airstyle.engine.Block;
import io.airlift.airstyle.engine.Indent;
import io.airlift.airstyle.engine.Spacing;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Math.max;

final class JavaDeclarationBuilder
{
    private final JavaBlockBuilder owner;
    private final JavaSourceContext sourceContext;
    private final String source;
    private final List<JavaTokens.Token> tokens;
    private final Set<Integer> genericOpens;
    private final Map<Integer, Annotation> wrappedAnnotations;

    JavaDeclarationBuilder(
            JavaBlockBuilder owner,
            JavaSourceContext sourceContext,
            Set<Integer> genericOpens,
            Map<Integer, Annotation> wrappedAnnotations)
    {
        this.owner = owner;
        this.sourceContext = sourceContext;
        this.source = sourceContext.source();
        this.tokens = sourceContext.tokens();
        this.genericOpens = genericOpens;
        this.wrappedAnnotations = wrappedAnnotations;
    }

    private List<JavaTokens.Token> tokensIn(int start, int end)
    {
        return sourceContext.tokensIn(start, end);
    }

    private boolean hasCommentIn(int start, int end)
    {
        return owner.hasCommentIn(start, end);
    }

    private int findMatchingRParen(int lparenOffset, int maxEnd)
    {
        return owner.findMatchingRParen(lparenOffset, maxEnd);
    }

    private int extendThroughTrailingInlineComments(int currentEnd, int boundary)
    {
        return owner.extendThroughTrailingInlineComments(currentEnd, boundary);
    }

    Block buildTypeOrDeclaration(BodyDeclaration node)
    {
        return switch (node) {
            case TypeDeclaration td -> buildTypeDeclaration(td);
            case RecordDeclaration rd -> buildRecordDeclaration(rd);
            case EnumDeclaration ed -> buildEnumDeclaration(ed);
            case AnnotationTypeDeclaration atd -> buildAnnotationTypeDeclaration(atd);
            case AnnotationTypeMemberDeclaration atmd -> buildAnnotationTypeMemberDeclaration(atmd);
            case MethodDeclaration md -> buildMethodDeclaration(md);
            case FieldDeclaration fd -> buildFieldDeclaration(fd);
            case Initializer init -> buildInitializer(init);
            default -> owner.buildTokensSpanning(node, node.getClass().getSimpleName());
        };
    }

    /// Annotation member declaration: `Type name() default value;`. If the
    /// default value spans multiple lines (e.g. array initializer), decompose
    /// it so elements carry CONTINUATION indent.
    private Block buildAnnotationTypeMemberDeclaration(AnnotationTypeMemberDeclaration node)
    {
        int start = node.getStartPosition();
        int end = start + node.getLength();
        Expression defaultExpr = node.getDefault();
        if (defaultExpr == null || !containsLineBreak(start, end)) {
            return owner.buildTokensRange(start, end, "AnnotationMember");
        }
        int exprStart = defaultExpr.getStartPosition();
        int exprEnd = exprStart + defaultExpr.getLength();
        JavaBlock.Builder composite = JavaBlock.builder(start, end, "AnnotationMember");
        Block headerPart = owner.buildTokensRange(start, exprStart, "AnnotMemberHeader");
        composite.child(headerPart);
        Block valueBlock = owner.buildExpressionBlock(defaultExpr, exprStart, exprEnd, "DefaultValue");
        JavaBlockBuilder.addSibling(composite, headerPart, valueBlock, JavaSpacingRules.keepLineOrSpace());
        if (exprEnd < end) {
            Block trailing = owner.buildTokensRange(exprEnd, end, "AnnotMemberTrailing");
            JavaBlockBuilder.addSibling(composite, valueBlock, trailing, Spacing.none());
        }
        return composite.build();
    }

    private Block buildTypeDeclaration(TypeDeclaration node)
    {
        int start = node.getStartPosition();
        int end = start + node.getLength();
        JavaBlock.Builder composite = JavaBlock.builder(start, end, "TypeDeclaration");
        int braceOffset = owner.findFirstOpenBrace(start, end);
        int headerEnd = braceOffset < 0 ? end : braceOffset;

        Block headerBlock = buildTypeHeader(node, start, headerEnd);
        composite.child(headerBlock);
        Block prev = headerBlock;

        // Empty type bodies — KEEP_SIMPLE_CLASSES_IN_ONE_LINE semantics:
        //   - If the declaration HEADER (keyword + name + type params +
        //     extends/implements/permits clauses, NOT counting trailing
        //     whitespace before the brace) is single-line, collapse to
        //     `class Foo {}` inline.
        //   - If the header itself spans multiple lines (wrapped clauses),
        //     put `{}` on its own line at the declaration indent.
        if (braceOffset >= 0 && node.bodyDeclarations().isEmpty()) {
            int closeBrace = owner.findLastCloseBrace(braceOffset, end);
            if (closeBrace >= 0) {
                // If the body contains only comments, preserve them on their
                // own lines. Without this, the comment text lives in the gap
                // between `{` and `}` and gets dropped when the empty-body
                // branch collapses to inline `{}`.
                boolean hasComment = hasCommentIn(braceOffset + 1, closeBrace);
                if (!hasComment) {
                    Block openBrace = owner.buildTokensRange(braceOffset, braceOffset + 1, "{");
                    // Empty body always renders as `{}` inline on the last
                    // header line — regardless of source `{` position. This
                    // collapses `class Foo\n{\n}` to `class Foo {}` and
                    // inlines `{}` after a wrapped clause (`...Orange {}`).
                    JavaBlockBuilder.addSibling(composite, prev, openBrace, Spacing.createSpacing(1, 1, 0, false, 0));
                    Block closeBraceBlock = owner.buildTokensRange(closeBrace, closeBrace + 1, "}");
                    JavaBlockBuilder.addSibling(
                            composite,
                            openBrace,
                            closeBraceBlock,
                            Spacing.createSpacing(0, 0, 0, false, 0));
                    return composite.build();
                }
                // Fall through so the body-with-comments path emits the
                // comments as inter-member leaves.
            }
        }

        if (braceOffset < 0) {
            // No body: headerEnd == end, nothing more to add.
            return composite.build();
        }

        // Opening brace and body.
        Block openBrace = owner.buildTokensRange(braceOffset, braceOffset + 1, "{");
        JavaBlockBuilder.addSibling(composite, prev, openBrace, Spacing.createSpacing(0, 0, 1, false, 0));
        prev = openBrace;

        int closeBraceOffset = owner.findLastCloseBrace(braceOffset, end);
        int bodyEnd = closeBraceOffset < 0 ? end : closeBraceOffset;

        BodyDeclaration prevMember = null;
        int scanCursor = braceOffset + 1;
        for (Object member : node.bodyDeclarations()) {
            BodyDeclaration memberDecl = (BodyDeclaration) member;
            // Emit any comments in the gap before this member as NORMAL-indent
            // leaves (line/markdown javadoc not attached to the AST ends up here).
            Block prevBeforeComments = prev;
            prev = emitInterBlockComments(composite, prev, scanCursor, memberDecl.getStartPosition());
            boolean commentAttached = prev != prevBeforeComments
                    && !containsBlankLine(prev.endOffset(), memberDecl.getStartPosition());
            BuiltMember builtMember = buildIndentedMember(memberDecl, bodyEnd, "Member");
            // Enforce blank line between different-kind members (field→method,
            // constructor→method, method→inner type, etc.).
            // Airlift style: blank line between member declarations, except
            // consecutive fields which stay on adjacent lines.
            // EXCEPTION: if a comment immediately precedes this member with no
            // blank line between, the comment is "attached" to the member and
            // the blank line belongs BEFORE the comment, not between the
            // comment and the member. Use 1 line feed here.
            boolean needBlankLine = prevMember != null
                    && !(prevMember instanceof FieldDeclaration && memberDecl instanceof FieldDeclaration);
            int minLines = (needBlankLine && !commentAttached) ? 2 : 1;
            JavaBlockBuilder.addSibling(composite, prev, builtMember.block(), Spacing.createSpacing(0, 0, minLines, true, 1));
            prev = builtMember.block();
            prevMember = memberDecl;
            scanCursor = builtMember.endOffset();
        }
        // Trailing comments between last member and `}`.
        if (closeBraceOffset >= 0) {
            prev = emitInterBlockComments(composite, prev, scanCursor, closeBraceOffset);
        }

        if (closeBraceOffset >= 0) {
            Block closeBrace = owner.buildTokensRange(closeBraceOffset, closeBraceOffset + 1, "}");
            JavaBlockBuilder.addSibling(composite, prev, closeBrace, Spacing.createSpacing(0, 0, 1, true, 0));
            if (bodyEnd + 1 < end) {
                Block trailing = owner.buildTokensRange(closeBraceOffset + 1, end, "Trailing");
                JavaBlockBuilder.addSibling(composite, closeBrace, trailing, Spacing.none());
            }
        }
        return composite.build();
    }

    private Block buildRecordDeclaration(RecordDeclaration node)
    {
        int start = node.getStartPosition();
        int end = start + node.getLength();
        JavaBlock.Builder composite = JavaBlock.builder(start, end, "RecordDeclaration");
        int braceOffset = owner.findFirstOpenBrace(start, end);
        int headerEnd = braceOffset < 0 ? end : braceOffset;

        // Build the header in three parts so parameters can carry CONTINUATION
        // indent independently: prefix (modifiers + record + name + `(`),
        // component list (each component with CONTINUATION), suffix (`)`).
        Block headerBlock = buildRecordHeader(node, start, headerEnd);
        composite.child(headerBlock);
        Block prev = headerBlock;

        int closeBraceOffset = owner.findLastCloseBrace(braceOffset, end);
        boolean emptyBody = node.bodyDeclarations().isEmpty()
                && (closeBraceOffset < 0 || !hasCommentIn(braceOffset + 1, closeBraceOffset));

        if (braceOffset < 0) {
            return composite.build();
        }

        Block openBrace = owner.buildTokensRange(braceOffset, braceOffset + 1, "{");
        // Airlift CLASS_BRACE_STYLE=NEXT_LINE: records with a body always
        // put `{` on its own line. Empty body collapses to inline `{}`
        // regardless of source shape.
        boolean forceBraceOnOwnLine = !emptyBody;
        Spacing headerToBrace = forceBraceOnOwnLine
                ? Spacing.createSpacing(0, 0, 1, false, 0)
                : Spacing.createSpacing(1, 1, 0, false, 0);
        JavaBlockBuilder.addSibling(composite, prev, openBrace, headerToBrace);
        prev = openBrace;

        if (emptyBody) {
            if (closeBraceOffset >= 0) {
                Block closeBrace = owner.buildTokensRange(closeBraceOffset, closeBraceOffset + 1, "}");
                // Collapse empty body to `{}` — do NOT keep source line break
                // between `{` and `}`, even if the source had `{\n}`.
                JavaBlockBuilder.addSibling(composite, prev, closeBrace, Spacing.createSpacing(0, 0, 0, false, 0));
            }
            return composite.build();
        }

        BodyDeclaration prevMember = null;
        int recordScanCursor = braceOffset + 1;
        int bodyEnd = closeBraceOffset < 0 ? end : closeBraceOffset;
        for (Object member : node.bodyDeclarations()) {
            BodyDeclaration memberDecl = (BodyDeclaration) member;
            Block prevBeforeComments = prev;
            prev = emitInterBlockComments(composite, prev, recordScanCursor, memberDecl.getStartPosition());
            boolean commentAttached = prev != prevBeforeComments
                    && !containsBlankLine(prev.endOffset(), memberDecl.getStartPosition());
            BuiltMember builtMember = buildIndentedMember(memberDecl, bodyEnd, "Member");
            boolean needBlankLine = prevMember != null
                    && !(prevMember instanceof FieldDeclaration && memberDecl instanceof FieldDeclaration);
            int minLines = (needBlankLine && !commentAttached) ? 2 : 1;
            JavaBlockBuilder.addSibling(composite, prev, builtMember.block(), Spacing.createSpacing(0, 0, minLines, true, 1));
            prev = builtMember.block();
            prevMember = memberDecl;
            recordScanCursor = builtMember.endOffset();
        }

        if (closeBraceOffset >= 0) {
            prev = emitInterBlockComments(composite, prev, recordScanCursor, closeBraceOffset);
            Block closeBrace = owner.buildTokensRange(closeBraceOffset, closeBraceOffset + 1, "}");
            JavaBlockBuilder.addSibling(composite, prev, closeBrace, Spacing.createSpacing(0, 0, 1, true, 0));
        }
        return composite.build();
    }

    private Block buildInitializer(Initializer node)
    {
        int start = node.getStartPosition();
        int end = start + node.getLength();
        org.eclipse.jdt.core.dom.Block body = node.getBody();
        if (body == null) {
            return owner.buildTokensRange(start, end, "Initializer");
        }
        JavaBlock.Builder composite = JavaBlock.builder(start, end, "Initializer");
        int bodyStart = body.getStartPosition();
        if (bodyStart > start) {
            Block header = owner.buildTokensRange(start, bodyStart, "InitializerHeader");
            composite.child(header);
            Block bodyBlock = owner.buildStatementBlock(body);
            // Airlift END_OF_LINE brace style for static initializers:
            // `static {` on the same line, not `static\n{`.
            JavaBlockBuilder.addSibling(composite, header, bodyBlock, Spacing.createSpacing(1, 1, 0, false, 0));
        }
        else {
            composite.child(owner.buildStatementBlock(body));
        }
        return composite.build();
    }

    private Block buildAnnotationTypeDeclaration(AnnotationTypeDeclaration node)
    {
        int start = node.getStartPosition();
        int end = start + node.getLength();
        JavaBlock.Builder composite = JavaBlock.builder(start, end, "AnnotationType");
        int braceOffset = owner.findFirstOpenBrace(start, end);
        int headerEnd = braceOffset < 0 ? end : braceOffset;
        Block header = buildTypeHeader(node, start, headerEnd);
        composite.child(header);
        Block prev = header;
        if (braceOffset < 0) {
            return composite.build();
        }
        int closeBraceOffset = owner.findLastCloseBrace(braceOffset, end);
        boolean emptyBody = node.bodyDeclarations().isEmpty()
                && (closeBraceOffset < 0 || !hasCommentIn(braceOffset + 1, closeBraceOffset));
        Block openBrace = owner.buildTokensRange(braceOffset, braceOffset + 1, "{");
        Spacing headerToBrace = emptyBody
                ? Spacing.createSpacing(1, 1, 0, false, 0)
                : Spacing.createSpacing(0, 0, 1, false, 0);
        JavaBlockBuilder.addSibling(composite, prev, openBrace, headerToBrace);
        prev = openBrace;
        if (emptyBody) {
            if (closeBraceOffset >= 0) {
                Block close = owner.buildTokensRange(closeBraceOffset, closeBraceOffset + 1, "}");
                JavaBlockBuilder.addSibling(composite, prev, close, Spacing.createSpacing(0, 0, 0, false, 0));
            }
            return composite.build();
        }
        BodyDeclaration prevMember = null;
        int annScanCursor = braceOffset + 1;
        int bodyEnd = closeBraceOffset < 0 ? end : closeBraceOffset;
        for (Object member : node.bodyDeclarations()) {
            BodyDeclaration memberDecl = (BodyDeclaration) member;
            Block prevBeforeComments = prev;
            prev = emitInterBlockComments(composite, prev, annScanCursor, memberDecl.getStartPosition());
            boolean commentAttached = prev != prevBeforeComments
                    && !containsBlankLine(prev.endOffset(), memberDecl.getStartPosition());
            BuiltMember builtMember = buildIndentedMember(memberDecl, bodyEnd, "AnnMember");
            // Airlift style: blank line between member declarations, except
            // consecutive fields which stay on adjacent lines.
            boolean needBlankLine = prevMember != null
                    && !(prevMember instanceof FieldDeclaration && memberDecl instanceof FieldDeclaration);
            int minLines = (needBlankLine && !commentAttached) ? 2 : 1;
            JavaBlockBuilder.addSibling(composite, prev, builtMember.block(), Spacing.createSpacing(0, 0, minLines, true, 1));
            prev = builtMember.block();
            prevMember = memberDecl;
            annScanCursor = builtMember.endOffset();
        }
        if (closeBraceOffset >= 0) {
            // Trailing comments between last member and `}`.
            prev = emitInterBlockComments(composite, prev, annScanCursor, closeBraceOffset);
            Block closeBrace = owner.buildTokensRange(closeBraceOffset, closeBraceOffset + 1, "}");
            JavaBlockBuilder.addSibling(composite, prev, closeBrace, Spacing.createSpacing(0, 0, 1, true, 0));
        }
        return composite.build();
    }

    private Block buildEnumDeclaration(EnumDeclaration node)
    {
        int start = node.getStartPosition();
        int end = start + node.getLength();
        JavaBlock.Builder composite = JavaBlock.builder(start, end, "EnumDeclaration");
        int braceOffset = owner.findFirstOpenBrace(start, end);
        int headerEnd = braceOffset < 0 ? end : braceOffset;

        Block header = buildTypeHeader(node, start, headerEnd);
        composite.child(header);
        Block prev = header;

        if (braceOffset < 0) {
            return composite.build();
        }

        int closeBraceOffset = owner.findLastCloseBrace(braceOffset, end);
        boolean emptyBody = node.enumConstants().isEmpty()
                && node.bodyDeclarations().isEmpty()
                && (closeBraceOffset < 0 || !hasCommentIn(braceOffset + 1, closeBraceOffset));

        Block openBrace = owner.buildTokensRange(braceOffset, braceOffset + 1, "{");
        // Follow the TypeDeclaration convention: empty body collapses to
        // `{}`; non-empty body puts `{` on its own line for an enum.
        Spacing headerToBrace = emptyBody
                ? Spacing.createSpacing(1, 1, 0, false, 0)
                : Spacing.createSpacing(0, 0, 1, false, 0);
        JavaBlockBuilder.addSibling(composite, prev, openBrace, headerToBrace);
        prev = openBrace;

        if (emptyBody) {
            if (closeBraceOffset >= 0) {
                Block close = owner.buildTokensRange(closeBraceOffset, closeBraceOffset + 1, "}");
                JavaBlockBuilder.addSibling(composite, prev, close, Spacing.createSpacing(0, 0, 0, false, 0));
            }
            return composite.build();
        }

        // Enum constants: each at NORMAL indent. Span each block to include
        // the trailing comma / semicolon that separates it from the next
        // constant (or the body declarations / closing brace). Using the
        // position of the next "thing" as this block's end keeps the comma
        // attached so POST_FORMAT enumDeclaration sees consistent output.
        List<?> constants = node.enumConstants();
        List<?> bodyDecls = node.bodyDeclarations();
        int bodyEnd = closeBraceOffset < 0 ? end : closeBraceOffset;
        int firstBodyDeclStart = bodyDecls.isEmpty()
                ? bodyEnd
                : ((ASTNode) bodyDecls.getFirst()).getStartPosition();
        int enumScanCursor = braceOffset + 1;
        for (int i = 0; i < constants.size(); i++) {
            EnumConstantDeclaration c = (EnumConstantDeclaration) constants.get(i);
            int cStart = c.getStartPosition();
            int cEndBase = cStart + c.getLength();
            int constantStart = firstNonCommentTokenStart(cStart, c.getName().getStartPosition());
            int cEnd;
            if (i + 1 < constants.size()) {
                cEnd = ((ASTNode) constants.get(i + 1)).getStartPosition();
            }
            else {
                cEnd = firstBodyDeclStart;
            }
            if (cEnd < cEndBase) {
                cEnd = cEndBase;
            }
            // If the enum constant has an anonymous class body, decompose it.
            Block constantBlock;
            if (c.getAnonymousClassDeclaration() != null) {
                AnonymousClassDeclaration anon = c.getAnonymousClassDeclaration();
                int anonStart = anon.getStartPosition();
                int anonEnd = anonStart + anon.getLength();
                JavaBlock.Builder ecBlock = JavaBlock.builder(constantStart, cEnd, "EnumConstantWithBody");
                Block ecPrefix = owner.buildTokensRange(constantStart, anonStart, "EnumConstPrefix");
                ecBlock.child(ecPrefix);
                Block anonBlock = owner.buildAnonymousClassDeclaration(anon);
                JavaBlockBuilder.addSibling(ecBlock, ecPrefix, anonBlock, Spacing.createSpacing(1, 1, 0, false, 0));
                if (anonEnd < cEnd) {
                    // Trailing tokens after `}` — typically a comma, possibly
                    // with intervening block comments and the enum-body
                    // terminator `;`. Emit any block comments that live on
                    // their own line at NORMAL indent so they align with the
                    // enum body column.
                    Block prevTrail = anonBlock;
                    int trailCursor = anonEnd;
                    for (JavaTokens.Token tok : tokensIn(anonEnd, cEnd)) {
                        if (!tok.isComment() || !startsOwnLineAfter(trailCursor, tok.start())) {
                            continue;
                        }
                        if (!tokensIn(trailCursor, tok.start()).isEmpty()) {
                            Block ecTrailing = owner.buildTokensRange(trailCursor, tok.start(), "EnumConstTrailing");
                            JavaBlockBuilder.addSibling(ecBlock, prevTrail, ecTrailing, Spacing.none());
                            prevTrail = ecTrailing;
                        }
                        int commentEnd = tok.text().endsWith("\n") ? tok.end() - 1 : tok.end();
                        Block commentBlock = JavaBlock.builder(tok.start(), commentEnd, "EnumConstTrailingComment")
                                .indent(Indent.noneIndent())
                                .child(commentLeaf(tok))
                                .build();
                        JavaBlockBuilder.addSibling(ecBlock, prevTrail, commentBlock, JavaSpacingRules.keepLineOrSpace());
                        prevTrail = commentBlock;
                        trailCursor = tok.end();
                    }
                    if (trailCursor < cEnd) {
                        Block ecTrailing = owner.buildTokensRange(trailCursor, cEnd, "EnumConstTrailing");
                        JavaBlockBuilder.addSibling(
                                ecBlock,
                                prevTrail,
                                ecTrailing,
                                prevTrail == anonBlock ? Spacing.none() : Spacing.none());
                    }
                }
                constantBlock = ecBlock.build();
            }
            else if (!c.arguments().isEmpty() && owner.needsStructuredCallArguments(c.arguments(), c)) {
                // Enum constant with multi-line constructor args (e.g. block
                // lambda) — decompose the call so lambda body statements get
                // proper NORMAL indent.
                constantBlock = owner.buildEnumConstantCall(c, constantStart, cEnd);
            }
            else {
                constantBlock = owner.buildTokensRange(constantStart, cEnd, "EnumConstant");
            }
            Block wrapped;
            if (constantStart > cStart && hasCommentIn(cStart, constantStart)) {
                JavaBlock.Builder member = JavaBlock.builder(cStart, cEnd, "EnumConstantMember")
                        .indent(Indent.normalIndent())
                        .firstChildIndentPolicy(Block.FirstChildIndentPolicy.USE_BLOCK_INDENT);
                Block memberPrev = emitEnumConstantLeadingComments(member, cStart, constantStart);
                JavaBlockBuilder.addSibling(member, memberPrev, constantBlock, Spacing.createSpacing(0, 0, 1, true, 0));
                wrapped = member.build();
            }
            else {
                wrapped = JavaBlock.builder(cStart, cEnd, "EnumConstantMember")
                        .indent(Indent.normalIndent())
                        .firstChildIndentPolicy(Block.FirstChildIndentPolicy.USE_BLOCK_INDENT)
                        .child(constantBlock)
                        .build();
            }
            // Emit comments in the gap before this constant (between `{` or
            // prev constant and this one). Preserves `// 1xx` at the top of
            // an enum body and comments between constants.
            prev = emitInterBlockComments(composite, prev, enumScanCursor, cStart);
            // First constant: force line break after `{`. Subsequent: preserve
            // source shape — inline (space) or newline — so single-line enum
            // bodies stay on one line.
            Spacing sp = (i == 0)
                    ? Spacing.createSpacing(0, 0, 1, false, 0)
                    : Spacing.createSpacing(1, 1, 0, true, 1);
            JavaBlockBuilder.addSibling(composite, prev, wrapped, sp);
            prev = wrapped;
            enumScanCursor = cEnd;
        }

        boolean firstBodyDecl = true;
        BodyDeclaration prevMember = null;
        for (Object member : bodyDecls) {
            BodyDeclaration memberDecl = (BodyDeclaration) member;
            // Comments in the gap before this body decl (between enum
            // constants semicolon and first body decl, or between body decls).
            prev = emitInterBlockComments(composite, prev, enumScanCursor, memberDecl.getStartPosition());
            BuiltMember memberBlock = buildIndentedMember(memberDecl, bodyEnd, "Member");
            // Blank line between enum constants section and body declarations.
            boolean needBlankLine = firstBodyDecl
                    ? !constants.isEmpty()
                    : !(prevMember instanceof FieldDeclaration && memberDecl instanceof FieldDeclaration);
            int minLF = needBlankLine ? 2 : 1;
            JavaBlockBuilder.addSibling(composite, prev, memberBlock.block(), Spacing.createSpacing(0, 0, minLF, true, 1));
            prev = memberBlock.block();
            firstBodyDecl = false;
            prevMember = memberDecl;
            enumScanCursor = memberBlock.endOffset();
        }

        if (closeBraceOffset >= 0) {
            // Trailing comments between the last member and `}`.
            prev = emitInterBlockComments(composite, prev, enumScanCursor, closeBraceOffset);
            Block close = owner.buildTokensRange(closeBraceOffset, closeBraceOffset + 1, "}");
            JavaBlockBuilder.addSibling(composite, prev, close, Spacing.createSpacing(0, 0, 1, true, 0));
        }
        return composite.build();
    }

    private boolean startsOwnLineAfter(int previousEnd, int tokenStart)
    {
        return containsLineBreak(previousEnd, tokenStart)
                || (previousEnd > 0
                && previousEnd <= source.length()
                && source.charAt(previousEnd - 1) == '\n'
                && sourceOnlyWhitespace(previousEnd, tokenStart));
    }

    private Block emitEnumConstantLeadingComments(JavaBlock.Builder member, int start, int end)
    {
        Block prev = null;
        for (JavaTokens.Token tok : tokensIn(start, end)) {
            if (tok.isComment()) {
                int cEnd = (tok.text().endsWith("\n")) ? tok.end() - 1 : tok.end();
                Block commentBlock = JavaBlock.builder(tok.start(), cEnd, "EnumConstantLeadingComment")
                        .indent(Indent.noneIndent())
                        .child(commentLeaf(tok))
                        .build();
                JavaBlockBuilder.addSibling(member, prev, commentBlock, Spacing.createSpacing(0, 0, 1, true, 1));
                prev = commentBlock;
            }
        }
        return prev;
    }

    /// Build a type (class/interface) header, decomposing extends/implements/permits
    /// clauses into their own blocks so they can be indented and kept inline
    /// independently.
    private Block buildTypeHeader(BodyDeclaration node, int start, int headerEnd)
    {
        List<TypeClauseDescriptor> clauses = typeClauseDescriptors(node, start, headerEnd);
        if (clauses.isEmpty()) {
            // No clauses — just the header prefix. If any modifier annotation
            // is wrapped, decompose it; otherwise flat tokens.
            if (owner.hasWrappedAnnotationIn(start, headerEnd)) {
                JavaBlock.Builder h = JavaBlock.builder(start, headerEnd, "TypeHeader");
                owner.emitPrefixWithAnnotations(h, start, headerEnd);
                return h.build();
            }
            return owner.buildTokensRange(start, headerEnd, "TypeHeader");
        }
        JavaBlock.Builder header = JavaBlock.builder(start, headerEnd, "TypeHeader");
        int prefixEnd = clauses.getFirst().keywordStart();
        // Split the prefix: emit tokens before any inter-prefix comments
        // normally, then emit comments between the prefix and the first
        // clause keyword with CONTINUATION indent (they align with clauses).
        List<JavaTokens.Token> prefixComments = new ArrayList<>();
        for (JavaTokens.Token tok : tokensIn(start, prefixEnd)) {
            if (tok.isComment()) {
                prefixComments.add(tok);
            }
        }
        Block prev;
        // Wrapped annotations in the prefix get decomposed so their
        // value lists carry CONTINUATION indent. Also fires when the
        // prefix contains comments (javadoc) — emitPrefixWithAnnotations
        // emits the inter-annotation tokens (including comments) as a
        // flat "HeaderPre" range, which preserves source shape for the
        // comment while still decomposing the annotation.
        if (owner.hasWrappedAnnotationIn(start, prefixEnd)) {
            prev = owner.emitPrefixWithAnnotations(header, start, prefixEnd);
        }
        else if (prefixComments.isEmpty() && containsLineBreak(start, prefixEnd)) {
            // CONTINUATION applies ONLY when the line break is INSIDE the
            // type parameter brackets `<...>` — e.g. `class Test<T, E\n
            // extends Base<T>>`. Line breaks between an annotation and the
            // class keyword, or between modifiers, are source-shape and must
            // NOT add CONTINUATION (would over-indent the class line).
            // Mirrors IntelliJ's architecture: the class header itself has
            // no CONTINUATION indent; only the extends/implements/permits
            // list or type-param list children wrap.
            int typeParamOpen = findTypeParamOpenBeforeLineBreak(start, prefixEnd);
            if (typeParamOpen >= 0) {
                int firstLineBreak = findFirstLineBreak(typeParamOpen, prefixEnd);
                if (firstLineBreak > typeParamOpen && !tokensIn(firstLineBreak, prefixEnd).isEmpty()) {
                    Block prefix = owner.buildTokensRange(start, firstLineBreak, "TypeHeaderPrefix");
                    header.child(prefix);
                    Block contPart = JavaBlock.continuationWrap(
                            firstLineBreak,
                            prefixEnd,
                            owner.buildTokensRange(firstLineBreak, prefixEnd, "TypeHeaderPrefixContTokens", false),
                            "TypeHeaderPrefixCont");
                    JavaBlockBuilder.addSibling(header, prefix, contPart, JavaSpacingRules.keepLineOrSpace());
                    prev = contPart;
                }
                else {
                    Block prefix = owner.buildTokensRange(start, prefixEnd, "TypeHeaderPrefix");
                    header.child(prefix);
                    prev = prefix;
                }
            }
            else {
                // Line break is outside type params (annotation/modifier on
                // own line): emit as flat tokens, preserving source shape.
                Block prefix = owner.buildTokensRange(start, prefixEnd, "TypeHeaderPrefix");
                header.child(prefix);
                prev = prefix;
            }
        }
        else if (prefixComments.isEmpty()) {
            Block prefix = owner.buildTokensRange(start, prefixEnd, "TypeHeaderPrefix");
            header.child(prefix);
            prev = prefix;
        }
        else {
            // Comments split into two groups by position:
            //   - "leading" — appear before any non-comment token follows the
            //     comment and precedes the clause keyword. These are class
            //     javadoc / annotation-attached comments — source shape, no
            //     CONTINUATION (keeping them at the class indent).
            //   - "trailing" — the last contiguous run of comments before the
            //     clause keyword (only whitespace between them and the clause).
            //     These are "pre-clause marker" comments and get CONTINUATION
            //     to align with the wrapped clause.
            int firstTrailingCommentIndex = prefixComments.size();
            for (int i = prefixComments.size() - 1; i >= 0; i--) {
                int commentEnd = prefixComments.get(i).end();
                int commentStart = prefixComments.get(i).start();
                int nextStart = (i + 1 < prefixComments.size())
                        ? prefixComments.get(i + 1).start()
                        : prefixEnd;
                if (!sourceOnlyWhitespace(commentEnd, nextStart)) {
                    break;
                }
                if (i > 0 && !sourceOnlyWhitespace(prefixComments.get(i - 1).end(), commentStart)) {
                    firstTrailingCommentIndex = i;
                    break;
                }
                firstTrailingCommentIndex = i;
            }
            int leadingEnd = (firstTrailingCommentIndex < prefixComments.size())
                    ? prefixComments.get(firstTrailingCommentIndex).start()
                    : prefixEnd;
            if (leadingEnd > start) {
                Block leading = owner.buildTokensRange(start, leadingEnd, "TypeHeaderPrefix");
                header.child(leading);
                prev = leading;
            }
            else {
                prev = null;
            }
            for (int i = firstTrailingCommentIndex; i < prefixComments.size(); i++) {
                JavaTokens.Token comment = prefixComments.get(i);
                int hcEnd = (comment.text().endsWith("\n")) ? comment.end() - 1 : comment.end();
                Block commentBlock = JavaBlock.builder(comment.start(), hcEnd, "HeaderComment")
                        .indent(Indent.continuationIndent())
                        .child(commentLeaf(comment))
                        .build();
                if (prev == null) {
                    header.child(commentBlock);
                }
                else {
                    JavaBlockBuilder.addSibling(header, prev, commentBlock, Spacing.createSpacing(0, 0, 1, true, 0));
                }
                prev = commentBlock;
            }
        }
        for (int i = 0; i < clauses.size(); i++) {
            TypeClauseDescriptor clause = clauses.get(i);
            int clauseEnd = (i < clauses.size() - 1) ? clauses.get(i + 1).keywordStart() : headerEnd;
            Block clauseBlock = buildTypeClause(clause, clauseEnd);
            JavaBlockBuilder.addSibling(header, prev, clauseBlock, JavaSpacingRules.keepLineOrSpace());
            prev = clauseBlock;
        }
        return header.build();
    }

    private Block buildTypeClause(TypeClauseDescriptor clause, int clauseEnd)
    {
        JavaBlock.Builder clauseBlock = JavaBlock.builder(clause.keywordStart(), clauseEnd, "TypeClause")
                .indent(Indent.continuationIndent());
        List<Type> types = clause.types();
        Type firstType = types.getFirst();
        int firstTypeStart = firstType.getStartPosition();
        Block prefix = owner.buildTokensRange(clause.keywordStart(), firstTypeStart, "TypeClausePrefix");
        clauseBlock.child(prefix);
        Block prev = prefix;

        for (int index = 0; index < types.size(); index++) {
            Type type = types.get(index);
            int typeStart = type.getStartPosition();
            int typeEnd = typeStart + type.getLength();
            int itemEnd = typeEnd;
            if (index < types.size() - 1) {
                int comma = findCommaBetween(typeEnd, types.get(index + 1).getStartPosition());
                if (comma >= 0) {
                    itemEnd = comma + 1;
                }
            }

            Block item = JavaBlock.builder(typeStart, itemEnd, "TypeClauseItem")
                    .indent(index == 0 ? Indent.noneIndent() : Indent.relativeSpaceIndent(clause.keyword().length() + 1))
                    .child(owner.buildTokensRange(typeStart, itemEnd, "TypeClauseItemTokens"))
                    .build();
            Spacing spacing = index == 0
                    ? Spacing.oneSpace()
                    : Spacing.createSpacing(1, 1, 0, true, 0);
            JavaBlockBuilder.addSibling(clauseBlock, prev, item, spacing);
            prev = item;

            // Inter-item comments (e.g. `Foo, // note\n Bar`) must be covered
            // by their own block; otherwise they fall into a child-gap and
            // get dropped by ApplyChangesState.
            if (index < types.size() - 1) {
                int nextStart = types.get(index + 1).getStartPosition();
                int scanCursor = itemEnd;
                for (JavaTokens.Token tok : tokensIn(scanCursor, nextStart)) {
                    if (!tok.isComment()) {
                        continue;
                    }
                    int cEnd = tok.text().endsWith("\n") ? tok.end() - 1 : tok.end();
                    boolean inlineWithPrev = !containsLineBreak(scanCursor, tok.start());
                    Block commentBlock = JavaBlock.builder(tok.start(), cEnd, "TypeClauseInlineComment")
                            .indent(inlineWithPrev
                                    ? Indent.noneIndent()
                                    : Indent.relativeSpaceIndent(clause.keyword().length() + 1))
                            .child(commentLeaf(tok))
                            .build();
                    Spacing commentSpacing = inlineWithPrev
                            ? Spacing.createSpacing(1, 1, 0, false, 0)
                            : Spacing.createSpacing(0, 0, 1, true, 0);
                    JavaBlockBuilder.addSibling(clauseBlock, prev, commentBlock, commentSpacing);
                    prev = commentBlock;
                    scanCursor = cEnd;
                }
            }
        }

        if (prev.endOffset() < clauseEnd && !sourceOnlyWhitespace(prev.endOffset(), clauseEnd)) {
            Block trailing = owner.buildTokensRange(prev.endOffset(), clauseEnd, "TypeClauseTrailing");
            JavaBlockBuilder.addSibling(clauseBlock, prev, trailing, JavaSpacingRules.keepLineOrSpace());
        }

        return clauseBlock.build();
    }

    private List<TypeClauseDescriptor> typeClauseDescriptors(BodyDeclaration node, int start, int headerEnd)
    {
        List<TypeClauseDescriptor> clauses = new ArrayList<>();
        if (node instanceof TypeDeclaration typeDeclaration) {
            addTypeClauseDescriptor(clauses, start, headerEnd, "extends", typeDeclaration.getSuperclassType());
            addTypeClauseDescriptor(
                    clauses,
                    start,
                    headerEnd,
                    typeDeclaration.isInterface() ? "extends" : "implements",
                    types(typeDeclaration.superInterfaceTypes()));
            addTypeClauseDescriptor(clauses, start, headerEnd, "permits", types(typeDeclaration.permittedTypes()));
        }
        else if (node instanceof EnumDeclaration enumDeclaration) {
            addTypeClauseDescriptor(clauses, start, headerEnd, "implements", types(enumDeclaration.superInterfaceTypes()));
        }
        clauses.sort(Comparator.comparingInt(TypeClauseDescriptor::keywordStart));
        return clauses;
    }

    private void addTypeClauseDescriptor(List<TypeClauseDescriptor> clauses, int start, int headerEnd, String keyword, Type type)
    {
        addTypeClauseDescriptor(clauses, start, headerEnd, keyword, type == null ? List.of() : List.of(type));
    }

    private void addTypeClauseDescriptor(List<TypeClauseDescriptor> clauses, int start, int headerEnd, String keyword, List<Type> types)
    {
        if (types.isEmpty()) {
            return;
        }
        ASTNode firstType = types.getFirst();
        int searchEnd = Math.min(headerEnd, firstType.getStartPosition());
        int offset = findKeyword(start, searchEnd, keyword, true);
        if (offset >= 0) {
            clauses.add(new TypeClauseDescriptor(offset, keyword, types));
        }
    }

    private static List<Type> types(List<?> nodes)
    {
        List<Type> types = new ArrayList<>(nodes.size());
        for (Object node : nodes) {
            types.add((Type) node);
        }
        return List.copyOf(types);
    }

    private int findCommaBetween(int start, int end)
    {
        for (JavaTokens.Token token : tokensIn(start, end)) {
            if (token.type() == ITerminalSymbols.TokenNameCOMMA) {
                return token.start();
            }
        }
        return -1;
    }

    private record TypeClauseDescriptor(int keywordStart, String keyword, List<Type> types) {}

    private int findKeyword(int start, int end, String keyword)
    {
        return findKeyword(start, end, keyword, false);
    }

    /// Find a keyword token between `start` and `end`. When
    /// `skipInsideAngles` is true, track generic bound `<`/`>`
    /// balance and skip tokens that occur inside generic bounds — important
    /// for distinguishing the class's own `extends` clause from the
    /// `extends` used inside a generic bound like
    /// `<T extends Comparable<T>>`.
    private int findKeyword(int start, int end, String keyword, boolean skipInsideAngles)
    {
        int angleDepth = 0;
        for (JavaTokens.Token tok : tokens) {
            if (tok.start() >= end) {
                break;
            }
            if (tok.start() < start) {
                continue;
            }
            if (skipInsideAngles) {
                String text = tok.text();
                switch (text) {
                    case "<" -> {
                        angleDepth++;
                        continue;
                    }
                    case ">" -> {
                        if (angleDepth > 0) {
                            angleDepth--;
                        }
                        continue;
                    }
                    case ">>" -> {
                        angleDepth = max(0, angleDepth - 2);
                        continue;
                    }
                    case ">>>" -> {
                        angleDepth = max(0, angleDepth - 3);
                        continue;
                    }
                }
                if (angleDepth > 0) {
                    continue;
                }
            }
            if (tok.text().equals(keyword)) {
                return tok.start();
            }
        }
        return -1;
    }

    private Block buildRecordHeader(RecordDeclaration node, int start, int headerEnd)
    {
        JavaBlock.Builder header = JavaBlock.builder(start, headerEnd, "RecordHeader");
        int lparen = findRecordLParen(node);
        int rparen = findRecordRParen(node);
        if (lparen < 0 || rparen < 0) {
            header.child(owner.buildTokensRange(start, headerEnd, "RecordHeaderTokens"));
            return header.build();
        }
        // Prefix: up to and including `(`. If there's a wrapped annotation in
        // the prefix, decompose it so its value list carries CONTINUATION indent.
        Block prev;
        if (owner.hasWrappedAnnotationIn(start, lparen)) {
            prev = owner.emitPrefixWithAnnotations(header, start, lparen + 1);
        }
        else {
            Block prefix = owner.buildTokensRange(start, lparen + 1, "RecordHeaderPrefix");
            header.child(prefix);
            prev = prefix;
        }

        // Components: each component gets CONTINUATION indent.
        Object[] components = node.recordComponents().toArray();
        Block prevComponent = null;
        int prevCompEnd = lparen + 1;
        for (int i = 0; i < components.length; i++) {
            ASTNode comp = (ASTNode) components[i];
            int compStart = comp.getStartPosition();
            int compEnd = compStart + comp.getLength();
            // Emit any inter-component comments. A comment on the same line
            // as the previous component's trailing `,` stays inline (`splits,
            // // sourcePartition -> splits`); a comment on its own line goes
            // on its own line at continuation indent.
            for (JavaTokens.Token tok : tokensIn(prevCompEnd, compStart)) {
                if (!tok.isComment()) {
                    continue;
                }
                int cEnd = (tok.text().endsWith("\n")) ? tok.end() - 1 : tok.end();
                boolean inlineWithPrev = prev != null && !containsLineBreak(prevCompEnd, tok.start());
                Block commentBlock = JavaBlock.builder(tok.start(), cEnd, "RecordComponentComment")
                        .indent(inlineWithPrev ? Indent.noneIndent() : Indent.continuationIndent())
                        .child(commentLeaf(tok))
                        .build();
                Spacing sp = inlineWithPrev
                        ? Spacing.createSpacing(1, 1, 0, false, 0)
                        : Spacing.createSpacing(0, 0, 1, true, 0);
                JavaBlockBuilder.addSibling(header, prev, commentBlock, sp);
                prev = commentBlock;
            }
            // Scan forward for trailing `,` if not the last component.
            int trailingCommaEnd = compEnd;
            if (i < components.length - 1) {
                int next = findNextCommaOrClose(compEnd, rparen);
                if (next >= 0 && next < rparen) {
                    trailingCommaEnd = next + 1;
                }
            }
            Block componentBlock = buildComponentBlock(comp, compStart, trailingCommaEnd);
            Spacing spacingForComponent = (prevComponent == null)
                    // From `(` to first component: allow line break but prefer inline.
                    ? Spacing.createSpacing(0, 0, 0, true, 0)
                    // Between components: one space or line break.
                    : Spacing.createSpacing(1, 1, 0, true, 0);
            JavaBlockBuilder.addSibling(header, prev, componentBlock, spacingForComponent);
            prev = componentBlock;
            prevComponent = componentBlock;
            prevCompEnd = trailingCommaEnd;
        }

        // Trailing comments between the last component and `)`. A standalone
        // trailing comment should land at the same column as the components
        // above it; a same-line trailing comment stays inline with prev.
        for (JavaTokens.Token tok : tokensIn(prevCompEnd, rparen)) {
            if (!tok.isComment()) {
                continue;
            }
            int cEnd = tok.text().endsWith("\n") ? tok.end() - 1 : tok.end();
            boolean inlineWithPrev = prev != null && !containsLineBreak(prevCompEnd, tok.start());
            Block commentBlock;
            Spacing sp;
            if (inlineWithPrev) {
                commentBlock = JavaBlock.builder(tok.start(), cEnd, "RecordComponentComment")
                        .indent(Indent.noneIndent())
                        .child(commentLeaf(tok))
                        .build();
                sp = Spacing.createSpacing(1, 1, 0, false, 0);
            }
            else {
                // Use relative-to-parent space indent so the comment lines
                // up with components above (which also carry CONT). A plain
                // CONT composite here renders at col 0 because the header
                // composite's line anchor doesn't cascade to a "last-of"
                // sibling at this position in the tree; relative-8 pins to
                // the header's own column +8 directly.
                commentBlock = JavaBlock.builder(tok.start(), cEnd, "RecordComponentComment")
                        .indent(Indent.relativeSpaceIndent(Indent.CONTINUATION_SIZE))
                        .child(commentLeaf(tok))
                        .build();
                sp = Spacing.createSpacing(0, 0, 1, true, 0);
            }
            JavaBlockBuilder.addSibling(header, prev, commentBlock, sp);
            prev = commentBlock;
            prevCompEnd = cEnd;
        }

        List<Type> interfaces = types(node.superInterfaceTypes());
        if (interfaces.isEmpty()) {
            // Suffix: `)` and anything after.
            Block suffix = owner.buildTokensRange(rparen, headerEnd, "RecordHeaderSuffix");
            // `)` right after last component — no space, but allow a line break.
            JavaBlockBuilder.addSibling(header, prev, suffix, Spacing.createSpacing(0, 0, 0, true, 0));
            return header.build();
        }

        int implementsStart = findKeyword(rparen, interfaces.getFirst().getStartPosition(), "implements", true);
        if (implementsStart < 0) {
            Block suffix = owner.buildTokensRange(rparen, headerEnd, "RecordHeaderSuffix");
            JavaBlockBuilder.addSibling(header, prev, suffix, Spacing.createSpacing(0, 0, 0, true, 0));
            return header.build();
        }

        Block suffixPrefix = owner.buildTokensRange(rparen, implementsStart, "RecordHeaderSuffixPrefix");
        JavaBlockBuilder.addSibling(header, prev, suffixPrefix, Spacing.createSpacing(0, 0, 0, true, 0));
        Block implementsClause = buildTypeClause(new TypeClauseDescriptor(implementsStart, "implements", interfaces), headerEnd);
        JavaBlockBuilder.addSibling(header, suffixPrefix, implementsClause, JavaSpacingRules.keepLineOrSpace());
        return header.build();
    }

    private Block buildComponentBlock(ASTNode component, int start, int end)
    {
        // If the component spans multiple lines (typically because an
        // annotation has a wrapped value), decompose it so annotations get
        // proper CONTINUATION for their wrapped values.
        if (containsLineBreak(start, end)
                && component instanceof SingleVariableDeclaration svd) {
            return buildComponentWithDecomposedAnnotations(svd, start, end);
        }
        return JavaBlock.builder(start, end, "RecordComponent")
                .indent(Indent.continuationIndent())
                .child(owner.buildTokensRange(start, end, "RecordComponentTokens"))
                .build();
    }

    /// Build a record component where the source spans multiple lines,
    /// typically because an annotation has a wrapped value. Decomposes
    /// annotations via buildAnnotationBlock so wrapped values get CONTINUATION.
    private Block buildComponentWithDecomposedAnnotations(
            SingleVariableDeclaration svd,
            int start,
            int end)
    {
        JavaBlock.Builder component = JavaBlock.builder(start, end, "RecordComponent")
                .indent(Indent.continuationIndent());
        int cursor = start;
        Block prev = null;
        for (Object m : svd.modifiers()) {
            if (m instanceof Annotation ann && containsLineBreak(
                    ann.getStartPosition(),
                    ann.getStartPosition() + ann.getLength())) {
                int annStart = ann.getStartPosition();
                int annEnd = annStart + ann.getLength();
                if (cursor < annStart) {
                    Block before = owner.buildTokensRange(cursor, annStart, "RecordCompPre");
                    if (prev == null) {
                        component.child(before);
                    }
                    else {
                        JavaBlockBuilder.addSibling(component, prev, before, Spacing.oneSpace());
                    }
                    prev = before;
                }
                Block annBlock = owner.buildAnnotationBlock(ann);
                if (prev == null) {
                    component.child(annBlock);
                }
                else {
                    JavaBlockBuilder.addSibling(component, prev, annBlock, Spacing.oneSpace());
                }
                prev = annBlock;
                cursor = annEnd;
            }
        }
        if (cursor < end) {
            Block tail = owner.buildTokensRange(cursor, end, "RecordCompTail", false);
            if (prev == null) {
                component.child(tail);
            }
            else {
                JavaBlockBuilder.addSibling(component, prev, tail, Spacing.oneSpace());
            }
        }
        return component.build();
    }

    private int findRecordLParen(RecordDeclaration node)
    {
        int nameEnd = node.getName().getStartPosition() + node.getName().getLength();
        int end = node.getStartPosition() + node.getLength();
        for (JavaTokens.Token tok : tokensIn(nameEnd, end)) {
            if (tok.type() == ITerminalSymbols.TokenNameLPAREN) {
                return tok.start();
            }
        }
        return -1;
    }

    private int findRecordRParen(RecordDeclaration node)
    {
        int lparen = findRecordLParen(node);
        if (lparen < 0) {
            return -1;
        }
        int end = node.getStartPosition() + node.getLength();
        return findMatchingRParen(lparen, end);
    }

    private int findNextCommaOrClose(int start, int rparen)
    {
        int depth = 0;
        int limit = Math.min(rparen + 1, source.length());
        for (JavaTokens.Token tok : tokens) {
            if (tok.end() <= start) {
                continue;
            }
            if (tok.start() >= limit) {
                break;
            }
            if (tok.isComment()) {
                continue;
            }
            int type = tok.type();
            if (type == ITerminalSymbols.TokenNameLPAREN
                    || type == ITerminalSymbols.TokenNameLBRACKET
                    || type == ITerminalSymbols.TokenNameLESS
                    || type == ITerminalSymbols.TokenNameLBRACE) {
                depth++;
            }
            else if (type == ITerminalSymbols.TokenNameRPAREN
                    || type == ITerminalSymbols.TokenNameRBRACKET
                    || type == ITerminalSymbols.TokenNameGREATER
                    || type == ITerminalSymbols.TokenNameRBRACE) {
                if (depth == 0) {
                    return -1;
                }
                depth--;
            }
            else if (type == ITerminalSymbols.TokenNameCOMMA && depth == 0) {
                return tok.start();
            }
        }
        return -1;
    }

    private boolean containsLineBreak(int start, int end)
    {
        return sourceContext.containsLineBreak(start, end);
    }

    /// Returns the offset AFTER the first line break in [start, end), or -1.
    private int findFirstLineBreak(int start, int end)
    {
        int limit = Math.min(end, source.length());
        for (int i = max(0, start); i < limit; i++) {
            if (source.charAt(i) == '\n') {
                return i + 1;
            }
        }
        return -1;
    }

    /// True when `source[start..end)` contains at least two consecutive
    /// `\n` characters (a blank line).
    private boolean containsBlankLine(int start, int end)
    {
        int limit = Math.min(end, source.length());
        int newlines = 0;
        for (int i = max(0, start); i < limit; i++) {
            char c = source.charAt(i);
            if (c == '\n') {
                newlines++;
                if (newlines >= 2) {
                    return true;
                }
            }
            else if (c != ' ' && c != '\t' && c != '\r') {
                newlines = 0;
            }
        }
        return false;
    }

    /// True when `source[start..end)` contains only whitespace
    /// characters (no tokens, no comments, no other content).
    private boolean sourceOnlyWhitespace(int start, int end)
    {
        int limit = Math.min(end, source.length());
        for (int i = max(0, start); i < limit; i++) {
            if (!Character.isWhitespace(source.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private int firstNonWhitespaceAtOrAfter(int start, int end)
    {
        int limit = Math.min(end, source.length());
        for (int i = max(0, start); i < limit; i++) {
            if (!Character.isWhitespace(source.charAt(i))) {
                return i;
            }
        }
        return limit;
    }

    private int firstNonCommentTokenStart(int start, int end)
    {
        for (JavaTokens.Token tok : tokensIn(start, end)) {
            if (!tok.isComment()) {
                return tok.start();
            }
        }
        return end;
    }

    /// If a type-parameter list `<...>` opens before a line break in
    /// `[start, end)`, return the position of the opening `<`. Otherwise
    /// return -1. Used in `buildTypeHeader` to distinguish legitimate
    /// type-param wraps (e.g. `class Foo<T,\\n E>`) from source-shape
    /// annotation-on-own-line (e.g. `@Ann\\npublic class Foo`).
    private int findTypeParamOpenBeforeLineBreak(int start, int end)
    {
        int limit = Math.min(end, source.length());
        for (int i = max(0, start); i < limit; i++) {
            char c = source.charAt(i);
            if (c == '\n') {
                return -1;
            }
            if (c == '<' && genericOpens.contains(i)) {
                return i;
            }
        }
        return -1;
    }

    /// Expanded column width of a tab character when measuring source columns.
    private static final int TAB_WIDTH = 4;

    /// Column of a source offset (0-based: how many characters since the last \n).
    private int columnOf(int offset)
    {
        if (offset <= 0) {
            return 0;
        }
        int col = 0;
        for (int i = offset - 1; i >= 0; i--) {
            if (source.charAt(i) == '\n') {
                break;
            }
            col++;
        }
        return col;
    }

    /// Returns the column of the first non-whitespace character on the line containing `offset`.
    private int lineStartColumn(int offset)
    {
        int lineStart = offset;
        while (lineStart > 0 && source.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }
        int col = 0;
        for (int i = lineStart; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == ' ') {
                col++;
            }
            else if (c == '\t') {
                col += TAB_WIDTH;
            }
            else {
                return col;
            }
        }
        return col;
    }

    private boolean startsLine(int offset)
    {
        int lineStart = offset;
        while (lineStart > 0 && source.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }
        for (int i = lineStart; i < offset; i++) {
            char c = source.charAt(i);
            if (c != ' ' && c != '\t') {
                return false;
            }
        }
        return true;
    }

    /// Find a trailing line comment on the same line as the token at
    /// `stmtEnd`. Returns null if no such comment exists.
    private JavaTokens.Token findTrailingLineComment(int stmtEnd, int rangeEnd)
    {
        return JavaCommentBlocks.trailingInlineComment(tokens, source, stmtEnd, rangeEnd);
    }

    /// Indent for a comment: NORMAL indent for most comments, but
    /// first-column comments (column 0 in source) stay at column 0
    /// (KEEP_FIRST_COLUMN_COMMENT behavior).
    private Indent commentIndent(JavaTokens.Token tok, Indent defaultIndent)
    {
        return JavaCommentBlocks.indent(source, tok, defaultIndent);
    }

    /// Create a leaf block for a comment token. Line comments (`//...`) in
    /// JDT include the trailing `\\n` in their token text; strip it so
    /// the newline becomes part of the whitespace managed by the engine
    /// rather than being baked into the leaf text.
    private static Block commentLeaf(JavaTokens.Token tok)
    {
        return JavaCommentBlocks.leaf(tok);
    }

    /// Scan for comment tokens between two blocks and emit them as children
    /// of the composite. Returns the last emitted block (or `prev` if
    /// no comments were found).
    private Block emitInterBlockComments(JavaBlock.Builder composite, Block prev, int gapStart, int gapEnd)
    {
        return emitInterBlockComments(composite, prev, gapStart, gapEnd, Indent.normalIndent());
    }

    private Block emitInterBlockComments(JavaBlock.Builder composite, Block prev, int gapStart, int gapEnd, Indent defaultIndent)
    {
        for (JavaTokens.Token tok : tokensIn(gapStart, gapEnd)) {
            if (tok.isComment()) {
                int cEnd = (tok.text().endsWith("\n")) ? tok.end() - 1 : tok.end();
                Indent ind = commentIndent(tok, defaultIndent);
                Block commentBlock = JavaBlock.builder(tok.start(), cEnd, "InterBlockComment")
                        .indent(ind)
                        .child(commentLeaf(tok))
                        .build();
                // Force at least one line break before each comment leaf so the
                // engine's indent computation places it at the proper column;
                // markdown javadoc lines must each render on their own line.
                JavaBlockBuilder.addSibling(composite, prev, commentBlock, Spacing.createSpacing(0, 0, 1, true, 1));
                prev = commentBlock;
            }
        }
        return prev;
    }

    private int firstCommentStart(int start, int end)
    {
        for (JavaTokens.Token tok : tokensIn(start, end)) {
            if (tok.isComment()) {
                return tok.start();
            }
        }
        return end;
    }

    private BuiltMember buildIndentedMember(BodyDeclaration memberDecl, int bodyEnd, String debugName)
    {
        Block memberBlock = buildTypeOrDeclaration(memberDecl);
        int memberEnd = memberDecl.getStartPosition() + memberDecl.getLength();

        // A trailing comment after a member declaration sits outside the AST
        // node range, but it belongs to the declaration rather than the gap
        // before the next member.
        JavaTokens.Token trailingComment = findTrailingLineComment(memberEnd, bodyEnd);
        if (trailingComment != null) {
            int wrapperEnd = trailingComment.text().endsWith("\n") ? trailingComment.end() - 1 : trailingComment.end();
            JavaBlock.Builder memberWrapper = JavaBlock.builder(memberBlock.startOffset(), wrapperEnd, debugName + "WithTrailingComment")
                    .indent(Indent.normalIndent());
            memberWrapper.child(memberBlock);
            Block commentLeafBlock = commentLeaf(trailingComment);
            int sourceSpaces = max(1, trailingComment.start() - memberEnd);
            memberWrapper.spacing(memberBlock, commentLeafBlock, Spacing.createSpacing(sourceSpaces, sourceSpaces, 0, false, 0));
            memberWrapper.child(commentLeafBlock);
            return new BuiltMember(memberWrapper.build(), wrapperEnd);
        }

        Block wrapped = JavaBlock.builder(memberBlock.startOffset(), memberBlock.endOffset(), debugName)
                .indent(Indent.normalIndent())
                .child(memberBlock)
                .build();
        return new BuiltMember(wrapped, memberEnd);
    }

    private record BuiltMember(Block block, int endOffset) {}

    /// Like [#emitInterBlockComments] but emits the FIRST comment inline
    /// with the previous sibling (preserving source shape like
    /// `case VALUE -> // inline`). Subsequent comments go on new lines.
    private Block emitInterBlockCommentsArrowInline(JavaBlock.Builder composite, Block prev, int gapStart, int gapEnd)
    {
        boolean first = true;
        for (JavaTokens.Token tok : tokensIn(gapStart, gapEnd)) {
            if (tok.isComment()) {
                int cEnd = (tok.text().endsWith("\n")) ? tok.end() - 1 : tok.end();
                Indent ind = commentIndent(tok, Indent.normalIndent());
                Block commentBlock = JavaBlock.builder(tok.start(), cEnd, "InterBlockComment")
                        .indent(ind)
                        .child(commentLeaf(tok))
                        .build();
                Spacing sp;
                if (first && prev != null) {
                    // Preserve source column alignment: `}   // note` with 3
                    // spaces stays as 3, not collapsed to 1.
                    int sourceSpaces = max(1, tok.start() - prev.endOffset());
                    sp = Spacing.createSpacing(sourceSpaces, sourceSpaces, 0, false, 0);
                }
                else if (first) {
                    sp = Spacing.createSpacing(1, 1, 0, false, 0);
                }
                else {
                    sp = Spacing.createSpacing(0, 0, 1, true, 1);
                }
                JavaBlockBuilder.addSibling(composite, prev, commentBlock, sp);
                prev = commentBlock;
                first = false;
            }
        }
        return prev;
    }

    /// Walk back from `before` to the last non-whitespace character position + 1 (exclusive).
    private int lastNonWhitespaceBefore(int before, int lowerBound)
    {
        for (int i = Math.min(before, source.length()) - 1; i >= lowerBound; i--) {
            char c = source.charAt(i);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                return i + 1;
            }
        }
        return lowerBound;
    }

    /// Field declaration: if the initializer expression is complex (chain,
    /// call with wrapped args, lambda), decompose it; otherwise flat-tokenize.
    private Block buildFieldDeclaration(FieldDeclaration node)
    {
        int start = node.getStartPosition();
        int end = start + node.getLength();
        // No initializer / single-line: if a modifier annotation wraps, emit
        // the whole field as "prefix with annotations" so the wrapped
        // annotation's value list picks up CONTINUATION indent. Otherwise
        // flat tokens.
        VariableDeclarationFragment firstFrag = node.fragments().isEmpty()
                ? null
                : (VariableDeclarationFragment) node.fragments().getFirst();
        boolean hasSingleLineOrNoInit = firstFrag == null
                || firstFrag.getInitializer() == null
                || !containsLineBreak(start, end);
        if (hasSingleLineOrNoInit && owner.hasWrappedAnnotationIn(start, end)) {
            JavaBlock.Builder composite = JavaBlock.builder(start, end, "FieldDeclaration");
            owner.emitPrefixWithAnnotations(composite, start, end);
            return composite.build();
        }
        // Only decompose if there's exactly one fragment with an initializer
        // that contains a line break (multi-line expression).
        if (node.fragments().size() == 1) {
            VariableDeclarationFragment frag = (VariableDeclarationFragment) node.fragments().getFirst();
            Expression init = frag.getInitializer();
            if (init != null && containsLineBreak(start, end)) {
                int initStart = init.getStartPosition();
                int initEnd = initStart + init.getLength();
                int initPrefixStart = initStart;
                for (JavaTokens.Token tok : tokensIn(start, initStart)) {
                    if (tok.type() == ITerminalSymbols.TokenNameEQUAL) {
                        initPrefixStart = tok.end();
                    }
                }
                int prefixEnd = firstCommentStart(initPrefixStart, initStart);
                JavaBlock.Builder composite = JavaBlock.builder(start, end, "FieldDeclaration");
                // Prefix: type + name + `=` up to the initializer. When any
                // modifier annotation is wrapped, decompose it so its value
                // list picks up CONTINUATION indent.
                Block prefix;
                if (owner.hasWrappedAnnotationIn(start, prefixEnd)) {
                    JavaBlock.Builder p = JavaBlock.builder(start, prefixEnd, "FieldPrefix");
                    owner.emitPrefixWithAnnotations(p, start, prefixEnd);
                    prefix = p.build();
                }
                else {
                    prefix = owner.buildTokensRange(start, prefixEnd, "FieldPrefix");
                }
                composite.child(prefix);
                // Initializer: when the initializer is a text block — either
                // directly, or as the leading-left receiver of a chain like
                // {@code """...""".trim()} or {@code """...""".formatted(x)} —
                // force it onto a new line at CONTINUATION so the Airlift
                // "text block always starts on its own line" style is applied.
                Block initBlock;
                Spacing initSpacing;
                if (JavaBlockBuilder.leadingExpressionIsTextBlock(init)) {
                    Block textBlock = JavaBlock.builder(initStart, initEnd, "FieldTextBlock")
                            .child(owner.buildTokensRangePreservingTextBlockMargin(initStart, initEnd, "FieldTextBlockTokens"))
                            .build();
                    initBlock = owner.buildIndentedBodyWrapper(
                            "FieldTextBlockWrap",
                            prefixEnd,
                            initStart,
                            initEnd,
                            textBlock,
                            Indent.continuationIndent());
                    initSpacing = Spacing.createSpacing(0, 0, 1, false, 0);
                }
                else {
                    Block initInner = owner.buildExpressionBlock(init, initStart, initEnd, "FieldInit");
                    // When the initializer spans multiple lines (wrapped `=\n
                    // new ...`), wrap it in a continuation-indent composite so
                    // the first line sits at +8 from the field declaration's
                    // column. Without this the RHS lines up with the LHS and
                    // AssignmentBlankLineNormalizer has to shift every line in
                    // the RHS by +8 — which over-indents any nested structure
                    // the engine already placed correctly (e.g. anon-class
                    // members).
                    initBlock = owner.buildIndentedBodyWrapper(
                            "FieldInitWrap",
                            prefixEnd,
                            initStart,
                            initEnd,
                            initInner,
                            Indent.continuationIndent());
                    initSpacing = JavaSpacingRules.keepLineOrSpace();
                }
                JavaBlockBuilder.addSibling(composite, prefix, initBlock, initSpacing);
                // Trailing: `;` etc.
                if (initEnd < end) {
                    Block trailing = owner.buildTokensRange(initEnd, end, "FieldTrailing");
                    JavaBlockBuilder.addSibling(composite, initBlock, trailing, Spacing.none());
                }
                return composite.build();
            }
        }
        return owner.buildTokensSpanning(node, "field");
    }

    private Block buildMethodDeclaration(MethodDeclaration node)
    {
        int start = node.getStartPosition();
        int end = start + node.getLength();
        if (node.getBody() == null) {
            // Abstract/interface method with a wrapped throws clause: decompose
            // so each thrown type gets CONTINUATION indent individually.
            int abstractThrowsOffset = node.thrownExceptionTypes().isEmpty()
                    ? -1
                    : findKeyword(start, end, "throws", true);
            // Trigger the decomposed-throws path when the `throws` keyword
            // is on its own line OR when the throws items themselves wrap.
            // Either way we want the keyword / items at continuation indent.
            if (abstractThrowsOffset > 0
                    && (containsLineBreak(start, abstractThrowsOffset)
                    || containsLineBreak(abstractThrowsOffset, start + node.getLength()))) {
                JavaBlock.Builder m = JavaBlock.builder(start, end, "MethodDeclaration");
                int headerEnd = lastNonWhitespaceBefore(abstractThrowsOffset, start);
                Block mHeader = buildMethodHeader(node, start, headerEnd, "MethodHeader");
                m.child(mHeader);
                Block throwsClauseBlock = buildThrowsClauseBlock(node, abstractThrowsOffset, end);
                JavaBlockBuilder.addSibling(m, mHeader, throwsClauseBlock, Spacing.createSpacing(0, 0, 1, false, 0));
                return m.build();
            }
            // Abstract/interface method: decompose parameter list if wrapped
            // (including single-parameter wraps like `Foo getBy(\n String x);`).
            if (!node.parameters().isEmpty() && hasWrappedParameters(node)) {
                return buildMethodHeader(node, start, end, "MethodDeclaration");
            }
            if (hasMethodPrefixStandaloneLineComments(node, start, end)) {
                return buildMethodHeader(node, start, end, "MethodDeclaration");
            }
            // Wrapped annotations on the method: decompose so their values
            // get CONTINUATION indent.
            if (owner.hasWrappedAnnotationIn(start, end)) {
                return buildMethodHeader(node, start, end, "MethodDeclaration");
            }
            return owner.buildTokensRange(start, end, "MethodDeclaration");
        }
        JavaBlock.Builder composite = JavaBlock.builder(start, end, "MethodDeclaration");
        int bodyStart = node.getBody().getStartPosition();
        // Build the header in two parts when there is a `throws` clause:
        // header up to `throws`, then the throws clause on its own continuation
        // line. Airlift style: THROWS_KEYWORD_WRAP=ALWAYS, line break before
        // `throws` and CONTINUATION indent for the clause.
        int throwsOffset = node.thrownExceptionTypes().isEmpty()
                ? -1
                : findKeyword(start, bodyStart, "throws", true);
        Block header;
        Block throwsClause = null;
        if (throwsOffset > 0) {
            // lastNonWhitespaceBefore returns the position AFTER the last
            // non-whitespace character (exclusive). Use it directly as the
            // end of the pre-throws header — adding +1 would erroneously
            // include the trailing `\n` and create a 1-char PostParen block.
            int headerEnd = lastNonWhitespaceBefore(throwsOffset, start);
            // Single-parameter methods can wrap too (`public X(\n Type p)`);
            // use the call-expression decomposition for any wrapped-param
            // case so the parameter picks up CONTINUATION indent.
            header = buildMethodHeader(node, start, headerEnd, "MethodHeader");
            throwsClause = buildThrowsClauseBlock(node, throwsOffset, bodyStart);
        }
        else if (!node.parameters().isEmpty() && hasWrappedParameters(node)) {
            header = buildMethodHeader(node, start, bodyStart, "MethodHeader");
        }
        else if (hasMethodPrefixStandaloneLineComments(node, start, bodyStart)) {
            header = buildMethodHeader(node, start, bodyStart, "MethodHeader");
        }
        else if (owner.hasWrappedAnnotationIn(start, bodyStart)) {
            header = buildMethodHeader(node, start, bodyStart, "MethodHeader");
        }
        else {
            header = owner.buildTokensRange(start, bodyStart, "MethodHeader");
        }
        composite.child(header);
        Block lastBeforeBody = header;
        if (throwsClause != null) {
            // Force `throws` onto its own line.
            JavaBlockBuilder.addSibling(composite, header, throwsClause, Spacing.createSpacing(0, 0, 1, false, 0));
            lastBeforeBody = throwsClause;
        }
        Block body = owner.buildStatementBlock(node.getBody());
        Spacing headerToBody = spacingBeforeMethodBody(node, bodyStart);
        JavaBlockBuilder.addSibling(composite, lastBeforeBody, body, headerToBody);
        return composite.build();
    }

    private Spacing spacingBeforeMethodBody(MethodDeclaration node, int bodyStart)
    {
        if (!isEmptyMethodBodyWithoutComments(node)) {
            // CLASS_BRACE_STYLE=NEXT_LINE: non-empty method bodies put `{` on
            // its own line.
            return Spacing.createSpacing(0, 0, 1, false, 0);
        }

        int signatureStart = methodSignatureStart(node);
        int previousCodeEnd = lastNonWhitespaceBefore(bodyStart, signatureStart);
        boolean signatureWrapped = previousCodeEnd > signatureStart
                && containsLineBreak(signatureStart, previousCodeEnd);
        if (signatureWrapped) {
            return Spacing.createSpacing(0, 0, 1, false, 0);
        }
        return Spacing.createSpacing(1, 1, 0, false, 0);
    }

    private boolean isEmptyMethodBodyWithoutComments(MethodDeclaration node)
    {
        org.eclipse.jdt.core.dom.Block body = node.getBody();
        if (body == null || !body.statements().isEmpty()) {
            return false;
        }
        int bodyStart = body.getStartPosition();
        int bodyEnd = bodyStart + body.getLength();
        int openBrace = owner.firstChar(bodyStart, bodyEnd, '{');
        int closeBrace = owner.lastChar(bodyStart, bodyEnd, '}');
        return openBrace >= 0
                && closeBrace >= 0
                && !hasCommentIn(openBrace + 1, closeBrace);
    }

    private static int methodSignatureStart(MethodDeclaration node)
    {
        int start = node.getName().getStartPosition();
        Type returnType = node.getReturnType2();
        if (returnType != null) {
            start = Math.min(start, returnType.getStartPosition());
        }
        @SuppressWarnings("unchecked")
        List<TypeParameter> typeParameters = node.typeParameters();
        if (!typeParameters.isEmpty()) {
            start = Math.min(start, typeParameters.getFirst().getStartPosition());
        }
        return start;
    }

    private Block buildMethodHeader(MethodDeclaration node, int start, int end, String debugName)
    {
        List<JavaTokens.Token> prefixComments = methodPrefixStandaloneLineComments(node, start, end);
        if (prefixComments.isEmpty()) {
            return buildMethodHeaderSegment(node, start, end, debugName);
        }

        JavaBlock.Builder header = JavaBlock.builder(start, end, debugName);
        Block prev = null;
        int cursor = start;
        boolean previousCommentUsedMethodInternalIndent = false;
        JavaTokens.Token previousComment = null;
        for (JavaTokens.Token comment : prefixComments) {
            if (cursor < comment.start() && !sourceOnlyWhitespace(cursor, comment.start())) {
                Block segment = buildMethodHeaderSegment(node, cursor, comment.start(), "MethodHeaderSegment");
                prev = appendMethodHeaderPart(header, prev, segment, Spacing.createSpacing(0, 0, 1, true, 0));
            }

            boolean useMethodInternalIndent = methodPrefixCommentUsesMethodInternalIndent(node, comment.start());
            if (!useMethodInternalIndent
                    && previousCommentUsedMethodInternalIndent
                    && previousComment != null
                    && sourceOnlyWhitespace(previousComment.end(), comment.start())) {
                useMethodInternalIndent = true;
            }

            int commentEnd = comment.text().endsWith("\n") ? comment.end() - 1 : comment.end();
            Indent indent = useMethodInternalIndent
                    ? Indent.normalIndent()
                    : Indent.relativeSpaceIndent(0);
            Block commentBlock = JavaBlock.builder(comment.start(), commentEnd, "MethodPrefixComment")
                    .indent(indent)
                    .child(commentLeaf(comment))
                    .build();
            prev = appendMethodHeaderPart(header, prev, commentBlock, Spacing.createSpacing(0, 0, 1, true, 0));
            cursor = comment.end();
            previousComment = comment;
            previousCommentUsedMethodInternalIndent = useMethodInternalIndent;
        }

        if (cursor < end && !sourceOnlyWhitespace(cursor, end)) {
            int tailStart = firstNonWhitespaceAtOrAfter(cursor, end);
            Block tail = buildMethodHeaderSegment(node, tailStart, end, "MethodHeaderTail");
            appendMethodHeaderPart(header, prev, tail, Spacing.createSpacing(0, 0, 1, true, 0));
        }
        return header.build();
    }

    private Block appendMethodHeaderPart(JavaBlock.Builder header, Block prev, Block child, Spacing spacing)
    {
        if (prev == null) {
            header.child(child);
        }
        else {
            JavaBlockBuilder.addSibling(header, prev, child, spacing);
        }
        return child;
    }

    private Block buildMethodHeaderSegment(MethodDeclaration node, int start, int end, String debugName)
    {
        return buildMethodHeaderSegment(node, start, end, debugName, true);
    }

    private Block buildMethodHeaderSegment(MethodDeclaration node, int start, int end, String debugName, boolean canUseFirstChildIndent)
    {
        if (!node.parameters().isEmpty() && hasWrappedParameters(node) && node.getReceiverType() != null) {
            return buildReceiverParameterMethodHeader(node, start, end, debugName, canUseFirstChildIndent);
        }
        if (!node.parameters().isEmpty() && hasWrappedParameters(node) && node.getReceiverType() == null) {
            int lparen = owner.findLParen(node.parameters(), start);
            if (lparen >= start && lparen < end) {
                Block call = owner.buildCallExpression(null, node.parameters(), start, end);
                if (canUseFirstChildIndent) {
                    return call;
                }
                return JavaBlock.builder(start, end, debugName)
                        .canUseFirstChildIndent(false)
                        .child(call)
                        .build();
            }
        }
        if (owner.hasWrappedAnnotationIn(start, end)) {
            JavaBlock.Builder header = JavaBlock.builder(start, end, debugName);
            if (!canUseFirstChildIndent) {
                header.canUseFirstChildIndent(false);
            }
            owner.emitPrefixWithAnnotations(header, start, end);
            return header.build();
        }
        return owner.buildTokensRange(start, end, debugName, canUseFirstChildIndent);
    }

    private Block buildReceiverParameterMethodHeader(MethodDeclaration node, int start, int end, String debugName, boolean canUseFirstChildIndent)
    {
        int lparen = owner.findLParen(node.parameters(), start);
        int rparen = findMatchingRParen(lparen, end);
        if (lparen < start || rparen < 0 || rparen >= end) {
            return owner.buildTokensRange(start, end, debugName, canUseFirstChildIndent);
        }

        JavaBlock.Builder header = JavaBlock.builder(start, end, debugName);
        if (!canUseFirstChildIndent) {
            header.canUseFirstChildIndent(false);
        }

        Block prefix = owner.buildTokensRange(start, lparen + 1, "MethodHeaderPrefix");
        header.child(prefix);
        Block prev = prefix;

        int receiverStart = firstNonWhitespaceAtOrAfter(lparen + 1, end);
        int firstParameterStart = ((ASTNode) node.parameters().getFirst()).getStartPosition();
        int receiverEnd = lastNonWhitespaceBefore(firstParameterStart, receiverStart);
        if (receiverStart < receiverEnd) {
            Block receiver = JavaBlock.builder(receiverStart, receiverEnd, "ReceiverParameter")
                    .indent(Indent.continuationIndent())
                    .child(owner.buildTokensRange(receiverStart, receiverEnd, "ReceiverParameterTokens"))
                    .build();
            JavaBlockBuilder.addSibling(header, prev, receiver, JavaSpacingRules.keepLineOrSpace());
            prev = receiver;
        }

        for (int i = 0; i < node.parameters().size(); i++) {
            ASTNode parameter = (ASTNode) node.parameters().get(i);
            int parameterStart = parameter.getStartPosition();
            int parameterEnd;
            if (i + 1 < node.parameters().size()) {
                ASTNode next = (ASTNode) node.parameters().get(i + 1);
                parameterEnd = lastNonWhitespaceBefore(next.getStartPosition(), parameterStart);
            }
            else {
                parameterEnd = rparen + 1;
            }
            Block parameterBlock = JavaBlock.builder(parameterStart, parameterEnd, "MethodParameter")
                    .indent(Indent.continuationIndent())
                    .child(owner.buildTokensRange(parameterStart, parameterEnd, "MethodParameterTokens"))
                    .build();
            JavaBlockBuilder.addSibling(header, prev, parameterBlock, JavaSpacingRules.keepLineOrSpace());
            prev = parameterBlock;
        }

        if (rparen + 1 < end) {
            Block trailing = owner.buildTokensRange(rparen + 1, end, "MethodHeaderTrailing");
            JavaBlockBuilder.addSibling(header, prev, trailing, JavaSpacingRules.keepLineOrSpace());
        }
        return header.build();
    }

    private boolean hasMethodPrefixStandaloneLineComments(MethodDeclaration node, int start, int end)
    {
        return !methodPrefixStandaloneLineComments(node, start, end).isEmpty();
    }

    private List<JavaTokens.Token> methodPrefixStandaloneLineComments(MethodDeclaration node, int start, int end)
    {
        int nameStart = node.getName().getStartPosition();
        if (nameStart < 0) {
            return List.of();
        }
        int commentEnd = Math.min(nameStart, end);
        List<JavaTokens.Token> result = new ArrayList<>();
        for (JavaTokens.Token tok : tokensIn(start, commentEnd)) {
            if (tok.type() != ITerminalSymbols.TokenNameCOMMENT_LINE) {
                continue;
            }
            if (!startsLine(tok.start())) {
                continue;
            }
            if (insideMethodModifier(node, tok.start())) {
                continue;
            }
            if (lastMethodModifierBefore(node, tok.start()) == null) {
                continue;
            }
            result.add(tok);
        }
        return result;
    }

    private boolean insideMethodModifier(MethodDeclaration node, int offset)
    {
        for (Object modifier : node.modifiers()) {
            if (!(modifier instanceof ASTNode modifierNode)) {
                continue;
            }
            int modifierStart = modifierNode.getStartPosition();
            int modifierEnd = modifierStart + modifierNode.getLength();
            if (modifierStart < offset && offset < modifierEnd) {
                return true;
            }
        }
        return false;
    }

    private boolean methodPrefixCommentUsesMethodInternalIndent(MethodDeclaration node, int commentStart)
    {
        ASTNode modifier = lastMethodModifierBefore(node, commentStart);
        if (modifier == null) {
            return false;
        }
        int modifierStart = modifier.getStartPosition();
        int modifierEnd = modifierStart + modifier.getLength();
        return containsLineBreak(modifierStart, modifierEnd)
                && sourceOnlyWhitespace(modifierEnd, commentStart);
    }

    private ASTNode lastMethodModifierBefore(MethodDeclaration node, int offset)
    {
        ASTNode last = null;
        int lastEnd = -1;
        for (Object modifier : node.modifiers()) {
            if (!(modifier instanceof ASTNode modifierNode)) {
                continue;
            }
            int modifierEnd = modifierNode.getStartPosition() + modifierNode.getLength();
            if (modifierEnd <= offset && modifierEnd > lastEnd) {
                last = modifierNode;
                lastEnd = modifierEnd;
            }
        }
        return last;
    }

    /// Build a decomposed throws clause: `throws` keyword plus each
    /// thrown type (with leading annotations) as a sibling. Each child gets
    /// [Indent#continuationIndent()] so annotated items like
    /// `@ThriftId(1) ExceptionA` land at method-indent + CONTINUATION
    /// on their own line. Mirrors IntelliJ's `ExtendsListBlock` which
    /// assigns `continuationIndent` to every list-item child.
    private Block buildThrowsClauseBlock(MethodDeclaration node, int throwsOffset, int end)
    {
        // The clause itself carries CONTINUATION so it lands +8 when placed
        // on its own line; items below get CONT relative to the clause's
        // `throws` column.
        JavaBlock.Builder clause = JavaBlock.builder(throwsOffset, end, "ThrowsClause")
                .indent(Indent.continuationIndent());
        int throwsEnd = throwsOffset + "throws".length();
        Block kw = JavaBlock.builder(throwsOffset, throwsEnd, "ThrowsKeyword")
                .child(owner.buildTokensRange(throwsOffset, throwsEnd, "ThrowsKeywordTokens"))
                .build();
        clause.child(kw);
        Block prev = kw;

        List<?> types = node.thrownExceptionTypes();
        boolean abstractMethod = node.getBody() == null;
        // Comments between `throws` and the first exception type —
        // `throws\n //note\n IOException`. Without this they fall into the
        // gap and disappear.
        if (!types.isEmpty()) {
            int firstItemStart = ((ASTNode) types.getFirst()).getStartPosition();
            for (JavaTokens.Token tok : tokensIn(throwsEnd, firstItemStart)) {
                if (!tok.isComment()) {
                    continue;
                }
                int cEnd = tok.text().endsWith("\n") ? tok.end() - 1 : tok.end();
                Block commentBlock = JavaBlock.builder(tok.start(), cEnd, "ThrowsLeadingComment")
                        .child(commentLeaf(tok))
                        .build();
                JavaBlockBuilder.addSibling(clause, prev, commentBlock, Spacing.createSpacing(0, 0, 1, true, 0));
                prev = commentBlock;
            }
        }
        for (int i = 0; i < types.size(); i++) {
            org.eclipse.jdt.core.dom.Type t = (org.eclipse.jdt.core.dom.Type) types.get(i);
            int itemStart = t.getStartPosition();
            int itemEnd = itemStart + t.getLength();
            if (i < types.size() - 1) {
                // Extend range to include trailing `,` (but not `;` or the body brace).
                for (int p = itemEnd; p < end; p++) {
                    char c = source.charAt(p);
                    if (c == ',') {
                        itemEnd = p + 1;
                        break;
                    }
                    if (!Character.isWhitespace(c)) {
                        break;
                    }
                }
            }
            else if (abstractMethod) {
                // Last item of an abstract/interface method — extend through
                // the trailing `;`. Without this the semicolon lives outside
                // every leaf block and the renderer drops it.
                itemEnd = end;
            }
            itemEnd = extendThroughTrailingInlineComments(itemEnd, end);
            Block item = JavaBlock.builder(itemStart, itemEnd, "ThrowsItem")
                    .child(owner.buildTokensRange(itemStart, itemEnd, "ThrowsItemTokens"))
                    .build();
            Spacing sp = containsLineBreak(prev.endOffset(), itemStart)
                    ? Spacing.createSpacing(0, 0, 1, false, 0)
                    : Spacing.createSpacing(1, 1, 0, true, 0);
            JavaBlockBuilder.addSibling(clause, prev, item, sp);
            prev = item;
        }
        return clause.build();
    }

    /// Do a method declaration's parameters span multiple source lines?
    /// Considers a single parameter that sits on its own line (after a
    /// line-break following `(`) as wrapped too.
    boolean hasWrappedParameters(MethodDeclaration node)
    {
        List<?> params = node.parameters();
        if (params.isEmpty()) {
            return false;
        }
        ASTNode first = (ASTNode) params.getFirst();
        ASTNode last = (ASTNode) params.getLast();
        int firstStart = first.getStartPosition();
        int lastEnd = last.getStartPosition() + last.getLength();
        int lparen = owner.findLParen(params, node.getStartPosition());
        if (lparen < 0) {
            return false;
        }
        if (containsLineBreak(lparen, firstStart)) {
            return true;
        }
        if (params.size() >= 2 && containsLineBreak(firstStart, lastEnd)) {
            return true;
        }
        return false;
    }
}
