/*
 * $Id:JBossArjunaTransactionManagerFactory.java 8215 2007-09-05 16:56:51Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.jboss.transaction;

import org.mule.MuleServer;
import org.mule.api.transaction.TransactionManagerFactory;

import com.arjuna.ats.arjuna.common.Environment;
import com.arjuna.ats.arjuna.common.arjPropertyManager;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.transaction.TransactionManager;

public class JBossArjunaTransactionManagerFactory implements TransactionManagerFactory
{

    private Map<String, String> properties = new HashMap<String, String>();

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


    }

    public synchronized TransactionManager create() throws Exception
    {
        if (tm == null)
        {
            // let the user override those in the config
            if (!properties.containsKey(Environment.OBJECTSTORE_DIR))
            {
                final String muleInternalDir = MuleServer.getMuleContext().getConfiguration().getWorkingDirectory();
                arjPropertyManager.propertyManager.setProperty(Environment.OBJECTSTORE_DIR, muleInternalDir + "/transaction-log");
            }

            if (!properties.containsKey(Environment.XA_NODE_IDENTIFIER))
            {
                try
                {
                    InetAddress address = InetAddress.getLocalHost();
                    final String xaNodeId = MessageFormat.format("Mule[{0}/{1}]",
                                                                 address.getHostName(), address.getHostAddress());
                    properties.put(Environment.XA_NODE_IDENTIFIER, xaNodeId);
                }
                catch (UnknownHostException e)
                {
                    // ignore, let the defaults be generated
                }
            }


            for (Map.Entry entry : properties.entrySet())
            {
                arjPropertyManager.propertyManager.setProperty((String) entry.getKey(), (String) entry.getValue());
            }
            tm = com.arjuna.ats.jta.TransactionManager.transactionManager();

        }
        return tm;
    }

    public Map getProperties()
    {
        return properties;
    }

    public void setProperties(Map properties)
    {
        this.properties = properties;
    }
}
