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

import org.mule.RegistryContext;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.providers.AbstractConnector;
import org.mule.registry.ServiceDescriptorFactory;
import org.mule.registry.ServiceException;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.BeanUtils;
import org.mule.util.MuleObjectHelper;
import org.mule.util.ObjectNameHelper;

import java.util.Collection;
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

    public static UMOEndpoint createEndpoint(UMOEndpointURI uri, String type, UMOManagementContext managementContext) throws EndpointException
    {
        String scheme = uri.getFullScheme();
        UMOConnector connector;
        try
        {
            if (uri.getCreateConnector() == ALWAYS_CREATE_CONNECTOR)
            {
                connector = createConnector(uri, managementContext);
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
                    throw new TransportFactoryException(
                        CoreMessages.objectNotRegistered("Connector", uri.getConnectorName()));
                }
            }
            else
            {
                connector = getConnectorByProtocol(scheme);
                if (connector == null)
                {
                    connector = createConnector(uri, managementContext);
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
    public static UMOConnector createConnector(UMOEndpointURI url, UMOManagementContext managementContext) throws TransportFactoryException
    {

        try
        {
            UMOConnector connector;
            String scheme = url.getSchemeMetaInfo();

            TransportServiceDescriptor sd = (TransportServiceDescriptor)
                RegistryContext.getRegistry().lookupServiceDescriptor(ServiceDescriptorFactory.PROVIDER_SERVICE_TYPE, scheme, null);
            if (sd == null)
            {
                throw new ServiceException(CoreMessages.noServiceTransportDescriptor(scheme));
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
                throw new TransportFactoryException(
                    CoreMessages.objectNotSetInService("Connector", scheme));
            }

            connector.setName(ObjectNameHelper.getConnectorName(connector));

            // TODO Do we still need to support this for 2.x?
            // set any manager default properties for the connector
            // these are set on the Manager with a protocol i.e.
            // jms.specification=1.1
//            Map props = new HashMap();
//            PropertiesUtils.getPropertiesWithPrefix(RegistryContext.getRegistry().lookupProperties(),
//                connector.getProtocol().toLowerCase(), props);
//            if (props.size() > 0)
//            {
//                props = PropertiesUtils.removeNamespaces(props);
//                BeanUtils.populateWithoutFail(connector, props, true);
//            }
            RegistryContext.getRegistry().registerConnector(connector, managementContext);

            return connector;
        }
        catch (Exception e)
        {
            throw new TransportFactoryException(
                CoreMessages.failedToCreateObjectWith("Endpoint", url), e);
        }
    }

    public static UMOConnector getOrCreateConnectorByProtocol(UMOEndpointURI uri, UMOManagementContext managementContext)
        throws TransportFactoryException
    {
        return getOrCreateConnectorByProtocol(uri, uri.getCreateConnector(), managementContext);
    }

    public static UMOConnector getOrCreateConnectorByProtocol(UMOImmutableEndpoint endpoint, UMOManagementContext managementContext)
        throws TransportFactoryException
    {
        return getOrCreateConnectorByProtocol(endpoint.getEndpointURI(), endpoint.getCreateConnector(), managementContext);
    }

    private static UMOConnector getOrCreateConnectorByProtocol(UMOEndpointURI uri, int create, UMOManagementContext managementContext)
        throws TransportFactoryException
    {
        String connectorName = uri.getConnectorName();
        if (null != connectorName)
        {
            // TODO this lookup fails currently on Mule 2.x! MuleAdminAgentTestCase
            UMOConnector connector = RegistryContext.getRegistry().lookupConnector(connectorName);
            if (connector != null)
            {
                return connector;
            }
        }

        UMOConnector connector = getConnectorByProtocol(uri.getFullScheme());
        if (ALWAYS_CREATE_CONNECTOR == create
            || (connector == null && create == GET_OR_CREATE_CONNECTOR))
        {
            connector = createConnector(uri, managementContext);
            try
            {
                BeanUtils.populate(connector, uri.getParams());

            }
            catch (Exception e)
            {
                throw new TransportFactoryException(
                    CoreMessages.failedToSetPropertiesOn("Connector"), e);
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
        UMOConnector resultConnector = null;
        Collection connectors = RegistryContext.getRegistry().getConnectors();
        for (Iterator iterator = connectors.iterator(); iterator.hasNext();)
        {
            connector = (UMOConnector)iterator.next();
            if (connector.supportsProtocol(protocol))
            {
                if(resultConnector==null)
                {
                    resultConnector = connector;
                }
                else
                {
                    throw new IllegalStateException(
                        CoreMessages.moreThanOneConnectorWithProtocol(protocol).getMessage());
                }
            }
        }
        return resultConnector;
    }
}
