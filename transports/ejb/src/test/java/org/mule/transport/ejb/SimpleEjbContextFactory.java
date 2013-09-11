/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ejb;

import org.mule.api.config.PropertyFactory;
import org.mule.jndi.MuleInitialContextFactory;

import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SimpleEjbContextFactory implements PropertyFactory
{
    protected final Log logger = LogFactory.getLog(getClass());

    public Object create(Map<?, ?> properties) throws Exception
    {
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, MuleInitialContextFactory.class.getName());
        
        InitialContext context = new InitialContext(env);
        for (Map.Entry<?, ?> entry : properties.entrySet())
        {
            Object key = entry.getKey();
            if (key instanceof String)
            {
                Object value = entry.getValue();
                logger.debug("Binding " + key + " to " + value);
                context.bind((String) key, value);
            }
        }
        
        return context;
    }

}
