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
package io.airlift.airstyle.normalizer;

import io.airlift.airstyle.model.SourceModel;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestAstRewrites
{
    @Test
    void testFormatterFixesRewriteFailuresByThrowing()
    {
        AST ast = SourceModel.create("class Test {}").compilationUnit().getAST();
        ASTRewrite rewrite = new BrokenAstRewrite(ast);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> AstRewrites.apply("class Test {}", rewrite));

        assertEquals("AST rewrite failed", exception.getMessage());
        assertTrue(
                exception.getCause() instanceof BadLocationException || exception.getCause() instanceof MalformedTreeException,
                "Expected a JDT rewrite failure cause");
    }

    private static final class BrokenAstRewrite
            extends ASTRewrite
    {
        private BrokenAstRewrite(AST ast)
        {
            super(ast);
        }

        @Override
        public TextEdit rewriteAST(IDocument document, Map options)
        {
            return new ReplaceEdit(document.getLength() + 1, 1, "x");
        }
    }
}
