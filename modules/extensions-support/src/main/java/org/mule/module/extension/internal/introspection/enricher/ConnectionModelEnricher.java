/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.enricher;

import org.mule.extension.api.introspection.OperationModel;
import org.mule.extension.api.introspection.declaration.DescribingContext;
import org.mule.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.module.extension.internal.model.property.ConnectionTypeModelProperty;
import org.mule.module.extension.internal.runtime.connector.ConnectionInterceptor;

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
        describingContext.getDeclarationDescriptor().getDeclaration().getOperations().stream()
                .filter(operation -> operation.getModelProperty(ConnectionTypeModelProperty.KEY) != null)
                .forEach(operation -> operation.addInterceptorFactory(ConnectionInterceptor::new));
    }
}
