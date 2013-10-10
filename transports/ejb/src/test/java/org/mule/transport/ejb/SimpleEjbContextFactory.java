/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
