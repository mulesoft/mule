/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.lifecycle.InitialisationException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import java.util.Hashtable;
import java.util.Map;

/**
 * Code by (c) 2005 P.Oikari.
 * <p/>
 * This class acts as common baseclass for both Rmi & EjbConnector
 * Resolves Jndi root for connector usage
 *
 * @author <a href="mailto:tsuppari@yahoo.co.uk">P.Oikari</a>
 * @version $Revision$
 */


public abstract class AbstractJndiConnector extends AbstractServiceEnabledConnector {
    protected String jndiInitialFactory;

    protected String jndiUrlPkgPrefixes;

    protected String jndiProviderUrl;

    protected Context jndiContext;

    protected Map jndiProviderProperties = null;

    protected void initJndiContext() throws NamingException, InitialisationException {
        if (null == jndiContext) {
            Hashtable props = new Hashtable();

            if (null != jndiInitialFactory) {
                props.put(Context.INITIAL_CONTEXT_FACTORY, jndiInitialFactory);
            }

            if (jndiProviderUrl != null) {
                props.put(Context.PROVIDER_URL, jndiProviderUrl);
            }

            if (jndiUrlPkgPrefixes != null) {
                props.put(Context.URL_PKG_PREFIXES, jndiUrlPkgPrefixes);
            }

            if(jndiProviderProperties!=null) {
                props.putAll(jndiProviderProperties);
            }
            jndiContext = new InitialContext(props);
        }
    }

    public Context getJndiContext(String jndiProviderUrl) throws InitialisationException {
        try {
            setJndiProviderUrl(jndiProviderUrl);

            initJndiContext();
        } catch (Exception e) {
            throw new InitialisationException(new Message(Messages.FAILED_TO_CREATE_X, "AbstractJndiConnector"), e, this);
        }

        return jndiContext;
    }

    public void setJndiContext(Context jndiContext) {
        this.jndiContext = jndiContext;
    }

    public void setJndiInitialFactory(String jndiInitialFactory) {
        this.jndiInitialFactory = jndiInitialFactory;
    }

    public String getJndiInitialFactory() {
        return jndiInitialFactory;
    }

    public void setJndiUrlPkgPrefixes(String jndiUrlPkgPrefixes) {
        this.jndiUrlPkgPrefixes = jndiUrlPkgPrefixes;
    }

    public String getJndiUrlPkgPrefixes() {
        return jndiUrlPkgPrefixes;
    }

    public String getJndiProviderUrl() {
        return jndiProviderUrl;
    }

    public void setJndiProviderUrl(String jndiProviderUrl) {
        this.jndiProviderUrl = jndiProviderUrl;
    }

    public Map getJndiProviderProperties() {
        return jndiProviderProperties;
    }

    public void setJndiProviderProperties(Map jndiProviderProperties) {
        this.jndiProviderProperties = jndiProviderProperties;
    }
}
