/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config;

import org.mule.MuleRuntimeException;
import org.mule.RegistryContext;
import org.mule.config.i18n.CoreMessages;
import org.mule.providers.ConnectionStrategy;
import org.mule.providers.SingleAttemptConnectionStrategy;
import org.mule.registry.RegistrationException;
import org.mule.umo.UMOException;
import org.mule.umo.manager.DefaultWorkListener;
import org.mule.util.FileUtils;
import org.mule.util.StringUtils;
import org.mule.util.UUID;

import javax.resource.spi.work.WorkListener;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>MuleConfiguration</code> holds the runtime configuration specific to the
 * <code>MuleManager</code>. Once the <code>MuleManager</code> has been
 * initialised this class is immutable.
 * 
 * TODO MULE-2162 MuleConfiguration should be a stateless "view" of info. in the Registry
 */
public class MuleConfiguration
{
    /** logger used by this class */
    protected transient Log logger = LogFactory.getLog(getClass());


    /**
     * Specifies whether mule should process messages sysnchonously, i.e. that a
     * mule-model can only processone message at a time, or asynchonously. The
     * default value is 'false'.
     */
    public static final String SYNCHRONOUS_PROPERTY = "synchronous";

    public static final String DEFAULT_ENCODING = "UTF-8";
    /** Default encoding used in OS running Mule */
    public static final String DEFAULT_OS_ENCODING = System.getProperty("file.encoding");


    /** Default value for SYNCHRONOUS_PROPERTY */
    public static final boolean DEFAULT_SYNCHRONOUS = false;

    /** Default value for MAX_OUTSTANDING_MESSAGES_PROPERTY */

    public static final int DEFAULT_TIMEOUT = 10000;

    public static final int DEFAULT_TRANSACTION_TIMEOUT = 30000;

    public static final String DEFAULT_SYSTEM_MODEL_TYPE = "seda";

    /** Where Mule stores any runtime files to disk */
    public static final String DEFAULT_WORKING_DIRECTORY = "./.mule";

    /** The default queueStore directory for persistence */
    public static final String DEFAULT_QUEUE_STORE = "queuestore";

    /** holds the value for SYNCHRONOUS */
    private boolean synchronous = DEFAULT_SYNCHRONOUS;

    /** The type of model used for the internal system model where system created services are registered */
    private String systemModelType = DEFAULT_SYSTEM_MODEL_TYPE;

    private String encoding = DEFAULT_ENCODING;

    private String osEncoding = DEFAULT_OS_ENCODING;

    /** Names of threading profiles in the registry */
    public static final String DEFAULT_THREADING_PROFILE = "defaultThreadingProfile";
    public static final String DEFAULT_MESSAGE_DISPATCHER_THREADING_PROFILE = "defaultMessageDispatcherThreadingProfile";
    public static final String DEFAULT_MESSAGE_RECEIVER_THREADING_PROFILE = "defaultMessageReceiverThreadingProfile";
    public static final String DEFAULT_COMPONENT_THREADING_PROFILE = "defaultComponentThreadingProfile";

    /**
     * When running sychonously, return events can be received over transports that
     * support ack or replyTo This property determines how long to wait for a receive
     */
    private int synchronousEventTimeout = DEFAULT_TIMEOUT;

    /**
     * The default transaction timeout value used if no specific transaction time out
     * has been set on the transaction config
     */
    private int defaultTransactionTimeout = DEFAULT_TRANSACTION_TIMEOUT;

    /**
     * Determines whether when running synchronously, return events are received
     * before returning the call. i.e. in jms wait for a replyTo. Vm queues do this
     * automatically
     */
    private boolean remoteSync = false;

    /** Where mule will store any runtime files to disk */
    private String workingDirectory;

    /** The configuration resources used to configure the MuleManager instance */
    //private String[] configResources = new String[]{};

    /**
     * Whether the server instance is running in client mode, which means that some
     * services will not be started
     */
    private boolean clientMode = false;

    /** The unique Id for this ManagementContext */
    private String id;

    /** The cluster Id for this ManagementContext */
    private String clusterId;

    /** The Domain Id for this ManagementContext */
    private String domainId;

    /**
     * The default connection Strategy used for a connector when one hasn't been
     * defined for the connector
     */
    private ConnectionStrategy connectionStrategy = new SingleAttemptConnectionStrategy();

    private WorkListener workListener = new DefaultWorkListener();

    public MuleConfiguration()
    {
        super();
        setWorkingDirectory(DEFAULT_WORKING_DIRECTORY);
        setId(UUID.getUUID());
        setDomainId("org.mule");
    }

    /** @return true if the model is running synchronously or false otherwise */
    public boolean isDefaultSynchronousEndpoints()
    {
        return synchronous;
    }

    public void setDefaultSynchronousEndpoints(boolean synchronous)
    {
        this.synchronous = synchronous;
    }

    public ThreadingProfile getDefaultMessageDispatcherThreadingProfile()
    {
        return getThreadingProfile(DEFAULT_MESSAGE_DISPATCHER_THREADING_PROFILE);
    }

