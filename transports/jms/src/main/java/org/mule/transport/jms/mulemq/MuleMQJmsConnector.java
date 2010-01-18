/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.mulemq;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.transport.jms.JmsConnector;
import org.mule.transport.jms.JmsConstants;
import org.mule.transport.jms.i18n.JmsMessages;
import org.mule.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Map;

import javax.jms.ConnectionFactory;

public class MuleMQJmsConnector extends JmsConnector
{
    public static final String MULEMQ_CONNECTION_FACTORY_CLASS = "com.pcbsys.nirvana.nJMS.ConnectionFactoryImpl";

    // The default values for the connection factory properties
    public static final String DEFAULT_REALM_URL = "nsp://localhost:9000";
    public static final String DEFAULT_BUFFER_OUTPUT = "queued";
    public static final boolean DEFAULT_SYNC_WRITES = false;
    public static final int DEFAULT_SYNC_BATCH_SIZE = 50;
    public static final int DEFAULT_SYNC_TIME = 20;
    public static final int DEFAULT_GLOBAL_STORE_CAPACITY = 1000;
    public static final int DEFAULT_MAX_UNACKED_SIZE = 100;
    public static final boolean DEFAULT_USE_JMS_ENGINE = true;
    public static final int DEFAULT_QUEUE_WINDOW_SIZE = 1000;
    public static final int DEFAULT_AUTO_ACK_COUNT = 500;
    public static final boolean DEFAULT_ENABLE_SHARED_DURABLE = false;
    public static final boolean DEFAULT_RANDOMISE_R_NAMES = false;
    public static final int DEFAULT_MAX_REDELIVERY = 100;
    public static final int DEFAULT_MESSAGE_THREAD_POOL_SIZE = 30;
    public static final boolean DEFAULT_DISC_ON_CLUSTER_FAILURE = true;
    public static final int DEFAULT_INITIAL_RETRY_COUNT = 2;
    public static final boolean DEFAULT_RETRY_COMMIT = false;

    // properties to be set on the connector all initialised to their respective
    // default value
    private String realmURL = DEFAULT_REALM_URL;
    private String bufferOutput = DEFAULT_BUFFER_OUTPUT;
    private boolean syncWrites = DEFAULT_SYNC_WRITES;
    private int syncBatchSize = DEFAULT_SYNC_BATCH_SIZE;
    private int syncTime = DEFAULT_SYNC_TIME;
    private int globalStoreCapacity = DEFAULT_GLOBAL_STORE_CAPACITY;
    private int maxUnackedSize = DEFAULT_MAX_UNACKED_SIZE;
    private boolean useJMSEngine = DEFAULT_USE_JMS_ENGINE;
    private int queueWindowSize = DEFAULT_QUEUE_WINDOW_SIZE;
    private int autoAckCount = DEFAULT_AUTO_ACK_COUNT;
    private boolean enableSharedDurable = DEFAULT_ENABLE_SHARED_DURABLE;
    private boolean randomiseRNames = DEFAULT_RANDOMISE_R_NAMES;
    private int muleMqMaxRedelivery = DEFAULT_MAX_REDELIVERY;
    private int messageThreadPoolSize = DEFAULT_MESSAGE_THREAD_POOL_SIZE;
    private boolean discOnClusterFailure = DEFAULT_DISC_ON_CLUSTER_FAILURE;
    private int initialRetryCount = DEFAULT_INITIAL_RETRY_COUNT;
    private boolean retryCommit = DEFAULT_RETRY_COMMIT;

    // property names
    protected static final String BUFFER_OUTPUT = "BufferOutput";
    protected static final String SYNC_WRITES = "nirvana.syncWrites";
    protected static final String SYNC_BATCH_SIZE = "nirvana.syncBatchSize";
    protected static final String SYNC_TIME = "nirvana.syncTime";
    protected static final String GLOBAL_STORE_CAPACITY = "nirvana.globalStoreCapacity";
    protected static final String MAX_UNACKED_SIZE = "nirvana.maxUnackedSize";
    protected static final String USE_JMS_ENGINE = "nirvana.useJMSEngine";
    protected static final String QUEUE_WINDOW_SIZE = "nirvana.queueWindowSize";
    protected static final String AUTO_ACK_COUNT = "nirvana.autoAckCount";
    protected static final String ENABLE_SHARED_DURABLE = "nirvana.enableSharedDurable";
    protected static final String RANDOMISE_R_NAMES = "nirvana.randomiseRNames";
    protected static final String MAX_REDELIVERY = "nirvana.maxRedelivery";
    protected static final String MESSAGE_THREAD_POOL_SIZE = "nirvana.messageThreadPoolSize";
    protected static final String DISC_ON_CLUSTER_FAILURE = "nirvana.discOnClusterFailure";
    protected static final String INITIAL_RETRY_COUNT = "nirvana.initialRetryCount";
    protected static final String RETRY_COMMIT = "nirvana.retryCommit";

