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
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/// Expands wildcard (`.*`) imports to explicit single-type imports for every
/// name referenced in the file. Leaves the star import in place if the set of
/// used names cannot be determined safely (e.g. when an on-demand import is
/// shadowed by another star import).
///
/// ### Example
///
/// Before:
/// ```java
/// import java.util.*;
///
/// class Test
/// {
///     List<String> names = new ArrayList<>();
/// }
/// ```
///
/// After:
/// ```java
/// import java.util.ArrayList;
/// import java.util.List;
///
/// class Test
/// {
///     List<String> names = new ArrayList<>();
/// }
/// ```
public final class AvoidStarImportNormalizer
{
    private static final Set<String> COMMON_JAVA_LANG_TYPES = Set.of(
            "String",
            "Object",
            "Class",
            "Boolean",
            "Byte",
            "Character",
            "Short",
            "Integer",
            "Long",
            "Float",
            "Double",
            "Void",
            "Throwable",
            "Exception",
            "RuntimeException",
            "Error",
            "AssertionError",
            "IllegalArgumentException",
            "IllegalStateException",
            "NullPointerException",
            "UnsupportedOperationException",
            "Iterable",
            "Enum",
            "Comparable",
            "CharSequence",
            "Number");

    private AvoidStarImportNormalizer() {}

    public static String normalize(String source)
    {
        CompilationUnit compilationUnit = SourceModel.create(source).compilationUnit();
        Usage usage = collectUsage(compilationUnit);
        List<ImportExpansion> expansions = collectExpansions(compilationUnit, usage);
        if (expansions.isEmpty()) {
            return source;
        }

        ASTRewrite rewrite = ASTRewrite.create(compilationUnit.getAST());
        ListRewrite imports = rewrite.getListRewrite(compilationUnit, CompilationUnit.IMPORTS_PROPERTY);
        for (ImportExpansion expansion : expansions) {
            imports.remove(expansion.starImport(), null);
            for (String explicit : expansion.explicitImports()) {
                ImportDeclaration importDeclaration = compilationUnit.getAST().newImportDeclaration();
                importDeclaration.setStatic(expansion.starImport().isStatic());
                importDeclaration.setOnDemand(false);
                importDeclaration.setName(compilationUnit.getAST().newName(explicit));
                imports.insertLast(importDeclaration, null);
            }
        }

        return AstRewrites.apply(source, rewrite);
    }

    private static Usage collectUsage(CompilationUnit compilationUnit)
    {
        Set<String> usedTypeNames = new HashSet<>();
        Set<String> usedAnnotationNames = new HashSet<>();
        Set<String> unqualifiedMethodNames = new HashSet<>();
        Set<String> declaredTypeNames = new HashSet<>();
        Set<String> typeParameterNames = new HashSet<>();

        compilationUnit.accept(new ASTVisitor()
        {
            @Override
            public boolean visit(SimpleType node)
            {
                if (node.getName() instanceof SimpleName simpleName) {
                    usedTypeNames.add(simpleName.getIdentifier());
                }
                return true;
            }

            @Override
            public boolean visit(MarkerAnnotation node)
            {
                if (node.getTypeName() instanceof SimpleName simpleName) {
                    usedAnnotationNames.add(simpleName.getIdentifier());
                }
                return true;
            }

            @Override
            public boolean visit(NormalAnnotation node)
            {
                if (node.getTypeName() instanceof SimpleName simpleName) {
                    usedAnnotationNames.add(simpleName.getIdentifier());
                }
                return true;
            }

            @Override
            public boolean visit(SingleMemberAnnotation node)
            {
                if (node.getTypeName() instanceof SimpleName simpleName) {
                    usedAnnotationNames.add(simpleName.getIdentifier());
                }
                return true;
            }

            @Override
            public boolean visit(MethodInvocation node)
            {
                if (node.getExpression() == null) {
                    unqualifiedMethodNames.add(node.getName().getIdentifier());
                }
                return true;
            }

            @Override
            public boolean visit(TypeDeclaration node)
            {
                declaredTypeNames.add(node.getName().getIdentifier());
                for (Object typeParameterObject : node.typeParameters()) {
                    TypeParameter typeParameter = (TypeParameter) typeParameterObject;
                    typeParameterNames.add(typeParameter.getName().getIdentifier());
                }
                return true;
            }

            @Override
            public boolean visit(MethodDeclaration node)
            {
                for (Object typeParameterObject : node.typeParameters()) {
                    TypeParameter typeParameter = (TypeParameter) typeParameterObject;
                    typeParameterNames.add(typeParameter.getName().getIdentifier());
                }
                return true;
            }
        });

        return new Usage(usedTypeNames, usedAnnotationNames, unqualifiedMethodNames, declaredTypeNames, typeParameterNames);
    }

