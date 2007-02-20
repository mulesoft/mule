/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.RegistryContext;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * TODO
 */
public class EndpointFactoryBean extends MuleEndpoint implements FactoryBean, ApplicationContextAware
{


    private String address;
    private String encoding;

    private ApplicationContext context;


    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        applicationContext = context;
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }


    public String getEncoding()
    {
        return encoding;
    }

    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }

    public Object getObject() throws Exception
    {
        String key = address;
        if(key==null)
        {
            key = name;
        }
        UMOEndpoint ep = null;

        ep = RegistryContext.getRegistry().lookupEndpoint(name);
        if(ep==null)
        {
            ep = new MuleEndpoint(name, new MuleEndpointURI(address), connector, transformer, type, createConnector, encoding, properties);
        }
        else
        {
            ep = new MuleEndpoint(ep);
        }
        if(responseTransformer!=null) ep.setResponseTransformer(responseTransformer);
        if(filter!=null) ep.setFilter(filter);
        if(securityFilter!=null) ep.setSecurityFilter(securityFilter);
        if(transactionConfig!=null) ep.setTransactionConfig(transactionConfig);

        return ep;
    }

    public boolean isSingleton()
    {
        return false;
    }

    public Class getObjectType()
    {
        return UMOImmutableEndpoint.class;
    }
}
