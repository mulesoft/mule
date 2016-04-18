/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import org.mule.runtime.extension.api.introspection.ModelProperty;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ParameterizedDeclaration;
import org.mule.runtime.module.extension.internal.model.property.DeclaringMemberModelProperty;

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
        ExtensionDeclarer descriptor = describingContext.getExtensionDeclarer();
        ExtensionDeclaration extensionDeclaration = descriptor.getDeclaration();

        enrichParameters(extensionDeclaration.getConfigurations());
        enrichParameters(extensionDeclaration.getConnectionProviders());
        enrichParameters(extensionDeclaration.getOperations());
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
        parameterDeclaration.getModelProperty(DeclaringMemberModelProperty.class).ifPresent(declaringMember -> {
            Field field = declaringMember.getDeclaringField();
            if (field.isAnnotationPresent(getAnnotationClass()))
            {
                parameterDeclaration.addModelProperty(getModelProperty(parameterDeclaration));
            }
        });
    }

    /**
     * Gets the annotation that this model enricher will look for.
     */
    protected abstract Class<? extends Annotation> getAnnotationClass();

    /**
     * Gets an instance of the model property this enricher will contribute.
     *
     * @return an immutable instance of a model property.
     */
    protected abstract ModelProperty getModelProperty(ParameterDeclaration parameterDeclaration);
}
