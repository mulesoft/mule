/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.endpoint;

import org.mule.RegistryContext;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.providers.AbstractConnector;
import org.mule.providers.service.TransportFactory;
import org.mule.providers.service.TransportFactoryException;
import org.mule.providers.service.TransportServiceDescriptor;
import org.mule.registry.ServiceDescriptorFactory;
import org.mule.registry.ServiceException;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointFactory;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.MuleObjectHelper;
import org.mule.util.ObjectNameHelper;

import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EndpointFactory implements UMOEndpointFactory
{
    
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(EndpointFactory.class);

    public static final int GET_OR_CREATE_CONNECTOR = 0;
    public static final int ALWAYS_CREATE_CONNECTOR = 1;
    public static final int NEVER_CREATE_CONNECTOR = 2;
    public static final int USE_CONNECTOR = 3;

    public UMOImmutableEndpoint createInboundEndpoint(String uri, UMOManagementContext managementContext) throws UMOException
    {
        UMOImmutableEndpoint globalEndpoint = lookupEndpoint(uri);
        UMOImmutableEndpoint endpoint = null;
        if (globalEndpoint != null)
        {
            endpoint = new InboundEndpoint(globalEndpoint);

        }
        else
        {
            endpoint = createEndpoint(new MuleEndpointURI(uri), UMOImmutableEndpoint.ENDPOINT_TYPE_RECEIVER, managementContext);
        }
        return endpoint;
    }

    public UMOImmutableEndpoint createOutboundEndpoint(String uri, UMOManagementContext managementContext) throws UMOException
    {
        UMOImmutableEndpoint globalEndpoint = lookupEndpoint(uri);
        UMOImmutableEndpoint endpoint = null;
        if (globalEndpoint != null)
        {
            endpoint = new OutboundEndpoint(globalEndpoint);

        }
        else
        {
            endpoint = createEndpoint(new MuleEndpointURI(uri), UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER, managementContext);
        }
        return endpoint;
    }

    public UMOImmutableEndpoint createResponseEndpoint(String uri, UMOManagementContext managementContext) throws UMOException
    {
        UMOImmutableEndpoint globalEndpoint = lookupEndpoint(uri);
        UMOImmutableEndpoint endpoint = null;
        if (globalEndpoint != null)
        {
            endpoint = new ResponseEndpoint(globalEndpoint);

        }
        else
        {
            endpoint = createEndpoint(new MuleEndpointURI(uri), UMOImmutableEndpoint.ENDPOINT_TYPE_RESPONSE, managementContext);
        }
        return endpoint;
    }

    protected UMOImmutableEndpoint lookupEndpoint(String poiendpointNamentName)
    {
        return RegistryContext.getRegistry().lookupEndpoint(poiendpointNamentName);
    }
    
    /**
     * @deprecated
     */
    public UMOImmutableEndpoint createEndpoint(UMOEndpointURI uri, String type, UMOManagementContext managementContext) throws UMOException
    {
        UMOImmutableEndpoint endpoint = lookupEndpoint(uri.getEndpointName());
        if (endpoint == null)
        {
            uri.initialise();
            endpoint = createEndpointInternal(uri, type, managementContext);
            return endpoint;
        }
        return endpoint;
    }

    
    private UMOEndpoint createEndpointInternal(UMOEndpointURI uri, String type, UMOManagementContext managementContext) throws EndpointException
    {
        String scheme = uri.getFullScheme();
        UMOConnector connector;
        try
        {
            if (uri.getCreateConnector() == ALWAYS_CREATE_CONNECTOR)
            {
                connector = TransportFactory.createConnector(uri, managementContext);
            }
            else if (uri.getCreateConnector() == NEVER_CREATE_CONNECTOR)
            {
                connector = TransportFactory.getConnectorByProtocol(scheme);
            }
            else if (uri.getConnectorName() != null)
            {
                connector = RegistryContext.getRegistry().lookupConnector(uri.getConnectorName());
                if (connector == null)
                {
                    throw new TransportFactoryException(
                        CoreMessages.objectNotRegistered("Connector", uri.getConnectorName()));
                }
            }
            else
            {
                connector = TransportFactory.getConnectorByProtocol(scheme);
                if (connector == null)
                {
                    connector = TransportFactory.createConnector(uri, managementContext);
                }
            }
        }
        catch (Exception e)
        {
            throw new TransportFactoryException(e);
        }

        if (connector == null)
        {
            Message m = CoreMessages.failedToCreateObjectWith("Endpoint", "Uri: " + uri);
            m.setNextMessage(CoreMessages.objectIsNull("connector"));
            throw new TransportFactoryException(m);

        }

        UMOEndpoint endpoint = new MuleEndpoint();
        endpoint.setConnector(connector);
        endpoint.setEndpointURI(uri);
        if (uri.getEndpointName() != null)
        {
            endpoint.setName(uri.getEndpointName());
        }
        String name = ObjectNameHelper.getEndpointName(endpoint);
        endpoint.setName(name);

        if (type != null)
        {
            endpoint.setType(type);
            UMOTransformer trans = getTransformer(uri, connector,
                (UMOEndpoint.ENDPOINT_TYPE_RECEIVER.equals(type) ? 0 : 1));
            endpoint.setTransformer(trans);
            if (UMOEndpoint.ENDPOINT_TYPE_RECEIVER.equals(type))
            {
                // set the response transformer
                trans = getTransformer(uri, connector, 2);
                endpoint.setResponseTransformer(trans);
            }
        }
        try
        {
            RegistryContext.getRegistry().registerEndpoint(endpoint);

        }
        catch (UMOException e)
        {
            throw new TransportFactoryException(e);
        }
        return endpoint;
    }
    
    /**
     * @param url
     * @param cnn
     * @param type 0=inbound, 1=outbound, 2=response
     * @return
     * @throws TransportFactoryException
     */
    private static UMOTransformer getTransformer(UMOEndpointURI url, UMOConnector cnn, int type)
        throws TransportFactoryException
    {
        try
        {
            UMOTransformer trans = null;
            String transId = null;
            if (type == 2)
            {
                transId = url.getResponseTransformers();
            }
            else
            {
                transId = url.getTransformers();
            }

            if (transId != null)
            {
                trans = MuleObjectHelper.getTransformer(transId, ",");
            }
            else
            {
                // Get connector specific overrides to set on the descriptor
                Properties overrides = new Properties();
                if (cnn instanceof AbstractConnector)
                {
                    Map so = ((AbstractConnector)cnn).getServiceOverrides();
                    if (so != null)
                    {
                        overrides.putAll(so);
                    }
                }

                String scheme = url.getSchemeMetaInfo();

                TransportServiceDescriptor sd = (TransportServiceDescriptor)
                    RegistryContext.getRegistry().lookupServiceDescriptor(ServiceDescriptorFactory.PROVIDER_SERVICE_TYPE, scheme, overrides);
                if (sd != null)
                {
                    if (type == 0)
                    {
                        trans = sd.createInboundTransformer();
                    }
                    else if (type == 1)
                    {
                        trans = sd.createOutboundTransformer();
                    }
                    else
                    {
                        trans = sd.createResponseTransformer();
                    }
                    
                    if (trans != null)
                    {
                        trans.initialise();
                    }
                }
                else
                {
                    throw new ServiceException(CoreMessages.noServiceTransportDescriptor(scheme));
                }
            }
            return trans;
        }
        catch (Exception e)
        {
            throw new TransportFactoryException(e);
        }
    }

}
