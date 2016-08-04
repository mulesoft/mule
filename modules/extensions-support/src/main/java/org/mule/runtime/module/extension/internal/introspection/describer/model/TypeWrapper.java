/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer.model;

import static java.util.stream.Collectors.toList;

import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

/**
 * Wrapper for {@link Class} that provide utility methods to facilitate the introspection of a {@link Class}
 *
 * @param <T> Class that the {@link TypeWrapper} represents
 * @since 4.0
 */
public class TypeWrapper<T> implements Annotated, WithName, WithAlias
{

    private final Class<T> aClass;

    TypeWrapper(Class<T> aClass)
    {
        this.aClass = aClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Annotation[] getAnnotations()
    {
        return aClass.getAnnotations();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName()
    {
        return aClass.getSimpleName();
    }

    /**
     * {@inheritDoc}
     */
    public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationClass)
    {
        return Optional.ofNullable(aClass.getAnnotation(annotationClass));
    }

    /**
     * @return A list of {@link FieldWrapper} that represent the list of {@link Field} that the class {@link T} declares
     */
    public List<FieldWrapper> getFields()
    {
        return IntrospectionUtils.getFields(aClass)
                .stream()
                .map(FieldWrapper::new)
                .collect(toList());
    }

    /**
     * @param annotation class that the fields of this type should be annoted with
     * @return A list of {@link FieldWrapper} that represent the list of {@link Field} that the class {@link T} declares
     * and are annotated with the given annotation
     */
    public List<FieldWrapper> getAnnotatedFields(Class<? extends Annotation> annotation)
    {
        return getFields()
                .stream()
                .filter(field -> field.isAnnotatedWith(annotation))
                .collect(toList());
    }

    /**
     * @return the class that {@link TypeWrapper} represents
     */
    public Class<T> getDeclaredClass()
    {
        return aClass;
    }
}