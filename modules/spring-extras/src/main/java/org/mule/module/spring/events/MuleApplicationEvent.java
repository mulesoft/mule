/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
 */

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
