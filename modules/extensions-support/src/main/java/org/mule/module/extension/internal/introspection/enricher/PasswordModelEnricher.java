/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.enricher;

import org.mule.extension.api.annotation.param.display.Password;
import org.mule.extension.api.introspection.property.ImmutablePasswordModelProperty;
import org.mule.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.extension.api.introspection.property.PasswordModelProperty;

import java.lang.annotation.Annotation;

/**
 * Enriches a {@link ParameterDeclaration} with a model property which key is {@link PasswordModelProperty#KEY} and the value an instance of {@link PasswordModelProperty} when the associated field is annotated with {@link Password}.
 *
 * @since 4.0
 */
public final class PasswordModelEnricher extends AbstractAnnotatedParameterModelEnricher
{

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<? extends Annotation> getAnnotationClass()
    {
        return Password.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModelPropertyKey(ParameterDeclaration parameterDeclaration)
    {
        return PasswordModelProperty.KEY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object getModelProperty(ParameterDeclaration parameterDeclaration)
    {
        return new ImmutablePasswordModelProperty();
    }
}
