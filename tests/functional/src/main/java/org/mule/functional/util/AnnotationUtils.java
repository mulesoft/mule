/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for annotations related stuff.
 *
 * @since 4.0
 */
public class AnnotationUtils
{

    private AnnotationUtils()
    {
    }

    /**
     * Looks for the {@link Annotation} in the {@link Class} and invokes the {@link Method}. It will return the result of the invocation.
     * If there {@link Class} is not annotated it will return the default value from the {@link Annotation}.
     *
     * @param klass the {@link Class} where to look for the annotation
     * @param annotationClass the {@link Annotation} class to look for
     * @param methodName the method name of the annotation to be called
     * @param <T> the result of the invocation
     * @throws IllegalStateException if the method is not defined in the annotation
     * @return the attribute from the annotation for the given class
     */
    public static <T> T getAnnotationAttributeFrom(Class<?> klass, Class<? extends Annotation> annotationClass, String methodName)
    {
        T extensions;
        Annotation annotation = klass.getAnnotation(annotationClass);
        Method method;
        try
        {
            method = annotationClass.getMethod(methodName);

            if (annotation != null)
            {
                extensions = (T) method.invoke(annotation);
            }
            else
            {
                extensions = (T) method.getDefaultValue();

            }
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Cannot read default " + methodName + " from " + annotationClass);
        }

        return extensions;
    }

    /**
     * Looks for the {@link Annotation} in the {@link Class} and invokes the {@link Method} in the whole hierarhcy until it
     * reaches {@link Object}. It will return a {@link List<T>} with the results of each invocation.
     * If there {@link Class} is not annotated it will return the default value from the {@link Annotation}.
     *
     * @param klass the {@link Class} where to look for the annotation
     * @param annotationClass the {@link Annotation} class to look for
     * @param methodName the method name of the annotation to be called
     * @param <T> the result of the invocation
     * @throws IllegalStateException if the method is not defined in the annotation
     * @return a (@link List} of T for the attributes annotated in all the object hierarchy until it reaches {@link Object} class.
     */
    public static <T> List<T> getAnnotationAttributeFromHierarchy(Class<?> klass, Class<? extends Annotation> annotationClass, String methodName)
    {
        List<T> extensions = new ArrayList<>();
        Class<?> currentClass = klass;
        while (currentClass != Object.class)
        {
            T attributeFrom = getAnnotationAttributeFrom(currentClass, annotationClass, methodName);
            if (attributeFrom != null)
            {
                extensions.add(attributeFrom);
            }
            currentClass = currentClass.getSuperclass();
        }
        return extensions;
    }
}
