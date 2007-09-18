/*
 * $Id:JBossArjunaTransactionManagerFactory.java 8215 2007-09-05 16:56:51Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.jboss.transactions;

import org.mule.RegistryContext;
import org.mule.umo.manager.UMOTransactionManagerFactory;

import com.arjuna.ats.arjuna.common.Environment;
import com.arjuna.ats.arjuna.common.arjPropertyManager;

import javax.transaction.TransactionManager;

public class JBossArjunaTransactionManagerFactory implements UMOTransactionManagerFactory
{
    //static
    //{
        //arjPropertyManager.propertyManager.setProperty(LogFactory.LOGGER_PROPERTY, "log4j_releveler");
        //arjPropertyManager.propertyManager.setProperty(LogFactory.LOGGER_PROPERTY, "jakarta");
        //arjPropertyManager.propertyManager.setProperty(LogFactory.DEBUG_LEVEL, String.valueOf(DebugLevel.FULL_DEBUGGING));
        //commonPropertyManager.propertyManager.setProperty(LogFactory.LOGGER_PROPERTY, "jakarta");
        //commonPropertyManager.propertyManager.setProperty(LogFactory.DEBUG_LEVEL, String.valueOf(DebugLevel.FULL_DEBUGGING));
    //}

    private TransactionManager tm;

    public JBossArjunaTransactionManagerFactory()
    {
        //arjPropertyManager.propertyManager.setProperty("com.arjuna.ats.arjuna.objectstore.objectStoreType", "ShadowNoFileLockStore");
        //arjPropertyManager.propertyManager.setProperty(Environment.OBJECTSTORE_TYPE, ArjunaNames.Implementation_ObjectStore_JDBCStore().stringForm());
        final String muleInternalDir = RegistryContext.getRegistry().getConfiguration().getWorkingDirectory();
        arjPropertyManager.propertyManager.setProperty(Environment.OBJECTSTORE_DIR, muleInternalDir + "/transaction-log");
    }

    public synchronized TransactionManager create() throws Exception
    {
        if (tm == null)
        {
            tm = com.arjuna.ats.jta.TransactionManager.transactionManager();
        }
        return tm;
    }

}
