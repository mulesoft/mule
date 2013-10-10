/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.jndi;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;

import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractJndiNameResolver implements JndiNameResolver
{

    protected final Log logger = LogFactory.getLog(getClass());

    private String jndiProviderUrl;
    private String jndiInitialFactory;
    private Map jndiProviderProperties;

    // Default contextFactory
    private InitialContextFactory contextFactory = new InitialContextFactory()
    {
        public Context getInitialContext(Hashtable<?, ?> hashtable) throws NamingException
        {
            return new InitialContext(hashtable);
        }
    };

    /**
     * Creates a JNDI context using the current {@link #contextFactory}
     *
     * @return a new {@link Context} instance. Callers must provide concurrent
     *         access control on the returned value.
     * @throws NamingException if there is a problem during the context creation.
     */
    protected Context createInitialContext() throws NamingException
    {
        return contextFactory.getInitialContext(getContextProperties());
    }

    protected Hashtable getContextProperties()
    {
        if ((jndiInitialFactory == null) && (jndiProviderProperties == null
                                             || !jndiProviderProperties.containsKey(Context.INITIAL_CONTEXT_FACTORY)))
        {
            throw new IllegalArgumentException("Undefined value for jndiInitialFactory property");
        }

        Hashtable<String, Object> props = new Hashtable<String, Object>();

        if (jndiInitialFactory != null)
        {
            props.put(Context.INITIAL_CONTEXT_FACTORY, jndiInitialFactory);
        }

        if (jndiProviderUrl != null)
        {
            props.put(Context.PROVIDER_URL, jndiProviderUrl);
        }

        if (jndiProviderProperties != null)
        {
            props.putAll(jndiProviderProperties);
        }

        return props;
    }

    public String getJndiProviderUrl()
    {
        return jndiProviderUrl;
    }

    public void setJndiProviderUrl(String jndiProviderUrl)
    {
        this.jndiProviderUrl = jndiProviderUrl;
    }

    public String getJndiInitialFactory()
    {
        return jndiInitialFactory;
    }

    public void setJndiInitialFactory(String jndiInitialFactory)
    {
        this.jndiInitialFactory = jndiInitialFactory;
    }

    public Map getJndiProviderProperties()
    {
        return jndiProviderProperties;
    }

    public void setJndiProviderProperties(Map jndiProviderProperties)
    {
        this.jndiProviderProperties = jndiProviderProperties;
    }

    public InitialContextFactory getContextFactory()
    {
        return contextFactory;
    }

    public void setContextFactory(InitialContextFactory contextFactory)
    {
        if (contextFactory == null)
        {
            throw new IllegalArgumentException("Context factory cannot be null");
        }

        this.contextFactory = contextFactory;
    }

    public void dispose()
    {
        // Does nothing
    }

    public void initialise() throws InitialisationException
    {
        // Does nothing
    }

    public void start() throws MuleException
    {
        // Does nothing
    }

    public void stop() throws MuleException
    {
        // Does nothing
    }
}
