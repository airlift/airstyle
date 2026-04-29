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

import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

final class AstRewrites
{
    private AstRewrites() {}

    /// Applies `rewrite` to `source`.
    static String apply(String source, ASTRewrite rewrite)
    {
        try {
            IDocument document = new Document(source);
            TextEdit edit = rewrite.rewriteAST(document, null);
            edit.apply(document);
            return document.get();
        }
        catch (MalformedTreeException | BadLocationException e) {
            throw new IllegalStateException("AST rewrite failed", e);
        }
    }
}
