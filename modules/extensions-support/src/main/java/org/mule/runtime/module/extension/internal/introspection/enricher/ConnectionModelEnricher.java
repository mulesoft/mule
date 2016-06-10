/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.fluent.OperationDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.module.extension.internal.model.property.ConnectionTypeModelProperty;
import org.mule.runtime.module.extension.internal.runtime.connector.ConnectionInterceptor;
import org.mule.runtime.module.extension.internal.util.IdempotentDeclarationWalker;

/**
 * Adds a {@link ConnectionInterceptor} to all {@link OperationModel operations} which
 * contain the {@link ConnectionTypeModelProperty}
 *
 * @since 4.0
 */
public class ConnectionModelEnricher implements ModelEnricher
{

    @Override
    public void enrich(DescribingContext describingContext)
    {
        new IdempotentDeclarationWalker()
        {
            @Override
            protected void onOperation(OperationDeclaration declaration)
            {
                declaration.getModelProperty(ConnectionTypeModelProperty.class).ifPresent(p -> declaration.addInterceptorFactory(ConnectionInterceptor::new));
            }
        }.walk(describingContext.getExtensionDeclarer().getDeclaration());
    }
}
