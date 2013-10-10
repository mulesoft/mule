/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.service;

import org.mule.api.registry.ServiceException;
import org.mule.api.registry.ServiceType;
import org.mule.api.transport.Connector;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.AbstractConnector;
import org.mule.util.PropertiesUtils;
import org.mule.util.SpiUtils;

import java.util.Properties;

/**
 * Used to describe a Meta transport, one which only defines an endpoint, not a connector, receiver, dispatcher, etc
 *
 * @since 3.0.0
 */
public class MetaTransportServiceDescriptor extends DefaultTransportServiceDescriptor
{
    private String metaScheme;

    public MetaTransportServiceDescriptor(String metaScheme, String scheme, Properties props, ClassLoader classLoader) throws ServiceException
    {
        super(metaScheme, props, classLoader);
        this.metaScheme = metaScheme;
        Properties p = SpiUtils.findServiceDescriptor(ServiceType.TRANSPORT, scheme);
        //Load any overrides for the for the endpoint scheme
        if (p == null)
        {
            throw new ServiceException(CoreMessages.failedToCreate("transport: " + metaScheme + ":" + scheme));
        }
        Properties temp = new Properties();
        PropertiesUtils.getPropertiesWithPrefix(props, scheme + ".", temp);
        if (temp.size() > 0)
        {
            p.putAll(PropertiesUtils.removeNamespaces(temp));
        }
        setOverrides(p);
    }

    /**
     * Override the connector cration and register our Meta scheme with the connecotr so that the connector can
     * be used when creating endpoints using this meta transport
     *
     * @return a transport connector matching the scheme of the descriptor with the meta scheme registered with the
     *         connector
     * @throws TransportServiceException if the connector cannot be created
     */
    @Override
    public Connector createConnector() throws TransportServiceException
    {
        AbstractConnector c = (AbstractConnector) super.createConnector();
        c.registerSupportedMetaProtocol(metaScheme);
        return c;
    }
}
