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
package io.airlift.airstyle;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

import java.util.HashMap;
import java.util.Map;

public final class JavaLanguageSupport
{
    private static final String LATEST_JAVA_VERSION = CompilerOptions.getLatestVersion();

    private JavaLanguageSupport() {}

    public static String latestJavaVersion()
    {
        return LATEST_JAVA_VERSION;
    }

    public static int latestAstLevel()
    {
        return AST.getJLSLatest();
    }

    public static Map<String, String> compilerOptions()
    {
        Map<String, String> options = new HashMap<>(JavaCore.getOptions());
        options.put(JavaCore.COMPILER_SOURCE, LATEST_JAVA_VERSION);
        options.put(JavaCore.COMPILER_COMPLIANCE, LATEST_JAVA_VERSION);
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, LATEST_JAVA_VERSION);
        options.put(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, JavaCore.ENABLED);
        options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
        return Map.copyOf(options);
    }
}
