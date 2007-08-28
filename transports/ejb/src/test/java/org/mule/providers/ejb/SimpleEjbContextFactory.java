/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ejb;

import org.mule.config.PropertyFactory;
import org.mule.impl.jndi.MuleInitialContextFactory;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SimpleEjbContextFactory implements PropertyFactory
{
    protected final Log logger = LogFactory.getLog(getClass());

    public Object create(Map properties) throws Exception
    {
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, MuleInitialContextFactory.class.getName());
        InitialContext context = new InitialContext(env);
        Iterator keys = properties.keySet().iterator();
        while (keys.hasNext())
        {
            Object key = keys.next();
            if (key instanceof String)
            {
                Object value = properties.get(key);
                logger.debug("Binding " + key + " to " + value);
                context.bind((String) key, value);
            }
        }
        return context;
    }

}
