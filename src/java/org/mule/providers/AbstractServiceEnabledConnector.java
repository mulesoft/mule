/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers;

import org.mule.InitialisationException;
import org.mule.providers.service.ConnectorFactory;
import org.mule.providers.service.ConnectorServiceDescriptor;
import org.mule.providers.service.ConnectorServiceException;
import org.mule.umo.MessageException;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.provider.UMOMessageReceiver;
import org.mule.util.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Properties;

/**
 * <code>AbstractServiceEnabledConnector</code> initialises a connector from a sercive
 * descriptor.  using this method greatly reduces the code required to implement a connector and
 * means that Mule can create connectors and endpoints from a url if the connector has a service descriptor.
 * @see ConnectorServiceDescriptor
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public abstract class AbstractServiceEnabledConnector extends AbstractConnector
{
    /**
     * Holds the service configuration for this connector
     */
    protected ConnectorServiceDescriptor serviceDescriptor;

    protected Properties serviceOverrides;

    public void doInitialise() throws InitialisationException
    {
        initFromServiceDescriptor();
    }

    public void initialiseFromUrl(UMOEndpointURI endpointUri) throws InitialisationException
    {
        if(!getProtocol().equalsIgnoreCase(endpointUri.getScheme()) && !getProtocol().equalsIgnoreCase(endpointUri.getSchemeMetaInfo())) {
            throw new InitialisationException("The endpointUri scheme is not compatible with this connector: " + getProtocol() + ", " + endpointUri);
        }
        Properties props = new Properties();
        props.putAll(endpointUri.getParams());
        //auto set username and password
        if(endpointUri.getUserInfo()!=null) {
            int i = endpointUri.getUserInfo().indexOf(":");
            if(i == -1) {
                props.setProperty("username", endpointUri.getUserInfo());
            } else {
                props.setProperty("username", endpointUri.getUserInfo().substring(0, i));
                props.setProperty("password", endpointUri.getUserInfo().substring(i + 1));
            }
        }
        if(endpointUri.getHost()!=null) {
            props.setProperty("hostname", endpointUri.getHost());
            props.setProperty("host", endpointUri.getHost());
        }
        if(endpointUri.getPort() > -1) props.setProperty("port", String.valueOf(endpointUri.getPort()));
        try
        {
            BeanUtils.populateWithoutFail(this, props, true);
        } catch (InvocationTargetException e)
        {
            throw new InitialisationException(e.getMessage(), e);
        }
    }

    protected synchronized void initFromServiceDescriptor() throws InitialisationException
    {
        try
        {
            serviceDescriptor = ConnectorFactory.getServiceDescriptor(getProtocol().toLowerCase(), serviceOverrides);

            if (serviceDescriptor.getDispatcherFactory() != null)
            {
                logger.info("Loading DispatcherFactory: " + serviceDescriptor.getDispatcherFactory());
                dispatcherFactory = serviceDescriptor.createDispatcherFactory();
            }

            defaultInboundTransformer = serviceDescriptor.createInboundTransformer();
            defaultOutboundTransformer = serviceDescriptor.createOutboundTransformer();
            defaultResponseTransformer = serviceDescriptor.createResponseTransformer();
        } catch (Exception e)
        {
            throw new InitialisationException("Failed to initialise connector from endpoint service descriptor: " + e.getMessage(), e);
        }
    }

    protected ConnectorServiceDescriptor getServiceDescriptor() {
        if(serviceDescriptor==null) {
            throw new IllegalStateException("This connector has not yet been initiaiised: " + name);
        }
        return serviceDescriptor;
    }

    public UMOMessageReceiver createReceiver(UMOComponent component, UMOEndpoint endpoint) throws Exception
    {
        return getServiceDescriptor().createMessageReceiver(this, component, endpoint);
    }

    /**
     * Gets a <code>UMOMessageAdapter</code> for the endpoint for the given message (data)
     *
     * @param message the data with which to initialise the <code>UMOMessageAdapter</code>
     * @return the <code>UMOMessageAdapter</code> for the endpoint
     * @throws org.mule.umo.MessageException if the message parameter is not supported
     * @see org.mule.umo.provider.UMOMessageAdapter
     */
    public UMOMessageAdapter getMessageAdapter(Object message) throws MessageException
    {
        try
        {
            return serviceDescriptor.createMessageAdapter(message);
        } catch (ConnectorServiceException e)
        {
            throw new MessageException("Failed to create message adapter from service: " + e.getMessage(), e);
        }
    }

    public Map getServiceOverrides()
    {
        return serviceOverrides;
    }

    public void setServiceOverrides(Map serviceOverrides)
    {
        this.serviceOverrides = new Properties();
        this.serviceOverrides.putAll(serviceOverrides);
    }
}
