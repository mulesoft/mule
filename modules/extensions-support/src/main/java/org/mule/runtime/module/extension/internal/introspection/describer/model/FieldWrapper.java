/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer.model;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Optional;

import org.springframework.core.ResolvableType;

/**
 * Wrapper for {@link Field} that provide utility methods to facilitate the introspection of a {@link Field}
 *
 * @since 4.0
 */
public class FieldWrapper implements ExtensionParameter
{

    private final Field field;

    public FieldWrapper(Field field)
    {
        this.field = field;
    }

    /**
     * @return the wrapped {@link Field}
     */
    public Field getField()
    {
        return field;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName()
    {
        return field.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Annotation[] getAnnotations()
    {
        return field.getAnnotations();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationClass)
    {
        return Optional.ofNullable(field.getAnnotation(annotationClass));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TypeWrapper getType()
    {
        return new TypeWrapper<>(field.getType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetadataType getMetadataType(ClassTypeLoader typeLoader)
    {
        return typeLoader.load(ResolvableType.forField(field).getType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAlias()
    {
        return InfrastructureTypeMapping.getMap().getOrDefault(field.getType(), ExtensionParameter.super.getAlias());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFieldBased()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOwner()
    {
        return "Class " + field.getDeclaringClass().getSimpleName();
    }
}