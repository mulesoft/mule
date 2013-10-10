/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jboss.transaction;

import org.mule.api.config.MuleConfiguration;
import org.mule.api.transaction.TransactionManagerFactory;

import com.arjuna.ats.arjuna.common.arjPropertyManager;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.transaction.TransactionManager;

public class JBossArjunaTransactionManagerFactory implements TransactionManagerFactory
{

    public static final String PROPERTY_OBJECTSTORE_DIR = "com.arjuna.ats.arjuna.objectstore.objectStoreDir";
    public static final String PROPERTY_NODE_IDENTIFIER = "com.arjuna.ats.arjuna.nodeIdentifier";
    public static final String PROPERTY_DEFAULT_TIMEOUT = "com.arjuna.ats.arjuna.coordinator.defaultTimeout";
    public static final String PROPERTY_TX_REAPER_TIMEOUT = "com.arjuna.ats.arjuna.coordinator.txReaperTimeout";

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

    public synchronized TransactionManager create(MuleConfiguration config) throws Exception
    {
        if (tm == null)
        {
            // let the user override those in the config
            if (!properties.containsKey(PROPERTY_OBJECTSTORE_DIR))
            {
                final String muleInternalDir = config.getWorkingDirectory();
                arjPropertyManager.getObjectStoreEnvironmentBean().setObjectStoreDir(muleInternalDir + "/transaction-log");
            }

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
