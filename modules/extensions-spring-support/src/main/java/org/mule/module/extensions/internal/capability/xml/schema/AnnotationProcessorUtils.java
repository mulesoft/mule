/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.capability.xml.schema;

import com.google.common.collect.ImmutableMap;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.StringTokenizer;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;

import org.apache.commons.lang.StringUtils;

/**
 * Utility class for annotation processing using the {@link javax.annotation.processing.Processor} API
 *
 * @since 3.7.0
 */
final class AnnotationProcessorUtils
{

    private AnnotationProcessorUtils()
    {
    }

    static Map<String, VariableElement> getFieldsAnnotatedWith(TypeElement typeElement, Class<? extends Annotation> annotation)
    {
        ImmutableMap.Builder<String, VariableElement> fields = ImmutableMap.builder();

        for (VariableElement variableElement : ElementFilter.fieldsIn(typeElement.getEnclosedElements()))
        {
            if (variableElement.getAnnotation(annotation) != null)
            {
                fields.put(variableElement.getSimpleName().toString(), variableElement);
            }
        }

        return fields.build();
    }

    static Map<String, ExecutableElement> getMethodsAnnotatedWith(TypeElement typeElement, Class<? extends Annotation> annotation)
    {
        ImmutableMap.Builder<String, ExecutableElement> fields = ImmutableMap.builder();

        for (ExecutableElement executableElement : ElementFilter.methodsIn(typeElement.getEnclosedElements()))
        {
            if (executableElement.getAnnotation(annotation) != null)
            {
                fields.put(executableElement.getSimpleName().toString(), executableElement);
            }
        }

        return fields.build();
    }


    static MethodDocumentation getMethodDocumentation(ProcessingEnvironment processingEnv, Element element)
    {
        final StringBuilder parsedComment = new StringBuilder();
        final ImmutableMap.Builder<String, String> parameters = ImmutableMap.builder();
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
                parsedComment.append(bodyLine).append('\n');
            }
        });

        return new MethodDocumentation(stripTags(parsedComment.toString()), parameters.build());
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
                parsedComment.append(bodyLine).append('\n');
            }
        });

        return stripTags(parsedComment.toString());
    }

    private static String stripTags(String comment)
    {
        StringBuilder builder = new StringBuilder();
        boolean insideTag = false;
        comment = comment.trim();

        for (int i = 0; i < comment.length(); i++)
        {
            if (comment.charAt(i) == '{' &&
                comment.charAt(i + 1) == '@')
            {
                insideTag = true;
                i++; //skip
                continue;
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
               strippedComments.charAt(strippedComments.length() - 1) == '\n')
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
            if (nextToken.startsWith("@param"))
            {
                handler.onParam(nextToken);
            }
            else if (!nextToken.startsWith("@"))
            {
                handler.onBodyLine(nextToken);
            }
        }
    }

    private static void parseMethodParameter(ImmutableMap.Builder<String, String> parameters, String param)
    {
        param = param.replaceFirst("@param", StringUtils.EMPTY).trim();
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
