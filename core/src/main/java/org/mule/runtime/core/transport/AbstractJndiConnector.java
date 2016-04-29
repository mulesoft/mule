/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transport;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.config.i18n.CoreMessages;

import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * This class acts as common baseclass for both Rmi & EjbConnector Resolves Jndi root for connector usage
 * 
 */
public abstract class AbstractJndiConnector extends AbstractConnector
{
    protected String jndiInitialFactory;

    protected String jndiUrlPkgPrefixes;

    protected String jndiProviderUrl;

    protected Context jndiContext;

    protected Map jndiProviderProperties = null;

    public AbstractJndiConnector(MuleContext context)
    {
        super(context);
    }

    protected void initJndiContext() throws InitialisationException
    {
        if (null == jndiContext)
        {
            Hashtable props = new Hashtable();

            if (null != jndiInitialFactory)
            {
                props.put(Context.INITIAL_CONTEXT_FACTORY, jndiInitialFactory);
            }

            if (jndiProviderUrl != null)
            {
                props.put(Context.PROVIDER_URL, jndiProviderUrl);
            }

            if (jndiUrlPkgPrefixes != null)
            {
                props.put(Context.URL_PKG_PREFIXES, jndiUrlPkgPrefixes);
            }

            if (jndiProviderProperties != null)
            {
                props.putAll(jndiProviderProperties);
            }
            try
            {
                jndiContext = new InitialContext(props);
            }
            catch (NamingException e)
            {
                throw new InitialisationException(e, this);
            }
        }
    }

    public Context getJndiContext(String jndiProviderUrl) throws InitialisationException
    {
        try
        {
            setJndiProviderUrl(jndiProviderUrl);

            initJndiContext();
        }
        catch (Exception e)
        {
            throw new InitialisationException(
                CoreMessages.failedToCreate("AbstractJndiConnector"), e, this);
        }

        return jndiContext;
    }

    public Context getJndiContext()
    {

        return jndiContext;
    }

    public void setJndiContext(Context jndiContext)
    {
        this.jndiContext = jndiContext;
    }

    public void setJndiInitialFactory(String jndiInitialFactory)
    {
        this.jndiInitialFactory = jndiInitialFactory;
    }

    public String getJndiInitialFactory()
    {
        return jndiInitialFactory;
    }

    public void setJndiUrlPkgPrefixes(String jndiUrlPkgPrefixes)
    {
        this.jndiUrlPkgPrefixes = jndiUrlPkgPrefixes;
    }

    public String getJndiUrlPkgPrefixes()
    {
        return jndiUrlPkgPrefixes;
    }

    public String getJndiProviderUrl()
    {
        return jndiProviderUrl;
    }

    public void setJndiProviderUrl(String jndiProviderUrl)
    {
        this.jndiProviderUrl = jndiProviderUrl;
    }

    public Map getJndiProviderProperties()
    {
        return jndiProviderProperties;
    }

    public void setJndiProviderProperties(Map jndiProviderProperties)
    {
        this.jndiProviderProperties = jndiProviderProperties;
    }
}
