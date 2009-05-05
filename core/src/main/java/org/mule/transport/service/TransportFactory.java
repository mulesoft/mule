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

import org.mule.RegistryContext;
import org.mule.api.MuleContext;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.registry.ServiceDescriptorFactory;
import org.mule.api.registry.ServiceException;
import org.mule.api.transport.Connector;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.AbstractConnector;
import org.mule.util.BeanUtils;
import org.mule.util.ObjectNameHelper;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

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
    public static Connector createConnector(EndpointURI url, MuleContext muleContext) throws TransportFactoryException
    {

        try
        {
            Connector connector;
            String scheme = url.getSchemeMetaInfo();

            TransportServiceDescriptor sd = (TransportServiceDescriptor)
                RegistryContext.getRegistry().lookupServiceDescriptor(ServiceDescriptorFactory.TRANSPORT_SERVICE_TYPE, scheme, null);
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

            return connector;
        }
        catch (Exception e)
        {
            throw new TransportFactoryException(
                CoreMessages.failedToCreateObjectWith("Endpoint", url), e);
        }
    }

    public static Connector getOrCreateConnectorByProtocol(ImmutableEndpoint endpoint, MuleContext muleContext)
        throws TransportFactoryException
    {
        return getOrCreateConnectorByProtocol(endpoint.getEndpointURI(), muleContext);
    }

    /**
     * Returns an initialized connector.
     */
    public static Connector getOrCreateConnectorByProtocol(EndpointURI uri, MuleContext muleContext)
        throws TransportFactoryException
    {
        String connectorName = uri.getConnectorName();
        if (null != connectorName)
        {
            // TODO this lookup fails currently on Mule 2.x! MuleAdminAgentTestCase
            Connector connector = RegistryContext.getRegistry().lookupConnector(connectorName);
            if (connector != null)
            {
                return connector;
            }
        }

        Connector connector = getConnectorByProtocol(uri.getFullScheme());
        if (connector == null)
        {
            connector = createConnector(uri, muleContext);
            try
            {
                BeanUtils.populate(connector, uri.getParams());
                connector.setMuleContext(muleContext);
                muleContext.getRegistry().registerConnector(connector);
            }
            catch (Exception e)
            {
                throw new TransportFactoryException(e);
            }
        }
        return connector;
    }

    public static Connector getConnectorByProtocol(String protocol)
    {
        Connector connector;
        List<Connector> results = new ArrayList<Connector>();
        Collection connectors = RegistryContext.getRegistry().lookupObjects(Connector.class);
        for (Iterator iterator = connectors.iterator(); iterator.hasNext();)
        {
            connector = (Connector)iterator.next();
            if (connector.supportsProtocol(protocol))
            {
                results.add(connector);
            }
        }
        if(results.size() > 1)
        {
            StringBuffer buf = new StringBuffer();
            for (Connector result : results)
            {
                buf.append(result.getName()).append(", ");
            }
            throw new IllegalStateException(
                        CoreMessages.moreThanOneConnectorWithProtocol(protocol, buf.toString()).getMessage());
        }
        else if(results.size() == 1)
        {
            return results.get(0);
        }
        else
        {
            return null;
        }
    }
}
