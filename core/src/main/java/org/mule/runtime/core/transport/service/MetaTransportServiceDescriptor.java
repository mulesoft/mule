/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transport.service;

import org.mule.runtime.core.api.registry.ServiceException;
import org.mule.runtime.core.api.registry.ServiceType;
import org.mule.runtime.core.api.transport.Connector;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.transport.AbstractConnector;
import org.mule.runtime.core.util.PropertiesUtils;
import org.mule.runtime.core.util.SpiUtils;

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
