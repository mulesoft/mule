/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.enricher;

import org.mule.extension.api.introspection.declaration.DescribingContext;
import org.mule.extension.api.introspection.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.extension.api.introspection.property.ConnectionHandlingTypeModelProperty;
import org.mule.module.extension.internal.model.property.ImmutableConnectionHandlingTypeModelProperty;


/**
 * Model Enricher for {@link org.mule.extension.api.introspection.ConnectionProviderModel} which describes
 * the Connection Handling Type for a given {@link org.mule.api.connection.ConnectionProvider}.
 *
 * @since 4.0
 */
public final class ConnectionHandlingEnricher implements ModelEnricher
{

    @Override
    public void enrich(DescribingContext describingContext)
    {
        for (ConnectionProviderDeclaration declaration : describingContext.getDeclarationDescriptor().getDeclaration().getConnectionProviders())
        {
            declaration.addModelProperty(ConnectionHandlingTypeModelProperty.KEY, new ImmutableConnectionHandlingTypeModelProperty(declaration.getFactory().newInstance()));
        }
    }
}
