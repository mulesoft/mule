/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.jndi;

import org.mule.config.spring.MuleApplicationContext;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;

/**
 * TODO
 */
public class SpringInitialContextFactory implements InitialContextFactory
{
    private static final transient Log log = LogFactory.getLog(SpringInitialContextFactory.class);

    private static Map cache = new HashMap();

    private static Context singleton;

    /**
     * A factory method which can be used to initialise a singleton JNDI context from inside a Spring.xml
     * such that future calls to new InitialContext() will reuse it
     */
    public static Context makeInitialContext() {
        singleton = new DefaultSpringJndiContext();
        return singleton;
    }

    public Context getInitialContext(Hashtable environment) throws NamingException
    {
        if (singleton != null) {
            return singleton;
        }
        Resource resource = null;
        Object value = environment.get(Context.PROVIDER_URL);
        String key = "jndi.xml";
        if (value == null) {
            resource = new ClassPathResource(key);
        }
        else {
            if (value instanceof Resource) {
                resource = (Resource) value;
            }
            else {
                ResourceEditor editor = new ResourceEditor();
                key = value.toString();
                editor.setAsText(key);
                resource = (Resource) editor.getValue();
            }
        }
        BeanFactory context = loadContext(resource, key);
        Context answer = (Context) context.getBean("jndi");
        if (answer == null) {
            log.warn("No JNDI context available in JNDI resource: " + resource);
            answer = new DefaultSpringJndiContext(environment, new ConcurrentHashMap());
        }
        return answer;
    }

    protected BeanFactory loadContext(Resource resource, String key) {
        synchronized (cache) {
            BeanFactory answer = (BeanFactory) cache.get(key);
            if (answer == null) {
                answer =  createContext(resource);
                cache.put(key, answer);
            }
            return answer;
        }
    }

    protected BeanFactory createContext(Resource resource) {
        log.info("Loading JNDI context from: " + resource);
        return new MuleApplicationContext(new Resource[] {resource});
    }
}