    public ThreadingProfile getDefaultMessageReceiverThreadingProfile()
    {
        return getThreadingProfile(DEFAULT_MESSAGE_RECEIVER_THREADING_PROFILE);
    }

    public ThreadingProfile getDefaultComponentThreadingProfile()
    {
        return getThreadingProfile(DEFAULT_COMPONENT_THREADING_PROFILE);
    }

    public ThreadingProfile getDefaultThreadingProfile()
    {
        return getThreadingProfile(DEFAULT_THREADING_PROFILE);
    }

    private ThreadingProfile getThreadingProfile(String name)
    {
        ThreadingProfile tp = (ThreadingProfile) RegistryContext.getRegistry().lookupObject(name);
        if (null != tp)
        {
            return tp;
        }
        else
        {
            // only used in tests, where no registry is present
            return ThreadingProfile.DEFAULT_THREADING_PROFILE;
        }
    }

    public int getDefaultSynchronousEventTimeout()
    {
        return synchronousEventTimeout;
    }

    public void setDefaultSynchronousEventTimeout(int synchronousEventTimeout)
    {
        this.synchronousEventTimeout = synchronousEventTimeout;
    }

    public boolean isDefaultRemoteSync()
    {
        return remoteSync;
    }

    public void setDefaultRemoteSync(boolean remoteSync)
    {
        this.remoteSync = remoteSync;
    }

    public String getWorkingDirectory()
    {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory)
    {
        // fix windows backslashes in absolute paths, convert them to forward ones
        this.workingDirectory = FileUtils.newFile(workingDirectory).getAbsolutePath().replaceAll("\\\\", "/");
        updateApplicationProperty(MuleProperties.MULE_WORKING_DIRECTORY_PROPERTY, this.workingDirectory);
    }

//    public String[] getConfigResources()
//    {
//        return configResources;
//    }
//
//    public void setConfigResources(String[] configResources)
//    {
//        if (configResources != null)
//        {
//            int current = this.configResources.length;
//            String[] newResources = new String[configResources.length + current];
//            System.arraycopy(this.configResources, 0, newResources, 0, current);
//            System.arraycopy(configResources, 0, newResources, current, configResources.length);
//            this.configResources = newResources;
//        }
//        else
//        {
//            this.configResources = configResources;
//        }
//    }


    public int getDefaultTransactionTimeout()
    {
        return defaultTransactionTimeout;
    }

    public void setDefaultTransactionTimeout(int defaultTransactionTimeout)
    {
        this.defaultTransactionTimeout = defaultTransactionTimeout;
    }

    public boolean isClientMode()
    {
        return clientMode;
    }

    /**
     * Returns a clone of the default Connection strategy. The clone ensures that the
     * connection strategy can be manipulated without affecting other connectors
     * using the same strategy
     *
     * @return a clone of the default Connection strategy
     */
    public ConnectionStrategy getDefaultConnectionStrategy()
    {
        try
        {
            return (ConnectionStrategy) BeanUtils.cloneBean(connectionStrategy);
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(CoreMessages.failedToClone("Connection Strategy"), e);
        }
    }

    /**
     * Sets the connection strategy used by all connectors managed in this Mule
     * instance if the connector has no connection strategy specifically set on it.
     *
     * @param connectionStrategy the default strategy to use
     */
    public void setDefaultConnectionStrategy(ConnectionStrategy connectionStrategy)
    {
        this.connectionStrategy = connectionStrategy;
    }

    public String getDefaultEncoding()
    {
        return encoding;
    }

    public void setDefaultEncoding(String encoding)
    {
        if (StringUtils.isEmpty(encoding))
        {
            logger.warn("Cannot set encoding to null or empty String");
            return;
        }
        this.encoding = encoding;
    }

    public String getDefaultOSEncoding()
    {
        return osEncoding;
    }

    public void setDefaultOSEncoding(String osEncoding)
    {
        this.osEncoding = osEncoding;
    }

    public WorkListener getDefaultWorkListener()
    {
        return workListener;
    }

    public void setDefaultWorkListener(WorkListener workListener)
    {
        if (workListener == null)
        {
            throw new NullPointerException("workListener");
        }
        this.workListener = workListener;
    }


    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getClusterId()
    {
        return clusterId;
    }

    public void setClusterId(String clusterId)
    {
        this.clusterId = clusterId;
    }

    public String getDomainId()
    {
        return domainId;
    }

    public void setDomainId(String domainId)
    {
        this.domainId = domainId;
    }

    private void updateApplicationProperty(String name, Object value)
    {
        if (RegistryContext.getRegistry() != null)
        {
            try
            {
                RegistryContext.getRegistry().unregisterObject(name);
            }
            catch (UMOException e)
            {
                //ignore
            }
            try
            {
                RegistryContext.getRegistry().registerObject(name, value);
            }
            catch (RegistrationException e)
            {
                logger.error(e);
            }
        }
    }

    public String getSystemModelType()
    {
        return systemModelType;
    }

    public void setSystemModelType(String systemModelType)
    {
        this.systemModelType = systemModelType;
    }
}
