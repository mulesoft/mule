/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.service;

import org.mule.api.MuleContext;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.registry.ServiceException;
import org.mule.api.registry.ServiceType;
import org.mule.api.transport.Connector;
import org.mule.config.i18n.CoreMessages;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.transport.AbstractConnector;
import org.mule.util.BeanUtils;
import org.mule.util.ObjectNameHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>TransportFactory</code> can be used for generically creating endpoints
 * from an url. Note that for some endpoints, the url alone is not enough to create
 * the endpoint if a connector for the endpoint has not already been configured with
 * the Mule Manager.
 */
public class TransportFactory
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(TransportFactory.class);

    protected MuleContext muleContext;

    public TransportFactory(MuleContext muleContext)
    {
        this.muleContext = muleContext;
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
    public Connector createConnector(EndpointURI url) throws TransportFactoryException
    {

        try
        {
            Connector connector;
            String scheme = url.getFullScheme();

            TransportServiceDescriptor sd = (TransportServiceDescriptor)
                    muleContext.getRegistry().lookupServiceDescriptor(ServiceType.TRANSPORT, scheme, null);
            if (sd == null)
            {
                throw new ServiceException(CoreMessages.noServiceTransportDescriptor(scheme));
            }

            connector = sd.createConnector();
            if (connector != null)
            {
                if (connector instanceof AbstractConnector)
                {
                    ((AbstractConnector) connector).initialiseFromUrl(url);
                }
            }
            else
            {
                throw new TransportFactoryException(
                        CoreMessages.objectNotSetInService("Connector", scheme));
            }

            connector.setName(new ObjectNameHelper(muleContext).getConnectorName(connector));

            return connector;
        }
        catch (Exception e)
        {
            throw new TransportFactoryException(
                    CoreMessages.failedToCreateObjectWith("Endpoint", url), e);
        }
    }

    public Connector createConnector(String uri) throws TransportFactoryException
    {
        try
        {
            return createConnector(new MuleEndpointURI(uri, muleContext));
        }
        catch (EndpointException e)
        {
            throw new TransportFactoryException(e);
        }
    }

    public Connector getOrCreateConnectorByProtocol(ImmutableEndpoint endpoint)
            throws TransportFactoryException
    {
        return getOrCreateConnectorByProtocol(endpoint.getEndpointURI());
    }

    /**
     * Returns an initialized connector.
     */
    public Connector getOrCreateConnectorByProtocol(EndpointURI uri)
            throws TransportFactoryException
    {
        String connectorName = uri.getConnectorName();
        if (null != connectorName)
        {
            // TODO this lookup fails currently on Mule 2.x! MuleAdminAgentTestCase
            Connector connector = muleContext.getRegistry().lookupConnector(connectorName);
            if (connector != null)
            {
                return connector;
            }
        }

        Connector connector = getConnectorByProtocol(uri.getFullScheme());
        if (connector == null)
        {
            connector = createConnector(uri);
            try
            {
                BeanUtils.populate(connector, uri.getParams());
                muleContext.getRegistry().registerConnector(connector);
            }
            catch (Exception e)
            {
                throw new TransportFactoryException(e);
            }
        }
        return connector;
    }

    public Connector getConnectorByProtocol(String protocol)
    {
        Connector connector;
        List<Connector> results = new ArrayList<Connector>();
        Collection connectors = muleContext.getRegistry().lookupObjects(Connector.class);
        for (Iterator iterator = connectors.iterator(); iterator.hasNext();)
        {
            connector = (Connector) iterator.next();
            if (connector.supportsProtocol(protocol))
            {
                results.add(connector);
            }
        }
        if (results.size() > 1)
        {
            StringBuilder buf = new StringBuilder();
            for (Connector result : results)
            {
                buf.append(result.getName()).append(", ");
            }
            throw new IllegalStateException(
                    CoreMessages.moreThanOneConnectorWithProtocol(protocol, buf.toString()).getMessage());
        }
        else if (results.size() == 1)
        {
            return results.get(0);
        }
        else
        {
            return null;
        }
    }

    public Connector getDefaultConnectorByProtocol(String protocol)
    {
        Connector connector;
        List<Connector> results = new ArrayList<Connector>();
        Collection connectors = muleContext.getRegistry().lookupObjects(Connector.class);
        for (Iterator iterator = connectors.iterator(); iterator.hasNext();)
        {
            connector = (Connector) iterator.next();
            if (connector.supportsProtocol(protocol)  && ObjectNameHelper.isDefaultAutoGeneratedConnector(connector))
            {
                results.add(connector);
            }
        }
        if (results.size() > 1)
        {
            StringBuilder buf = new StringBuilder();
            for (Connector result : results)
            {
                buf.append(result.getName()).append(", ");
            }
            throw new IllegalStateException(
                    CoreMessages.moreThanOneConnectorWithProtocol(protocol, buf.toString()).getMessage());
        }
        else if (results.size() == 1)
        {
            return results.get(0);
        }
        else
        {
            return null;
        }
    }
}
