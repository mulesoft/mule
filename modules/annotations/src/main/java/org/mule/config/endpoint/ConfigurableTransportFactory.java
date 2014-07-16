/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.endpoint;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.transport.Connector;
import org.mule.transport.service.TransportFactory;
import org.mule.transport.service.TransportFactoryException;
import org.mule.util.BeanUtils;
import org.mule.util.ClassUtils;
import org.mule.util.PropertiesUtils;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

/**
 * TODO
 */
public class ConfigurableTransportFactory extends TransportFactory
{
    public static final String CHANNEL_OVERRIDES = "_configuredConnectorOverrides";
    public static final String CHANNEL_OVERRIDES_FILE = "META-INF/services/org/mule/config/channel-overrides.properties";
    public static final String SINGLETON_PROPERTY = "singleton";
    public static final String TRUE = "TRUE";

    private Properties overrides;

    public ConfigurableTransportFactory(MuleContext muleContext) throws MuleException
    {
        super(muleContext);
        overrides = (Properties) muleContext.getRegistry().lookupObject(CHANNEL_OVERRIDES);
        if (overrides == null)
        {
            overrides = loadOverrides();
            muleContext.getRegistry().registerObject(CHANNEL_OVERRIDES, overrides);
        }
    }

    @Override
    public Connector createConnector(EndpointURI endpointURI) throws TransportFactoryException
    {
        Connector c;
        String scheme = endpointURI.getScheme();
        Map temp = new Properties();
        PropertiesUtils.getPropertiesWithPrefix(overrides, scheme, temp);
        temp = PropertiesUtils.removeNamespaces(temp);
        String singleton = (String) temp.remove(SINGLETON_PROPERTY);
        if (TRUE.equalsIgnoreCase(singleton))
        {
            c = this.getConnectorByProtocol(scheme);
            if (c != null)
            {
                return c;
            }
        }
        c = super.createConnector(endpointURI);
        BeanUtils.populateWithoutFail(c, temp, true);
        return c;
    }

    protected Properties loadOverrides() throws MuleException
    {
        Properties props = new Properties();
        Enumeration e = ClassUtils.getResources(CHANNEL_OVERRIDES_FILE, getClass());
        while (e.hasMoreElements())
        {
            URL url = (URL) e.nextElement();
            try
            {
                props.load(url.openStream());
            }
            catch (IOException e1)
            {
                throw new DefaultMuleException("failed to read channel overrides from URL: " + url.toExternalForm());
            }
        }
        return props;
    }
}
