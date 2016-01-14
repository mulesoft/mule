/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.enricher;

import org.mule.extension.api.introspection.declaration.DescribingContext;
import org.mule.extension.api.introspection.declaration.fluent.Declaration;
import org.mule.extension.api.introspection.declaration.fluent.DeclarationDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.extension.api.introspection.declaration.fluent.ParameterizedDeclaration;
import org.mule.module.extension.internal.model.AbstractAnnotatedModelEnricher;
import org.mule.module.extension.internal.model.property.DeclaringMemberModelProperty;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Stream;

/**
 * A base class to be used to enrich a {@link ParameterDeclaration} when the associated {@link DeclaringMemberModelProperty} has a field that was annotated with a specific {@link Annotation}.
 *
 * @since 4.0
 */
public abstract class AbstractAnnotatedParameterModelEnricher extends AbstractAnnotatedModelEnricher
{

    @Override
    public void enrich(DescribingContext describingContext)
    {
        DeclarationDescriptor descriptor = describingContext.getDeclarationDescriptor();
        Declaration declaration = descriptor.getRootDeclaration().getDeclaration();

        enrichParameters(declaration.getConfigurations());
        enrichParameters(declaration.getConnectionProviders());
        enrichParameters(declaration.getOperations());
    }

    private void enrichParameters(List<? extends ParameterizedDeclaration> models)
    {
        Stream<ParameterDeclaration> stream = models.stream().flatMap(declaration -> declaration.getParameters().stream());
        stream.forEach(this::addModelPropertyIfRequired);
    }

    /**
     * Adds the property model provided by {@link #getModelProperty(ParameterDeclaration)} if the parameter declaration has a {@link DeclaringMemberModelProperty} , and it contains a field annotated with the annotation provided by {@link #getAnnotationClass()}.
     */
    private void addModelPropertyIfRequired(ParameterDeclaration parameterDeclaration)
    {
        DeclaringMemberModelProperty declaringMember = parameterDeclaration.getModelProperty(DeclaringMemberModelProperty.KEY);
        if (declaringMember != null)
        {
            Field field = declaringMember.getDeclaringField();
            if (field.isAnnotationPresent(getAnnotationClass()))
            {
                parameterDeclaration.addModelProperty(getModelPropertyKey(parameterDeclaration), getModelProperty(parameterDeclaration));
            }
        }
    }

    /**
     * Gets the annotation that this model enricher will look for.
     */
    protected abstract Class<? extends Annotation> getAnnotationClass();

    /**
     * Gets the key that will be used to populate a model property
     */
    protected abstract String getModelPropertyKey(ParameterDeclaration parameterDeclaration);

    /**
     * Gets an instance of the model property this enricher will contribute.
     *
     * @return an immutable instance of a model property.
     */
    protected abstract Object getModelProperty(ParameterDeclaration parameterDeclaration);
}
