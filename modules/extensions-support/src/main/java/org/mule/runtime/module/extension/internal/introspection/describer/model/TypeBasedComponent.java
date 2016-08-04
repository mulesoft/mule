/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer.model;

import static java.util.stream.Collectors.toList;

import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.UseConfig;

import com.google.common.collect.ImmutableList;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * {@link TypeWrapper} specification that for extension type based components (Sources, Configurations, Connection
 * providers and extensions).
 *
 * @param <T> type that the {@link TypeBasedComponent} represents
 * @since 4.0
 */
public class TypeBasedComponent<T> extends TypeWrapper<T> implements WithParameters
{

    public TypeBasedComponent(Class<T> aClass)
    {
        super(aClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ExtensionParameter> getParameters()
    {
        return ImmutableList.<ExtensionParameter>builder()
                .addAll(getAnnotatedFields(Parameter.class))
                .addAll(getAnnotatedFields(ParameterGroup.class))
                .addAll(getAnnotatedFields(Connection.class))
                .addAll(getAnnotatedFields(UseConfig.class))
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ExtensionParameter> getParameterGroups()
    {
        return ImmutableList
                .<ExtensionParameter>builder()
                .addAll(getAnnotatedFields(ParameterGroup.class))
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ExtensionParameter> getParametersAnnotatedWith(Class<? extends Annotation> annotationClass)
    {
        return getParameters()
                .stream()
                .filter(field -> field.getAnnotation(annotationClass).isPresent())
                .collect(toList());
    }
}
