/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 *
 */
package org.mule.transaction;

import org.mule.umo.manager.UMOTransactionManagerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.transaction.TransactionManager;

/**
 * A factory performing a JNDI lookup for TransactionManager.
 * <p/>
 * NOTE: Java EE 1.4 specification does not mandate application server vendors
 * to expose a TransactionManager for direct use, nor does it name the
 * standard way to locate it. For some servers the TransactionManager is not
 * even available in the global JNDI namespace, so your only bet is to run Mule
 * in the same JVM as the application server.
 *
 * @author <a href="mailto:aperepel@itci.com">Andrew Perepelytsya</a>
 */
public class GenericTransactionManagerLookupFactory implements UMOTransactionManagerFactory
{
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

    /**
     *
     * @see org.mule.umo.manager.UMOTransactionManagerFactory#create()
     */
    public TransactionManager create() throws Exception
    {
        if (txManager == null) {
            // TODO provide a way to pass in the JNDI 'environment' for multiple JNDI servers support
            Context ctx = new InitialContext();
            txManager = (TransactionManager) ctx.lookup(jndiName);
        }

        return txManager;
    }

}
