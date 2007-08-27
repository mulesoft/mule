/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.jotm;

import org.mule.umo.manager.UMOTransactionManagerFactory;

import javax.transaction.TransactionManager;

import org.objectweb.jotm.Current;
import org.objectweb.jotm.Jotm;

/**
 * This factory retrieves the transaction manager for <a
 * href="http://jotm.objectweb.org">JOTM </a>. If an existing JOTM instance exists
 * (for example if running on JOnAS) it will retrieve it, else if will create a new
 * local JOTM instance.
 * 
 */
public class JotmTransactionManagerFactory implements UMOTransactionManagerFactory
{
    private TransactionManager jotmCurrent;

    public JotmTransactionManagerFactory()
    {
        super();
    }

    /**
     * Retrieves the JOTM Current object that implements the TransactionManager
     * interface.
     * 
     * @see org.mule.umo.manager.UMOTransactionManagerFactory#create()
     */
    public synchronized TransactionManager create() throws Exception
    {
        if (jotmCurrent == null)
        {
            // check for already active JOTM instance
            jotmCurrent = Current.getCurrent();
            // if none found, create new local JOTM instance
            if (jotmCurrent == null)
            {
                jotmCurrent = new Jotm(true, false).getTransactionManager();
            }
        }
        return jotmCurrent;
    }

}