    public boolean supportJms102bSpec = false;

    public MuleMQJmsConnector()
    {
        super();
        super.setSpecification(JmsConstants.JMS_SPECIFICATION_11);
    }

    public boolean isSupportJms102bSpec()
    {
        return supportJms102bSpec;
    }

    public void setSupportJms102bSpec(boolean supportJms102bSpec)
    {
        this.supportJms102bSpec = supportJms102bSpec;
    }

    /*
     * We need to default the specification to 1.1, the xsd defaults to 1.0.2b, we
     * wouldn't have to do this here if the default was set in JmsConnector only, In
     * that case setting the default in the constructor would have been sufficient.
     */
    @Override
    public void setSpecification(String specification)
    {
        if (!isSupportJms102bSpec() && specification.equals(JmsConstants.JMS_SPECIFICATION_102B))
        {
            logger.warn(JmsMessages.errorMuleMqJmsSpecification());
            specification = JmsConstants.JMS_SPECIFICATION_11;
        }
        super.setSpecification(specification);
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        if (!isSupportJms102bSpec() && getSpecification().equals(JmsConstants.JMS_SPECIFICATION_102B))
        {
            throw new InitialisationException(JmsMessages.errorMuleMqJmsSpecification(), this);
        }
        super.doInitialise();
    }

    @Override
    protected ConnectionFactory getDefaultConnectionFactory()
    {
        try
        {
            ConnectionFactory connectionFactory = (ConnectionFactory) ClassUtils.instanciateClass(
                getMuleMQFactoryClass(), getRealmURL());
            applyVendorSpecificConnectionFactoryProperties(connectionFactory);
            return connectionFactory;
        }
        catch (Exception e)
        {
            handleException(e);
        }
        return null;
    }

    private void applyVendorSpecificConnectionFactoryProperties(ConnectionFactory connectionFactory)
    {
        // set the properties first on the prop hash table
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(BUFFER_OUTPUT, bufferOutput);
        props.put(SYNC_WRITES, Boolean.toString(syncWrites));
        props.put(SYNC_BATCH_SIZE, Integer.toString(syncBatchSize));
        props.put(SYNC_TIME, Integer.toString(syncTime));
        props.put(GLOBAL_STORE_CAPACITY, Integer.toString(globalStoreCapacity));
        props.put(MAX_UNACKED_SIZE, Integer.toString(maxUnackedSize));
        props.put(USE_JMS_ENGINE, Boolean.toString(useJMSEngine));
        props.put(QUEUE_WINDOW_SIZE, Integer.toString(queueWindowSize));
        props.put(AUTO_ACK_COUNT, Integer.toString(autoAckCount));
        props.put(ENABLE_SHARED_DURABLE, Boolean.toString(enableSharedDurable));
        props.put(RANDOMISE_R_NAMES, Boolean.toString(randomiseRNames));
        props.put(MAX_REDELIVERY, Integer.toString(muleMqMaxRedelivery));
        props.put(MESSAGE_THREAD_POOL_SIZE, Integer.toString(messageThreadPoolSize));
        props.put(DISC_ON_CLUSTER_FAILURE, Boolean.toString(discOnClusterFailure));
        props.put(INITIAL_RETRY_COUNT, Integer.toString(initialRetryCount));
        props.put(RETRY_COMMIT, Boolean.toString(retryCommit));

        // if the user used the connectionFactoryProperties map, these will override
        // the properties on the connector
        Map<String, Object> connectionFactoryProperties = getConnectionFactoryProperties();
        if (connectionFactoryProperties != null)
        {
            props.putAll(connectionFactoryProperties);
        }

        try
        {
            // use reflection to set the properties on the connection factory
            Method setPropertiesMethod = connectionFactory.getClass().getMethod("setProperties",
                Hashtable.class);
            setPropertiesMethod.invoke(connectionFactory, props);
        }
        catch (Exception e)
        {
            logger.error("Can not set properties on the MuleMQ connection factory " + e);
        }
    }

