/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transaction.lookup;

import org.mule.config.i18n.CoreMessages;
import org.mule.impl.container.JndiContextHelper;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOTransactionManagerFactory;
import org.mule.util.StringUtils;

import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A factory performing a JNDI lookup for TransactionManager. <p/> NOTE: Java EE 1.4
 * specification does not mandate application server vendors to expose a
 * TransactionManager for direct use, nor does it name the standard way to locate it.
 * For some servers the TransactionManager is not even available in the global JNDI
 * namespace, so your only bet is to run Mule in the same JVM as the application
 * server.
 */
public class GenericTransactionManagerLookupFactory implements UMOTransactionManagerFactory, Initialisable
{
    protected final Log logger = LogFactory.getLog(getClass());

    protected Context context;

    private Map environment;

    private TransactionManager txManager;

    private String jndiName;

    public String getJndiName()
    {
        return jndiName;
    }

    public void setJndiName(final String jndiName)
    {
        this.jndiName = jndiName;
    }

    public TransactionManager getTxManager()
    {
        return txManager;
    }

    public void setTxManager(final TransactionManager txManager)
    {
        this.txManager = txManager;
    }

    public Map getEnvironment()
    {
        return environment;
    }

    public void setEnvironment(final Map environment)
    {
        this.environment = environment;
    }

    public Context getContext()
    {
        return context;
    }

    public void setContext(final Context context)
    {
        this.context = context;
    }

    /** @see org.mule.umo.manager.UMOTransactionManagerFactory#create() */
    public TransactionManager create() throws Exception
    {
        // implementing the Initilisable interface does not call it??
        initialise();
        if (txManager == null)
        {
            txManager = (TransactionManager) context.lookup(jndiName);
        }

        return txManager;
    }

    /**
     * Method used to perform any initialisation work. If a fatal error occurs during
     * initialisation an <code>InitialisationException</code> should be thrown,
     * causing the Mule instance to shutdown. If the error is recoverable, say by
     * retrying to connect, a <code>RecoverableException</code> should be thrown.
     * There is no guarantee that by throwing a Recoverable exception that the Mule
     * instance will not shut down.
     *
     * @throws org.mule.umo.lifecycle.InitialisationException
     *          if a fatal error occurs
     *          causing the Mule instance to shutdown
     */
    public void initialise() throws InitialisationException
    {
        if (txManager == null && StringUtils.isEmpty(StringUtils.trim(jndiName)))
        {
            throw new InitialisationException(CoreMessages.propertiesNotSet("jndiName"), this);
        }

        try
        {
            if (context == null)
            {
                context = JndiContextHelper.initialise(getEnvironment());
            }
        }
        catch (NamingException e)
        {
            throw new InitialisationException(CoreMessages.failedToCreate("Jndi context"),
                    e, this);
        }
    }
}
