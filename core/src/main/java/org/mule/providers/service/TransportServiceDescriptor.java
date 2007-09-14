/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.service;

import org.mule.impl.endpoint.EndpointURIBuilder;
import org.mule.registry.ServiceDescriptor;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOTransactionFactory;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.provider.UMOMessageDispatcherFactory;
import org.mule.umo.provider.UMOMessageReceiver;
import org.mule.umo.provider.UMOSessionHandler;
import org.mule.umo.provider.UMOStreamMessageAdapter;
import org.mule.umo.transformer.UMOTransformer;

import java.io.InputStream;
import java.io.OutputStream;
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
    public UMOMessageAdapter createMessageAdapter(Object message) throws TransportServiceException;

    public UMOStreamMessageAdapter createStreamMessageAdapter(InputStream in, OutputStream out) throws TransportServiceException;

    public UMOSessionHandler createSessionHandler() throws TransportServiceException;

    public UMOMessageReceiver createMessageReceiver(UMOConnector connector,
                                                             UMOComponent component,
                                                             UMOImmutableEndpoint endpoint) throws UMOException;

    public UMOMessageReceiver createMessageReceiver(UMOConnector connector,
                                                             UMOComponent component,
                                                             UMOImmutableEndpoint endpoint,
                                                             Object[] args) throws UMOException;

    public UMOMessageDispatcherFactory createDispatcherFactory() throws TransportServiceException;

    public UMOTransactionFactory createTransactionFactory() throws TransportServiceException;

    public UMOConnector createConnector() throws TransportServiceException;

    public List createInboundTransformers() throws TransportFactoryException;

    public List createOutboundTransformers() throws TransportFactoryException;

    public List createResponseTransformers() throws TransportFactoryException;

    public EndpointURIBuilder createEndpointBuilder() throws TransportFactoryException;

    public void setExceptionMappings(Properties props);

    public Properties getExceptionMappings();

}
