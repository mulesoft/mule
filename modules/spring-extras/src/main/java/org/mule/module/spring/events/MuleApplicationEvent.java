/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.events;

import org.mule.api.MuleEventContext;
import org.mule.api.endpoint.MalformedEndpointException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

/**
 * <code>MuleApplicationEvent</code> is an Spring ApplicationEvent used to wrap a
 * DefaultMuleEvent
 *
 * <b>Deprecated from 3.6.0.</b>
 */
@Deprecated
public class MuleApplicationEvent extends ApplicationEvent
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 5297176859050194632L;

    private final MuleEventContext context;
    private final String endpoint;
    private final ApplicationContext applicationContext;
    private final Map properties = Collections.synchronizedMap(new HashMap());

    public MuleApplicationEvent(Object message, String endpoint)
    {
        super(message);
        this.endpoint = endpoint;
        this.applicationContext = null;
        this.context = null;
    }

    MuleApplicationEvent(Object message, MuleEventContext context, ApplicationContext appContext)
        throws MalformedEndpointException
    {
        super(message);
        this.context = context;
        this.endpoint = context.getEndpointURI().toString();
        this.applicationContext = appContext;
    }

    public MuleEventContext getMuleEventContext()
    {
        return context;
    }

    public String getEndpoint()
    {
        return endpoint;
    }

    public ApplicationContext getApplicationContext()
    {
        return applicationContext;
    }

    public Map getProperties()
    {
        return properties;
    }

    public void setProperty(Object key, Object value)
    {
        this.properties.put(key, value);
    }

    public Object getProperty(Object key)
    {
        return properties.get(key);
    }

}
