/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.service;

import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointURIBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.registry.ServiceDescriptor;
import org.mule.api.service.Service;
import org.mule.api.transaction.TransactionFactory;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageAdapter;
import org.mule.api.transport.MessageDispatcherFactory;
import org.mule.api.transport.MessageReceiver;
import org.mule.api.transport.MessageRequesterFactory;
import org.mule.api.transport.SessionHandler;

import java.util.List;
import java.util.Properties;

/**
 * <code>TransportServiceDescriptor</code> describes the necessary information for
 * creating a connector from a service descriptor. A service descriptor should be
 * located at META-INF/services/org/mule/providers/<protocol> where protocol is the
 * protocol of the connector to be created The service descriptor is in the form of
 * string key value pairs and supports a number of properties, descriptions of which
 * can be found here: http://www.muledocs.org/Transport+Service+Descriptors.
 */
public interface TransportServiceDescriptor extends ServiceDescriptor
{
    public static final String OSGI_HEADER_TRANSPORT = "Mule-Transport";
        
    public MessageAdapter createMessageAdapter(Object message) throws TransportServiceException;

    public MessageAdapter createMessageAdapter(Object message, MessageAdapter originalMessageAdapter)
        throws TransportServiceException;

    public SessionHandler createSessionHandler() throws TransportServiceException;

    public MessageReceiver createMessageReceiver(Connector connector,
                                                             Service service,
                                                             InboundEndpoint endpoint) throws MuleException;

    public MessageReceiver createMessageReceiver(Connector connector,
                                                             Service service,
                                                             InboundEndpoint endpoint,
                                                             Object[] args) throws MuleException;

    public MessageDispatcherFactory createDispatcherFactory() throws TransportServiceException;

    public MessageRequesterFactory createRequesterFactory() throws TransportServiceException;

    public TransactionFactory createTransactionFactory() throws TransportServiceException;

    public Connector createConnector() throws TransportServiceException;

    public List createInboundTransformers() throws TransportFactoryException;

    public List createOutboundTransformers() throws TransportFactoryException;

    public List createResponseTransformers() throws TransportFactoryException;

    public EndpointURIBuilder createEndpointBuilder() throws TransportFactoryException;

    public void setExceptionMappings(Properties props);

    public Properties getExceptionMappings();
}
