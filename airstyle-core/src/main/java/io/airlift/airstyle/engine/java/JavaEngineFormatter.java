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
import io.airlift.airstyle.engine.FormatProcessor;
import io.airlift.airstyle.model.SourceModel;

/// Public entry point for formatting Java source through the layout engine.
/// Wired in as the sole FORMAT-stage phase of `AirstyleFormatter`: it
/// produces the final indent and spacing decisions for every Java construct
/// the engine covers. A small set of legacy pre/post-format normalizers
/// (notably `WrappedListNormalizer`) still cooperate for a handful of
/// chain/lambda edge cases the engine does not yet handle.
public final class JavaEngineFormatter
{
    private JavaEngineFormatter() {}

    /// Format `source` through the engine. Returns the input unchanged
    /// if the source has parse errors (consistent with how the existing
    /// pipeline handles syntactic failures).
    public static String format(String source)
    {
        SourceModel model = SourceModel.create(source);
        if (model.hasErrors()) {
            return source;
        }
        Block root = JavaBlockBuilder.build(model.compilationUnit(), source);
        return new FormatProcessor().format(root, source);
    }
}
