/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.spring.events;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.mule.umo.UMOEventContext;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

/**
 * <code>MuleApplicationEvent</code> is an Spring ApplicationEvent used to wrap a
 * MuleEvent
 */

public class MuleApplicationEvent extends ApplicationEvent
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 5297176859050194632L;

    private final UMOEventContext context;
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

    MuleApplicationEvent(Object message, UMOEventContext context, ApplicationContext appContext)
        throws MalformedEndpointException
    {
        super(message);
        this.context = context;
        this.endpoint = context.getEndpointURI().toString();
        this.applicationContext = appContext;
    }

    public UMOEventContext getMuleEventContext()
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
