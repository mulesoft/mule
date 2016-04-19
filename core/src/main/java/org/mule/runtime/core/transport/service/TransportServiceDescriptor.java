/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transport.service;

import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.endpoint.EndpointBuilder;
import org.mule.runtime.core.api.endpoint.EndpointURIBuilder;
import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.registry.ServiceDescriptor;
import org.mule.runtime.core.api.transaction.TransactionFactory;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transport.Connector;
import org.mule.runtime.core.api.transport.MessageDispatcherFactory;
import org.mule.runtime.core.api.transport.MessageReceiver;
import org.mule.runtime.core.api.transport.MessageRequesterFactory;
import org.mule.runtime.core.api.transport.MuleMessageFactory;
import org.mule.runtime.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.runtime.core.message.SessionHandler;

import java.util.List;
import java.util.Properties;

/**
 * <code>TransportServiceDescriptor</code> describes the necessary information for
 * creating a connector from a service descriptor. A service descriptor should be
 * located at META-INF/services/org/mule/providers/<protocol> where protocol is the
 * protocol of the connector to be created The service descriptor is in the form of
 * string key value pairs and supports a number of properties, descriptions of which
 * can be found here: http://www.mulesoft.org/documentation/display/MULE3USER/Transport+Service+Descriptors
 */
public interface TransportServiceDescriptor extends ServiceDescriptor, MuleContextAware
{
    public static final String OSGI_HEADER_TRANSPORT = "Mule-Transport";

    MuleMessageFactory createMuleMessageFactory() throws TransportServiceException;

    SessionHandler createSessionHandler() throws TransportServiceException;

    MessageReceiver createMessageReceiver(Connector connector,
                                                 FlowConstruct flowConstruct,
                                                 InboundEndpoint endpoint) throws MuleException;

    MessageReceiver createMessageReceiver(Connector connector,
                                                 FlowConstruct flowConstruct,
                                                 InboundEndpoint endpoint,
                                                 Object... args) throws MuleException;

    MessageDispatcherFactory createDispatcherFactory() throws TransportServiceException;

    MessageRequesterFactory createRequesterFactory() throws TransportServiceException;

    TransactionFactory createTransactionFactory() throws TransportServiceException;

    Connector createConnector() throws TransportServiceException;

    List<Transformer> createInboundTransformers(ImmutableEndpoint endpoint) throws TransportFactoryException;

    List<Transformer> createOutboundTransformers(ImmutableEndpoint endpoint) throws TransportFactoryException;

    List<Transformer> createResponseTransformers(ImmutableEndpoint endpoint) throws TransportFactoryException;

    EndpointURIBuilder createEndpointURIBuilder() throws TransportFactoryException;

    @Deprecated
    EndpointBuilder createEndpointBuilder(String uri) throws TransportFactoryException;

    /**
     * Creates a {@link EndpointBuilder}
     *
     * @param uri  address for the created endpoints
     * @param muleContext context of the application owning endpoint builder
     * @return a non null endpoint builder for the given address
     * @throws TransportFactoryException
     */
    EndpointBuilder createEndpointBuilder(String uri, MuleContext muleContext) throws TransportFactoryException;

    @Deprecated
    EndpointBuilder createEndpointBuilder(EndpointURIEndpointBuilder builder) throws TransportFactoryException;

    /**
     * Creates a {@link EndpointBuilder} wrapping an existing builder
     *
     * @param builder  instance to be wrapped
     * @param muleContext context of the application owning endpoint builder
     * @return a non null endpoint builder for the given builder
     * @throws TransportFactoryException
     */
    EndpointBuilder createEndpointBuilder(EndpointURIEndpointBuilder builder, MuleContext muleContext) throws TransportFactoryException;

    void setExceptionMappings(Properties props);

    Properties getExceptionMappings();

    List<MessageExchangePattern> getInboundExchangePatterns() throws TransportServiceException;

    List<MessageExchangePattern> getOutboundExchangePatterns() throws TransportServiceException;

    MessageExchangePattern getDefaultExchangePattern() throws TransportServiceException;
}