    // returns the connection factory class name as a string. This method will be
    // used by getDefaultConnectionFactory() to acquire the appropriate class to
    // initialise
    protected String getMuleMQFactoryClass()
    {
        return MULEMQ_CONNECTION_FACTORY_CLASS;
    }

    public String getRealmURL()
    {
        return realmURL;
    }

    public void setRealmURL(String realmURL)
    {
        this.realmURL = realmURL;
    }

    public String getBufferOutput()
    {
        return bufferOutput;
    }

    public void setBufferOutput(String bufferOutput)
    {
        this.bufferOutput = bufferOutput;
    }

    public boolean isSyncWrites()
    {
        return syncWrites;
    }

    public void setSyncWrites(boolean syncWrites)
    {
        this.syncWrites = syncWrites;
    }

    public int getSyncBatchSize()
    {
        return syncBatchSize;
    }

    public void setSyncBatchSize(int syncBatchSize)
    {
        this.syncBatchSize = syncBatchSize;
    }

    public int getSyncTime()
    {
        return syncTime;
    }

    public void setSyncTime(int syncTime)
    {
        this.syncTime = syncTime;
    }

    public int getGlobalStoreCapacity()
    {
        return globalStoreCapacity;
    }

    public void setGlobalStoreCapacity(int globalStoreCapacity)
    {
        this.globalStoreCapacity = globalStoreCapacity;
    }

    public int getMaxUnackedSize()
    {
        return maxUnackedSize;
    }

    public void setMaxUnackedSize(int maxUnackedSize)
    {
        this.maxUnackedSize = maxUnackedSize;
    }

    public boolean isUseJMSEngine()
    {
        return useJMSEngine;
    }

    public void setUseJMSEngine(boolean useJMSEngine)
    {
        this.useJMSEngine = useJMSEngine;
    }

    public int getQueueWindowSize()
    {
        return queueWindowSize;
    }

    public void setQueueWindowSize(int queueWindowSize)
    {
        this.queueWindowSize = queueWindowSize;
    }

    public int getAutoAckCount()
    {
        return autoAckCount;
    }

    public void setAutoAckCount(int autoAckCount)
    {
        this.autoAckCount = autoAckCount;
    }

    public boolean isEnableSharedDurable()
    {
        return enableSharedDurable;
    }

    public void setEnableSharedDurable(boolean enableSharedDurable)
    {
        this.enableSharedDurable = enableSharedDurable;
    }

    public boolean isRandomiseRNames()
    {
        return randomiseRNames;
    }

    public void setRandomiseRNames(boolean randomiseRNames)
    {
        this.randomiseRNames = randomiseRNames;
    }

    public int getMessageThreadPoolSize()
    {
        return messageThreadPoolSize;
    }

    public void setMessageThreadPoolSize(int messageThreadPoolSize)
    {
        this.messageThreadPoolSize = messageThreadPoolSize;
    }

    public boolean isDiscOnClusterFailure()
    {
        return discOnClusterFailure;
    }

    public void setDiscOnClusterFailure(boolean discOnClusterFailure)
    {
        this.discOnClusterFailure = discOnClusterFailure;
    }

    public int getInitialRetryCount()
    {
        return initialRetryCount;
    }

    public void setInitialRetryCount(int initialRetryCount)
    {
        this.initialRetryCount = initialRetryCount;
    }

    public int getMuleMqMaxRedelivery()
    {
        return muleMqMaxRedelivery;
    }

    public void setMuleMqMaxRedelivery(int mulqMqMaxRedelivery)
    {
        this.muleMqMaxRedelivery = mulqMqMaxRedelivery;
    }

    public void setRetryCommit(boolean retryCommit)
    {
        this.retryCommit = retryCommit;
    }

    public boolean isRetryCommit()
    {
        return retryCommit;
    }
}
