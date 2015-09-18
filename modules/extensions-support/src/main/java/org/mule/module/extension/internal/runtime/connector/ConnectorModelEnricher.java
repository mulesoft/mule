/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.connector;

import static java.lang.String.format;
import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.extension.annotations.connector.Connector;
import org.mule.api.extension.connection.ConnectionHandler;
import org.mule.api.extension.introspection.declaration.DescribingContext;
import org.mule.api.extension.introspection.declaration.fluent.BaseDeclaration;
import org.mule.api.extension.introspection.declaration.fluent.ConfigurationDeclaration;
import org.mule.api.extension.introspection.declaration.fluent.Declaration;
import org.mule.api.extension.runtime.InterceptorFactory;
import org.mule.module.extension.internal.model.AbstractAnnotatedModelEnricher;
import org.mule.util.ClassUtils;

/**
 * Traverses all the {@link ConfigurationDeclaration configuration declarations} in the supplied
 * {@link DescribingContext} looking for those which were generated from a class annotated with
 * {@link Connector}.
 * <p/>
 * The matching {@link ConfigurationDeclaration declarations} are enriched with a {@link InterceptorFactory}
 * that creates instances of {@link ConnectionInterceptor}
 * <p/>
 * {@link #extractAnnotation(BaseDeclaration, Class)} is used to determine if a {@link ConfigurationDeclaration} is
 * annotated with {@link Connector} or not.
 *
 * @since 4.0
 */
public final class ConnectorModelEnricher extends AbstractAnnotatedModelEnricher
{

    @Override
    public void enrich(DescribingContext describingContext)
    {
        Declaration declaration = describingContext.getDeclarationDescriptor().getDeclaration();
        declaration.getConfigurations().forEach(configurationDeclaration -> {
            Connector connectorAnnotation = extractAnnotation(configurationDeclaration, Connector.class);
            if (connectorAnnotation != null)
            {
                configurationDeclaration.addInterceptorFactory(createConnectionInterceptorFactory(configurationDeclaration, connectorAnnotation));
            }
        });
    }

    private InterceptorFactory createConnectionInterceptorFactory(BaseDeclaration<? extends BaseDeclaration> declaration, Connector connectorAnnotation)
    {
        return () -> new ConnectionInterceptor<>(createConnectionHandler(declaration, connectorAnnotation));
    }

    private ConnectionHandler<?, ?> createConnectionHandler(BaseDeclaration<? extends BaseDeclaration> declaration, Connector connectorAnnotation)
    {
        ConnectionHandler<?, ?> connectionHandler;
        try
        {
            connectionHandler = ClassUtils.instanciateClass(connectorAnnotation.value());
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(createStaticMessage(format(
                    "Could not instantiate ConnectionHandler of type '%s' for configuration of type '%s'",
                    connectorAnnotation.value().getName(), extractExtensionType(declaration).getName())), e);
        }
        return connectionHandler;
    }
}
