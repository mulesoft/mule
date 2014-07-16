/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jboss.transaction;

import org.mule.api.config.MuleConfiguration;
import org.mule.api.transaction.TransactionManagerFactory;

import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.common.util.propertyservice.PropertiesFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.transaction.TransactionManager;

import org.springframework.util.StringUtils;

public class JBossArjunaTransactionManagerFactory implements TransactionManagerFactory
{

    public static final String PROPERTY_USER_DIR = "user.dir";
    public static final String PROPERTY_ENVIRONMENT_OBJECTSTORE_DIR = "ObjectStoreEnvironmentBean.objectStoreDir";
    public static final String PROPERTY_OBJECTSTORE_DIR = "com.arjuna.ats.arjuna.objectstore.objectStoreDir";
    public static final String PROPERTY_NODE_IDENTIFIER = "com.arjuna.ats.arjuna.nodeIdentifier";
    public static final String PROPERTY_DEFAULT_TIMEOUT = "com.arjuna.ats.arjuna.coordinator.defaultTimeout";
    public static final String PROPERTY_TX_REAPER_TIMEOUT = "com.arjuna.ats.arjuna.coordinator.txReaperTimeout";
    public static final String OS_ROOT = "os";

    private Map<String, String> properties = new HashMap<String, String>();


    private TransactionManager tm;

    public synchronized TransactionManager create(MuleConfiguration config) throws Exception
    {
        if (tm == null)
        {
            final String muleInternalDir = config.getWorkingDirectory();
            String objectStoreDir = properties.get(PROPERTY_OBJECTSTORE_DIR);

            if (StringUtils.isEmpty(objectStoreDir))
            {
                objectStoreDir = muleInternalDir + "/transaction-log";
            }

            arjPropertyManager.getObjectStoreEnvironmentBean().setObjectStoreDir(objectStoreDir);
            arjPropertyManager.getObjectStoreEnvironmentBean().setLocalOSRoot(OS_ROOT);

            Properties props = PropertiesFactory.getDefaultProperties();
            props.setProperty(PROPERTY_ENVIRONMENT_OBJECTSTORE_DIR, objectStoreDir);
            props.setProperty(PROPERTY_OBJECTSTORE_DIR, objectStoreDir);
            props.setProperty(PROPERTY_USER_DIR, muleInternalDir);

            if (!properties.containsKey(PROPERTY_NODE_IDENTIFIER))
            {
                try
                {
                    InetAddress address = InetAddress.getLocalHost();
                    final String xaNodeId = MessageFormat.format("Mule[{0}/{1}]",
                                                                 address.getHostName(), address.getHostAddress());
                    arjPropertyManager.getCoreEnvironmentBean().setNodeIdentifier(xaNodeId);
                }
                catch (UnknownHostException e)
                {
                    // ignore, let the defaults be generated
                }
            }
            // TODO JBossTS has many more properties. Should implement something generic, looking for properties with the annotation.
            // see http://docs.jboss.org/jbosstm/docs/4.2.3/javadoc/jts/com/arjuna/ats/arjuna/common/Environment.html
            // and com.arjuna.ats.arjuna.common.CoreEnvironmentBean, CoordinatorEnvironmentBean and ObjectStoreEnvironmentBean 
            //Setting the timeout if any
            if(properties.containsKey(PROPERTY_DEFAULT_TIMEOUT)){
                arjPropertyManager.getCoordinatorEnvironmentBean().setDefaultTimeout(Integer.valueOf(properties.get(PROPERTY_DEFAULT_TIMEOUT)));
            }
            //Setting the tx reaper timeout if any
            if(properties.containsKey(PROPERTY_TX_REAPER_TIMEOUT)){
                arjPropertyManager.getCoordinatorEnvironmentBean().setTxReaperTimeout(Long.valueOf(properties.get(PROPERTY_TX_REAPER_TIMEOUT)));
            }
            /*for (Map.Entry<String, String> entry : properties.entrySet())
            {
                arjPropertyManager.propertyManager.setProperty(entry.getKey(), entry.getValue());
            }*/

            tm = com.arjuna.ats.jta.TransactionManager.transactionManager();

        }
        return tm;
    }

    public Map<String, String> getProperties()
    {
        return properties;
    }

    public void setProperties(Map<String, String> properties)
    {
        this.properties = properties;
    }
}
