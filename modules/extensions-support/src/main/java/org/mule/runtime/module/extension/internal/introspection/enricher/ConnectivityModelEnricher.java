/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.fluent.BaseDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.OperationDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.SourceDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.WithOperationsDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.WithSourcesDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ExtensionParameter;
import org.mule.runtime.module.extension.internal.introspection.describer.model.WithParameters;
import org.mule.runtime.module.extension.internal.introspection.describer.model.runtime.MethodWrapper;
import org.mule.runtime.module.extension.internal.introspection.describer.model.runtime.TypeBasedComponentWrapper;
import org.mule.runtime.module.extension.internal.model.property.ConfigTypeModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ConnectivityModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.util.IdempotentDeclarationWalker;

import java.util.List;
import java.util.Optional;

/**
 * {@link ModelEnricher} implementation that walks through a {@link ExtensionDeclaration} and looks for annotated
 * component parameters (Sources and Operations), with {@link Connection} and adds a {@link ConnectivityModelProperty}
 * or annotated with {@link UseConfig} and adds a {@link ConfigTypeModelProperty}
 *
 * @since 4.0
 */
public class ConnectivityModelEnricher implements ModelEnricher
{

    private ClassTypeLoader typeLoader;

    @Override
    public void enrich(DescribingContext describingContext)
    {
        final Optional<ImplementingTypeModelProperty> optionalImplementingProperty = describingContext
                .getExtensionDeclarer()
                .getDeclaration()
                .getModelProperty(ImplementingTypeModelProperty.class);

        if (optionalImplementingProperty.isPresent())
        {
            typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(Thread.currentThread().getContextClassLoader());
            new IdempotentDeclarationWalker()
            {
                @Override
                public void onOperation(WithOperationsDeclaration owner, OperationDeclaration declaration)
                {
                    declaration.getModelProperty(ImplementingMethodModelProperty.class)
                            .ifPresent(implementingProperty -> contribute(declaration, new MethodWrapper(implementingProperty.getMethod())));
                }

                @Override
                public void onSource(WithSourcesDeclaration owner, SourceDeclaration declaration)
                {
                    declaration.getModelProperty(ImplementingTypeModelProperty.class)
                            .ifPresent(implementingProperty -> contribute(declaration, new TypeBasedComponentWrapper(implementingProperty.getType())));
                }
            }.walk(describingContext.getExtensionDeclarer().getDeclaration());
        }
    }

    private void contribute(BaseDeclaration declaration, WithParameters methodWrapper)
    {
        final List<ExtensionParameter> connectionParameters = methodWrapper.getParametersAnnotatedWith(Connection.class);
        if (!connectionParameters.isEmpty())
        {
            declaration.addModelProperty(new ConnectivityModelProperty(connectionParameters.get(0).getMetadataType(typeLoader)));
        }
    }
}
