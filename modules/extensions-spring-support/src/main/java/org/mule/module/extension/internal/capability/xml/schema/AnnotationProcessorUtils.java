/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.capability.xml.schema;

import org.mule.extension.annotations.Operation;
import org.mule.extension.annotations.Parameter;
import org.mule.extension.annotations.ParameterGroup;
import org.mule.util.ClassUtils;

import com.google.common.collect.ImmutableMap;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;

import org.apache.commons.lang.StringUtils;

/**
 * Utility class for annotation processing using the {@link javax.annotation.processing.Processor} API
 *
 * @since 3.7.0
 */
public final class AnnotationProcessorUtils
{

    private static final char NEW_LINE_CHAR = '\n';
    private static final char AT_CHAR = '@';
    public static final String PARAM = "@param";

    /**
     * Returns the {@link Class} object that is associated to the {@code typeElement}
     *
     * @param typeElement           a {@link TypeElement} which represents a {@link Class}
     * @param processingEnvironment the current {@link ProcessingEnvironment}
     * @param <T>                   the generic type of the returned {@link Class}
     * @return the {@link Class} represented by {@code typeElement}
     */
    public static <T> Class<T> classFor(TypeElement typeElement, ProcessingEnvironment processingEnvironment)
    {
        try
        {
            return ClassUtils.loadClass(processingEnvironment.getElementUtils().getBinaryName(typeElement).toString(), typeElement.getClass());
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }

    private AnnotationProcessorUtils()
    {
    }

    /**
     * Returns the {@link TypeElement}s in the
     * {@code roundEnvironment} which are annotated
     * with {@code annotationType}
     *
     * @param annotationType   the type of the {@link Annotation}
     * @param roundEnvironment the current {@link RoundEnvironment}
     * @return a {@link Set} with the {@link TypeElement}s annotated with {@code annotationType}
     */
    public static Set<TypeElement> getTypeElementsAnnotatedWith(Class<? extends Annotation> annotationType, RoundEnvironment roundEnvironment)
    {
        return ElementFilter.typesIn(roundEnvironment.getElementsAnnotatedWith(annotationType));
    }

    /**
     * Scans all classes in the {@code roundEnvironment} looking
     * for methods annotated with {@link Operation}.
     *
     * @param roundEnvironment the current {@link RoundEnvironment}
     * @return a {@link Map} which keys are the method names and the values are the
     * method represented as a {@link ExecutableElement}
     */
    static Map<String, ExecutableElement> getOperationMethods(RoundEnvironment roundEnvironment)
    {
        ImmutableMap.Builder<String, ExecutableElement> methods = ImmutableMap.builder();
        for (Element rootElement : roundEnvironment.getRootElements())
        {
            if (!(rootElement instanceof TypeElement))
            {
                continue;
            }

            methods.putAll(getMethodsAnnotatedWith((TypeElement) rootElement, Operation.class));
        }

        return methods.build();
    }

    static Map<String, VariableElement> getFieldsAnnotatedWith(TypeElement typeElement, Class<? extends Annotation> annotation)
    {
        return collectAnnotatedElements(ElementFilter.fieldsIn(typeElement.getEnclosedElements()), annotation);
    }

    static Map<String, ExecutableElement> getMethodsAnnotatedWith(TypeElement typeElement, Class<? extends Annotation> annotation)
    {
        return collectAnnotatedElements(ElementFilter.methodsIn(typeElement.getEnclosedElements()), annotation);
    }

    private static <T extends Element> Map<String, T> collectAnnotatedElements(Iterable<T> elements, Class<? extends Annotation> annotation)
    {
        ImmutableMap.Builder<String, T> fields = ImmutableMap.builder();

        for (T element : elements)
        {
            if (element.getAnnotation(annotation) != null)
            {
                fields.put(element.getSimpleName().toString(), element);
            }
        }

        return fields.build();
    }


    static MethodDocumentation getMethodDocumentation(ProcessingEnvironment processingEnv, Element element)
    {
        final StringBuilder parsedComment = new StringBuilder();
        final Map<String, String> parameters = new HashMap<>();
        parseJavaDoc(processingEnv, element, new JavadocParseHandler()
        {
            @Override
            void onParam(String param)
            {
                parseMethodParameter(parameters, param);
            }

            @Override
            void onBodyLine(String bodyLine)
            {
                parsedComment.append(bodyLine).append(NEW_LINE_CHAR);
            }
        });


        parseOperationParameterGroups(processingEnv, (MethodSymbol) element, parameters);


        return new MethodDocumentation(stripTags(parsedComment.toString()), parameters);
    }

    /**
     * Traverses the arguments of {@code methodElement} and for each
     * argument annotated with {@link ParameterGroup} it invokes
     * {@link #getOperationParameterGroupDocumentation(TypeElement, Map, ProcessingEnvironment)}
     *
     * @param processingEnv the current {@link ProcessingEnvironment}
     * @param methodElement the operation method being processed
     * @param parameterDocs a {@link Map} which keys are attribute names and values are their documentation
     */
    private static void parseOperationParameterGroups(ProcessingEnvironment processingEnv, MethodSymbol methodElement, Map<String, String> parameterDocs)
    {
        for (VarSymbol parameterSymbol : methodElement.getParameters())
        {
            for (Attribute.Compound compound : parameterSymbol.getAnnotationMirrors())
            {
                DeclaredType annotationType = compound.getAnnotationType();
                if (annotationType != null)
                {
                    Class<? extends Annotation> annotationClass = classFor((TypeElement) compound.getAnnotationType().asElement(), processingEnv);
                    if (ParameterGroup.class.isAssignableFrom(annotationClass))
                    {
                        try
                        {
                            getOperationParameterGroupDocumentation((TypeElement) processingEnv.getTypeUtils().asElement(parameterSymbol.asType()), parameterDocs, processingEnv);
                        }
                        catch (Exception e)
                        {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Extracts the documentation of the parameters
     * in a group described by {@code groupElement}.
     * The obtained docs are added to {@code parameterDocs}
     *
     * @param groupElement          a {@link TypeElement} representing the parameter group
     * @param parameterDocs         a {@link Map} which keys are attribute names and values are their documentation
     * @param processingEnvironment the current {@link ProcessingEnvironment}
     */
    private static void getOperationParameterGroupDocumentation(TypeElement groupElement,
                                                                final Map<String, String> parameterDocs,
                                                                ProcessingEnvironment processingEnvironment)
    {
        for (final Map.Entry<String, VariableElement> field : getFieldsAnnotatedWith(groupElement, Parameter.class).entrySet())
        {
            parseJavaDoc(processingEnvironment, field.getValue(), new JavadocParseHandler()
            {
                @Override
                void onParam(String param)
                {
                }

                @Override
                void onBodyLine(String bodyLine)
                {
                    parameterDocs.put(field.getKey(), bodyLine);
                }
            });
        }

        for (VariableElement field : getFieldsAnnotatedWith(groupElement, ParameterGroup.class).values())
        {

            getOperationParameterGroupDocumentation((TypeElement) processingEnvironment.getTypeUtils().asElement(field.asType()),
                                                    parameterDocs, processingEnvironment);
        }
    }

    static String getJavaDocSummary(ProcessingEnvironment processingEnv, Element element)
    {
        final StringBuilder parsedComment = new StringBuilder();
        parseJavaDoc(processingEnv, element, new JavadocParseHandler()
        {
            @Override
            void onParam(String param)
            {
            }

            @Override
            void onBodyLine(String bodyLine)
            {
                parsedComment.append(bodyLine).append(NEW_LINE_CHAR);
            }
        });

        return stripTags(parsedComment.toString());
    }

    private static String stripTags(String comment)
    {
        StringBuilder builder = new StringBuilder();
        boolean insideTag = false;
        comment = comment.trim();

        final int length = comment.length();
        for (int i = 0; i < length; i++)
        {
            if (comment.charAt(i) == '{')
            {
                int nextCharIndex = i + 1;
                if (nextCharIndex < length && comment.charAt(nextCharIndex) == AT_CHAR)
                {
                    insideTag = true;
                    i = nextCharIndex;
                    continue;
                }
            }
            else if (comment.charAt(i) == '}' && insideTag)
            {
                insideTag = false;
                continue;
            }

            builder.append(comment.charAt(i));
        }

        String strippedComments = builder.toString().trim();
        while (strippedComments.length() > 0 &&
               strippedComments.charAt(strippedComments.length() - 1) == NEW_LINE_CHAR)
        {
            strippedComments = StringUtils.chomp(strippedComments);
        }

        return strippedComments;
    }

    private static void parseJavaDoc(ProcessingEnvironment processingEnv, Element element, JavadocParseHandler handler)
    {
        String comment = extractJavadoc(processingEnv, element);

        StringTokenizer st = new StringTokenizer(comment, "\n\r");
        while (st.hasMoreTokens())
        {
            String nextToken = st.nextToken().trim();
            if (nextToken.startsWith(PARAM))
            {
                handler.onParam(nextToken);
            }
            else if (!(nextToken.charAt(0) == AT_CHAR))
            {
                handler.onBodyLine(nextToken);
            }
        }
    }

    private static void parseMethodParameter(Map<String, String> parameters, String param)
    {
        param = param.replaceFirst(PARAM, StringUtils.EMPTY).trim();
        int descriptionIndex = param.indexOf(" ");
        String paramName = param.substring(0, descriptionIndex).trim();
        String description = param.substring(descriptionIndex).trim();

        parameters.put(paramName, description);
    }

    private static String extractJavadoc(ProcessingEnvironment processingEnv, Element element)
    {
        String comment = processingEnv.getElementUtils().getDocComment(element);

        if (StringUtils.isBlank(comment))
        {
            return StringUtils.EMPTY;
        }

        return comment.trim();
    }

    private static abstract class JavadocParseHandler
    {

        abstract void onParam(String param);

        abstract void onBodyLine(String bodyLine);

    }
}