    private static List<ImportExpansion> collectExpansions(CompilationUnit compilationUnit, Usage usage)
    {
        List<ImportDeclaration> starImports = new ArrayList<>();
        for (Object importObject : compilationUnit.imports()) {
            ImportDeclaration importDeclaration = (ImportDeclaration) importObject;
            if (importDeclaration.isOnDemand() && !isModuleImport(importDeclaration)) {
                starImports.add(importDeclaration);
            }
        }

        List<ImportExpansion> expansions = new ArrayList<>();
        for (ImportDeclaration starImport : starImports) {
            Set<String> explicitImports = new TreeSet<>();
            String name = starImport.getName().getFullyQualifiedName();

            if (starImport.isStatic()) {
                explicitImports.addAll(expandStaticStar(name, usage.unqualifiedMethodNames()));
            }
            else {
                explicitImports.addAll(expandTypeStar(name, usage));
            }

            if (!explicitImports.isEmpty()) {
                expansions.add(new ImportExpansion(starImport, explicitImports));
            }
        }
        return expansions;
    }

    private static Set<String> expandTypeStar(String packageName, Usage usage)
    {
        Set<String> explicitImports = new TreeSet<>();
        Set<String> candidates = new TreeSet<>();
        candidates.addAll(usage.usedTypeNames());
        candidates.addAll(usage.usedAnnotationNames());

        for (String candidate : candidates) {
            if (usage.declaredTypeNames().contains(candidate) || usage.typeParameterNames().contains(candidate)) {
                continue;
            }
            if (COMMON_JAVA_LANG_TYPES.contains(candidate)) {
                continue;
            }
            if (classExists(packageName + "." + candidate)) {
                explicitImports.add(packageName + "." + candidate);
            }
        }

        return explicitImports;
    }

    private static Set<String> expandStaticStar(String ownerClass, Set<String> methodCandidates)
    {
        Set<String> explicitImports = new TreeSet<>();
        Class<?> owner = loadClass(ownerClass);
        if (owner != null) {
            Set<String> staticMethods = new HashSet<>();
            for (Method method : owner.getMethods()) {
                if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) && java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                    staticMethods.add(method.getName());
                }
            }

            Set<String> staticFields = new HashSet<>();
            for (Field field : owner.getFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && java.lang.reflect.Modifier.isPublic(field.getModifiers())) {
                    staticFields.add(field.getName());
                }
            }

            for (String candidate : methodCandidates) {
                if (staticMethods.contains(candidate) || staticFields.contains(candidate)) {
                    explicitImports.add(ownerClass + "." + candidate);
                }
            }
            return explicitImports;
        }
        return explicitImports;
    }

    private static boolean classExists(String className)
    {
        return loadClass(className) != null;
    }

    private static Class<?> loadClass(String className)
    {
        try {
            return Class.forName(className, false, AvoidStarImportNormalizer.class.getClassLoader());
        }
        catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    private static boolean isModuleImport(ImportDeclaration importDeclaration)
    {
        return Modifier.isModule(importDeclaration.getModifiers());
    }

    private record Usage(
            Set<String> usedTypeNames,
            Set<String> usedAnnotationNames,
            Set<String> unqualifiedMethodNames,
            Set<String> declaredTypeNames,
            Set<String> typeParameterNames) {}

    private record ImportExpansion(ImportDeclaration starImport, Set<String> explicitImports) {}
}
