/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.enricher;

import org.mule.extension.annotation.api.param.display.Password;
import org.mule.extension.api.introspection.declaration.DescribingContext;
import org.mule.extension.api.introspection.declaration.fluent.Declaration;
import org.mule.extension.api.introspection.declaration.fluent.DeclarationDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.extension.api.introspection.property.PasswordModelProperty;
import org.mule.module.extension.internal.introspection.ImmutablePasswordModelProperty;
import org.mule.module.extension.internal.model.AbstractAnnotatedModelEnricher;
import org.mule.module.extension.internal.model.property.DeclaringMemberModelProperty;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Supplier;

/**
 * Enriches the {@link DeclarationDescriptor} with a model property which key is {@link PasswordModelProperty#KEY} and the value an instance of {@link PasswordModelProperty}.
 *
 * @since 4.0
 */
public final class PasswordModelEnricher extends AbstractAnnotatedModelEnricher
{

    @Override
    public void enrich(DescribingContext describingContext)
    {
        DeclarationDescriptor descriptor = describingContext.getDeclarationDescriptor();
        Declaration declaration = descriptor.getRootDeclaration().getDeclaration();

        //TODO MULE-9083 Refactor to use a Parameterizable interface to make this polymorphic
        declaration.getConfigurations().forEach(parameterProvider ->
                                                        enrichParameters(() -> parameterProvider.getParameters()));

        declaration.getConnectionProviders().forEach(parameterProvider ->
                                                             enrichParameters(() -> parameterProvider.getParameters()));
        declaration.getOperations().forEach(parameterProvider ->
                                                    enrichParameters(() -> parameterProvider.getParameters()));
    }

    private void enrichParameters(Supplier<List<ParameterDeclaration>> parameters)
    {
        parameters.get().forEach(this::addModelPropertyIfRequired);
    }

    private void addModelPropertyIfRequired(ParameterDeclaration parameterDeclaration)
    {
        DeclaringMemberModelProperty declaringMember = parameterDeclaration.getModelProperty(DeclaringMemberModelProperty.KEY);
        if (declaringMember != null)
        {
            Field field = declaringMember.getDeclaringField();
            if (field.isAnnotationPresent(Password.class))
            {
                parameterDeclaration.addModelProperty(PasswordModelProperty.KEY, new ImmutablePasswordModelProperty());
            }
        }
    }
}
