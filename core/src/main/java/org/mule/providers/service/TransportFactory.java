/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.service;

import org.mule.RegistryContext;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.providers.AbstractConnector;
import org.mule.registry.ServiceDescriptorFactory;
import org.mule.registry.ServiceException;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.BeanUtils;
import org.mule.util.MuleObjectHelper;
import org.mule.util.ObjectNameHelper;
import org.mule.util.PropertiesUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>TransportFactory</code> can be used for generically creating endpoints
 * from an url. Note that for some endpoints, the url alone is not enough to create
 * the endpoint if a connector for the endpoint has not already been configured with
 * the Mule Manager.
 * 
 */
public class TransportFactory
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(TransportFactory.class);

    public static final int GET_OR_CREATE_CONNECTOR = 0;
    public static final int ALWAYS_CREATE_CONNECTOR = 1;
    public static final int NEVER_CREATE_CONNECTOR = 2;
    public static final int USE_CONNECTOR = 3;

    public static UMOEndpoint createEndpoint(UMOEndpointURI uri, String type) throws EndpointException
    {
        String scheme = uri.getFullScheme();
        UMOConnector connector = null;
        try
        {
            if (uri.getCreateConnector() == ALWAYS_CREATE_CONNECTOR)
            {
                connector = createConnector(uri);
                RegistryContext.getRegistry().registerConnector(connector);
            }
            else if (uri.getCreateConnector() == NEVER_CREATE_CONNECTOR)
            {
                connector = getConnectorByProtocol(scheme);
            }
            else if (uri.getConnectorName() != null)
            {
                connector = RegistryContext.getRegistry().lookupConnector(uri.getConnectorName());
                if (connector == null)
                {
                    throw new TransportFactoryException(new Message(Messages.X_NOT_REGISTERED_WITH_MANAGER,
                        "Connector: " + uri.getConnectorName()));
                }
            }
            else
            {
                connector = getConnectorByProtocol(scheme);
                if (connector == null)
                {
                    connector = createConnector(uri);
                    RegistryContext.getRegistry().registerConnector(connector);
                }
            }
        }
        catch (Exception e)
        {
            throw new TransportFactoryException(e);
        }

        if (connector == null)
        {
            Message m = new Message(Messages.FAILED_TO_CREATE_X_WITH_X, "Endpoint", "Uri: " + uri);
            m.setNextMessage(new Message(Messages.X_IS_NULL, "connector"));
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
                }
                else 
                {
                    throw new ServiceException(Message.createStaticMessage("No service descriptor found for transport: " + scheme + ".  This transport does not appear to be installed."));
                }
            }
            return trans;
        }
        catch (Exception e)
        {
            throw new TransportFactoryException(e);
        }
    }

    /**
     * Creates an uninitialied connector from the provided MuleEndpointURI. The
     * scheme is used to determine what kind of connector to create. Any params set
     * on the uri can be used to initialise bean properties on the created connector.
     * <p/> Note that the initalise method will need to be called on the connector
     * returned. This is so that developers can control when the connector
     * initialisation takes place as this is likely to initialse all connecotr
     * resources.
     * 
     * @param url the MuleEndpointURI url to create the connector with
     * @return a new Connector
     * @throws TransportFactoryException
     */
    public static UMOConnector createConnector(UMOEndpointURI url) throws TransportFactoryException
    {
        try
        {
            UMOConnector connector;
            String scheme = url.getSchemeMetaInfo();
    
            TransportServiceDescriptor sd = (TransportServiceDescriptor) 
                RegistryContext.getRegistry().lookupServiceDescriptor(ServiceDescriptorFactory.PROVIDER_SERVICE_TYPE, scheme, null);
            if (sd == null)
            {
                throw new ServiceException(Message.createStaticMessage("No service descriptor found for transport: " + scheme + ".  This transport does not appear to be installed."));
            }
            
            connector = sd.createConnector();
            if (connector != null)
            {
                if (connector instanceof AbstractConnector)
                {
                    ((AbstractConnector)connector).initialiseFromUrl(url);
                }
            }
            else
            {
                throw new TransportFactoryException(new Message(Messages.X_NOT_SET_IN_SERVICE_X,
                    "Connector", scheme));
            }
            connector.setName(ObjectNameHelper.getConnectorName(connector));

            // set any manager default properties for the connector
            // these are set on the Manager with a protocol i.e.
            // jms.specification=1.1
            Map props = new HashMap();
            PropertiesUtils.getPropertiesWithPrefix(RegistryContext.getRegistry().getProperties(),
                connector.getProtocol().toLowerCase(), props);
            if (props.size() > 0)
            {
                props = PropertiesUtils.removeNamespaces(props);
                BeanUtils.populateWithoutFail(connector, props, true);
            }

            return connector;
        }
        catch (Exception e)
        {
            throw new TransportFactoryException(new Message(Messages.FAILED_TO_CREATE_X_WITH_X, "Endpoint",
                url), e);
        }
    }

    public static UMOConnector getOrCreateConnectorByProtocol(UMOEndpointURI uri)
        throws TransportFactoryException
    {
        return getOrCreateConnectorByProtocol(uri, uri.getCreateConnector());
    }

    public static UMOConnector getOrCreateConnectorByProtocol(UMOImmutableEndpoint endpoint)
        throws TransportFactoryException
    {
        return getOrCreateConnectorByProtocol(endpoint.getEndpointURI(), endpoint.getCreateConnector());
    }

    private static UMOConnector getOrCreateConnectorByProtocol(UMOEndpointURI uri, int create)
        throws TransportFactoryException
    {
        UMOConnector connector = getConnectorByProtocol(uri.getFullScheme());
        if (ALWAYS_CREATE_CONNECTOR == create
            || (connector == null && create == GET_OR_CREATE_CONNECTOR))
        {
            connector = createConnector(uri);
            try
            {
                BeanUtils.populate(connector, uri.getParams());
                RegistryContext.getRegistry().registerConnector(connector);

            }
            catch (Exception e)
            {
                throw new TransportFactoryException(new Message(Messages.FAILED_TO_SET_PROPERTIES_ON_X,
                    "Connector"), e);
            }
        }
        else if (create == NEVER_CREATE_CONNECTOR && connector == null)
        {
            logger.warn("There is no connector for protocol: " + uri.getScheme()
                        + " and 'createConnector' is set to NEVER.  Returning null");
        }
        return connector;
    }

    public static UMOConnector getConnectorByProtocol(String protocol)
    {
        UMOConnector connector;
        Map connectors = RegistryContext.getRegistry().getConnectors();
        for (Iterator iterator = connectors.values().iterator(); iterator.hasNext();)
        {
            connector = (UMOConnector)iterator.next();
            if (connector.supportsProtocol(protocol))
            {
                return connector;
            }
        }
        return null;
    }
}
