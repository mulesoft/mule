/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.jndi;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.AbstractXmlApplicationContext;
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
     * A factory method which can be used to initialise a singleton JNDI context from
     * inside a Spring.xml such that future calls to new InitialContext() will reuse
     * it
     */
    public static Context makeInitialContext()
    {
        singleton = new DefaultSpringJndiContext();
        return singleton;
    }

    public Context getInitialContext(Hashtable environment) throws NamingException
    {
        if (singleton != null)
        {
            return singleton;
        }
        Resource resource = null;
        Object value = environment.get(Context.PROVIDER_URL);
        String key = "jndi.xml";
        if (value == null)
        {
            resource = new ClassPathResource(key);
        }
        else
        {
            if (value instanceof Resource)
            {
                resource = (Resource) value;
            }
            else
            {
                ResourceEditor editor = new ResourceEditor();
                key = value.toString();
                editor.setAsText(key);
                resource = (Resource) editor.getValue();
            }
        }
        BeanFactory context = loadContext(resource, key);
        Context answer = (Context) context.getBean("jndi");
        if (answer == null)
        {
            log.warn("No JNDI context available in JNDI resource: " + resource);
            answer = new DefaultSpringJndiContext(environment, new ConcurrentHashMap());
        }
        return answer;
    }

    protected BeanFactory loadContext(Resource resource, String key)
    {
        synchronized (cache)
        {
            BeanFactory answer = (BeanFactory) cache.get(key);
            if (answer == null)
            {
                answer = createContext(resource);
                cache.put(key, answer);
            }
            return answer;
        }
    }

    protected BeanFactory createContext(Resource resource)
    {
        log.info("Loading JNDI context from: " + resource);
        return new SpringInitialContextApplicationContext(new Resource[]{resource});
    }

    /**
     * Simple implementation of AbstractXmlApplicationContext that allows
     * {@link Resource} to be used in the constructor
     */
    class SpringInitialContextApplicationContext extends AbstractXmlApplicationContext
    {
        private Resource[] configResources;

        public SpringInitialContextApplicationContext(Resource[] resources)
        {
            super();
            configResources = resources;
            refresh();
        }

        protected Resource[] getConfigResources()
        {
            return configResources;
        }
    }

}
