/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleRuntimeException;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.providers.ConnectionStrategy;
import org.mule.providers.SingleAttemptConnectionStrategy;
import org.mule.umo.manager.DefaultWorkListener;
import org.mule.util.queue.EventFilePersistenceStrategy;
import org.mule.util.queue.QueuePersistenceStrategy;

import javax.resource.spi.work.WorkListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.Enumeration;

/**
 * <code>MuleConfiguration</code> holds the runtime configuration specific to
 * the <code>MuleManager</code>. Once the <code>MuleManager</code> has been
 * initialised this class is immutable.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleConfiguration
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    /**
     * The default serverUrl used to receive incoming requests from clients
     */
    public static final String DEFAULT_SERVER_URL = "tcp://localhost:60504";

    /**
     * Specifies that the transformer properties should be obtained from the
     * Mule Manager properties
     */
    public static final String USE_MANAGER_PROPERTIES = "org.mule.useManagerProperties";

    /**
     * Specifies whether mule should process messages sysnchonously, i.e. that a
     * mule-model can only processone message at a time, or asynchonously. The
     * default value is 'false'.
     */
    public static final String SYNCHRONOUS_PROPERTY = "synchronous";

    public static final String DEFAULT_ENCODING = "UTF-8";
    /**
     * Default encoding used in OS running Mule
     */
    public static final String DEFAULT_OS_ENCODING = System.getProperty("file.encoding");

    // /**
    // * Determines the default inboundProvider that Mule uses to communicate
    // between MuelUMO's when an
    // * inboundProvider is not specified on a mule.
    // * If this value is not specifed, Mule will create a default VM connection
    // * called 'muleVMInboundProvider' which will use a VMConnector.
    // */
    // public static final String DEFAULT_INBOUND_PROVIDER_PROPERTY =
    // "defaultInboundProvider";
    //
    // /**
    // * Determines the default outboundProvider that Mule uses to communicate
    // between MuelUMO's when an
    // * outboundProvider is not specified on a mule.
    // * If this value is not specifed, Mule will create a default VM connection
    // * called 'muleVMOutbound' which will use a VMConnector.
    // */
    // public static final String DEFAULT_OUTBOUND_PROVIDER_PROPERTY =
    // "defaultOutboundProvider";

    /**
     * Default value for SYNCHRONOUS_PROPERTY
     */
    public static final boolean DEFAULT_SYNCHRONOUS = false;

    /**
     * Default value for MAX_OUTSTANDING_MESSAGES_PROPERTY
     */
    public static final int DEFAULT_MAX_OUTSTANDING_MESSAGES = 1000;

    public static final int DEFAULT_TIMEOUT = 10000;

    public static final int DEFAULT_TRANSACTION_TIMEOUT = 30000;

    /**
     * Where Mule stores any runtime files to disk
     */
    public static final String DEFAULT_WORKING_DIRECTORY = "./.mule";

    /**
     * The default queueStore directory for persistence
     */
    public static final String DEFAULT_QUEUE_STORE = "queuestore";

    /**
     * holds the value for SYNCHRONOUS
     */
    private boolean synchronous = DEFAULT_SYNCHRONOUS;

    /**
     * Whether Mule should fire message events for every message sent and received
     */
    private boolean enableMessageEvents = false;

    /**
     * Name of the model to use. If blank the first model will be used
     */
    private String model = null;

    private String encoding = DEFAULT_ENCODING;

    private String osEncoding = DEFAULT_OS_ENCODING;

    private PoolingProfile poolingProfile = new PoolingProfile();

    /**
     * configuration for the threadpool used by message dispatchers
     */
    private ThreadingProfile messageDispatcherThreadingProfile = null;

    /**
     * configuration for the threadpool used by message receivers
     */
    private ThreadingProfile messageReceiverThreadingProfile = null;

    /**
     * configuration for the threadpool used by component pooling in mule
     */
    private ThreadingProfile componentPoolThreadingProfile = null;

    private QueueProfile queueProfile = new QueueProfile(DEFAULT_MAX_OUTSTANDING_MESSAGES, false);

    private QueuePersistenceStrategy persistenceStrategy = new EventFilePersistenceStrategy();

    /**
     * When running sychonously, return events can be received over transports
     * that support ack or replyTo This property determines how long to wait for
     * a receive
     */
    private int synchronousEventTimeout = DEFAULT_TIMEOUT;

    /**
     * The default transaction timeout value used if no specific transaction
     * time out has been set on the transaction config
     */
    private int transactionTimeout = DEFAULT_TRANSACTION_TIMEOUT;

    /**
     * Determines whether when running synchronously, return events are received
     * before returning the call. i.e. in jms wait for a replyTo. Vm queues do
     * this automatically
     */
    private boolean remoteSync = false;

    /**
     * Determines whether internal vm queues are persistent. If they are, if the
     * server dies unexpectedly it can resume it's current state and continue
     * processing
     */
    private boolean recoverableMode = false;
    /**
     * A helper thread pool configuration that is used for all other thread pool
     * configurations
     */
    private ThreadingProfile defaultThreadingProfile = new ThreadingProfile();

    /**
     * Where mule will store any runtime files to disk
     */
    private String workingDirectory = DEFAULT_WORKING_DIRECTORY;

    /**
     * The configuration resources used to configure the MuleManager instance
     */
    private String[] configResources = new String[] {};

    /**
     * This is the url used by the server itself to receive incomming requests.
     * This enables clients such as the Mule Client to marshal remote requests
     * to a MuleManager instance. The default value is tcp://localhost:61616
     */
    private String serverUrl = DEFAULT_SERVER_URL;

    /**
     * The Mule Jar manifest object
     */
    private Manifest manifest = null;

    /**
     * Whether the server instance is running in client mode, which means that
     * some services will not be started
     */
    private boolean clientMode = false;

    /**
     * Whether the server is embedded by another framework and certain stand-alone
     * features
     */
    private boolean embedded = false;

    /**
     * The model type to use for component invocations
     */
    private String modelType = "default";

    /**
     * The default connection Strategy used for a connector when one hasn't been
     * defined for the connector
     */
    private ConnectionStrategy connectionStrategy = new SingleAttemptConnectionStrategy();

    private WorkListener workListener = new DefaultWorkListener();

    public MuleConfiguration()
    {
        super();
    }

    /**
     * @return true if the model is running synchronously or false otherwise
     */
    public boolean isSynchronous()
    {
        return synchronous;
    }

    public void setSynchronous(boolean synchronous)
    {
        this.synchronous = synchronous;
    }

    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }

    public ThreadingProfile getMessageDispatcherThreadingProfile()
    {
        return getThreadingProfile(messageDispatcherThreadingProfile);
    }

    public void setMessageDispatcherThreadingProfile(ThreadingProfile messageDispatcherThreadingProfile)
    {
        this.messageDispatcherThreadingProfile = messageDispatcherThreadingProfile;
    }

    public ThreadingProfile getMessageReceiverThreadingProfile()
    {
        return getThreadingProfile(messageReceiverThreadingProfile);
    }

    public void setMessageReceiverThreadingProfile(ThreadingProfile messageReceiverThreadingProfile)
    {
        this.messageReceiverThreadingProfile = messageReceiverThreadingProfile;
    }

    public ThreadingProfile getComponentThreadingProfile()
    {
        return getThreadingProfile(componentPoolThreadingProfile);
    }

    public void setComponentThreadingProfile(ThreadingProfile componentPoolThreadingProfile)
    {
        this.componentPoolThreadingProfile = componentPoolThreadingProfile;
    }

    public ThreadingProfile getDefaultThreadingProfile()
    {
        return getThreadingProfile(defaultThreadingProfile);
    }

    public void setDefaultThreadingProfile(ThreadingProfile defaultThreadingProfile)
    {
        if (defaultThreadingProfile == null) {
            return;
        }
        this.defaultThreadingProfile = defaultThreadingProfile;
    }

    private ThreadingProfile getThreadingProfile(ThreadingProfile profile)
    {
        if (profile != null) {
            return new ThreadingProfile(profile);
        }
        return new ThreadingProfile(defaultThreadingProfile);
    }

    public PoolingProfile getPoolingProfile()
    {
        return new PoolingProfile(poolingProfile);
    }

    public void setPoolingProfile(PoolingProfile poolingProfile)
    {
        this.poolingProfile = poolingProfile;
    }

    public int getSynchronousEventTimeout()
    {
        return synchronousEventTimeout;
    }

    public void setSynchronousEventTimeout(int synchronousEventTimeout)
    {
        this.synchronousEventTimeout = synchronousEventTimeout;
    }

    public boolean isRemoteSync()
    {
        return remoteSync;
    }

    public void setRemoteSync(boolean remoteSync)
    {
        this.remoteSync = remoteSync;
    }

    public QueueProfile getQueueProfile()
    {
        return new QueueProfile(queueProfile);
    }

    public void setQueueProfile(QueueProfile queueProfile)
    {
        this.queueProfile = queueProfile;
    }

    public boolean isRecoverableMode()
    {
        return recoverableMode;
    }

    public void setRecoverableMode(boolean recoverableMode)
    {
        this.recoverableMode = recoverableMode;
        if (recoverableMode) {
            queueProfile.setPersistent(true);
        }
    }

    public String getWorkingDirectory()
    {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory)
    {
        this.workingDirectory = workingDirectory;
    }

    public String[] getConfigResources()
    {
        return configResources;
    }

    public void setConfigResources(String[] configResources)
    {
        if (configResources != null) {
            int current = this.configResources.length;
            String[] newResources = new String[configResources.length + current];
            System.arraycopy(this.configResources, 0, newResources, 0, current);
            System.arraycopy(configResources, 0, newResources, current, configResources.length);
            this.configResources = newResources;
        } else {
            this.configResources = configResources;
        }
    }

    public String getServerUrl()
    {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl)
    {
        if(embedded) {
            serverUrl = null;
        } else {
            this.serverUrl = serverUrl;
        }
    }

    public String getProductVersion()
    {
        return getManifestProperty("Implementation-Version");
    }

    public String getVendorName()
    {
        return getManifestProperty("Specification-Vendor");
    }

    public String getVendorUrl()
    {
        return getManifestProperty("Vendor-Url");
    }

    public String getProductUrl()
    {
        return getManifestProperty("Product-Url");
    }

    public String getProductName()
    {
        return getManifestProperty("Implementation-Title");
    }

    public String getProductMoreInfo()
    {
        return getManifestProperty("More-Info");
    }

    public String getProductSupport()
    {
        return getManifestProperty("Support");
    }

    public String getProductLicenseInfo()
    {
        return getManifestProperty("License");
    }

    public String getProductDescription()
    {
        return getManifestProperty("Description");
    }

    public String getBuildDate()
    {
        return getManifestProperty("Build-Date");
    }

    public Manifest getManifest()
    {
        if (manifest == null) {
            manifest = new Manifest();

            InputStream is = null;
            try {
                //We want to load the MANIFEST.MF from the mule-core jar. Sine we don't the version we're using
                //we have to search for the jar on the classpath
                URL url = (URL) AccessController.doPrivileged(new PrivilegedAction()
                {
                    public Object run()
                    {
                        try
                        {
                            Enumeration e = MuleConfiguration.class.getClassLoader().getResources(("META-INF/MANIFEST.MF"));
                            while(e.hasMoreElements()) {
                                URL url = (URL)e.nextElement();
                                if(url.toExternalForm().indexOf("mule-core") > -1) {
                                    return url;
                                }
                            }
                        } catch (IOException e1)
                        {
                            e1.printStackTrace();
                        }
                        return null;
                    }
                });

                if(url !=null)
                {
                    is = url.openStream();
                }


                if (is != null) { manifest.read(is); }

            } catch (IOException e) {
                logger.warn("Failed to read manifest Info, Manifest information will not display correctly: " + e.getMessage());
            }
        }
        return manifest;
    }

    protected String getManifestProperty(String name)
    {
        return getManifest().getMainAttributes().getValue(new Attributes.Name(name));
    }

    public int getTransactionTimeout()
    {
        return transactionTimeout;
    }

    public void setTransactionTimeout(int transactionTimeout)
    {
        this.transactionTimeout = transactionTimeout;
    }

    public boolean isClientMode()
    {
        return clientMode;
    }

    public void setClientMode(boolean clientMode)
    {
        this.clientMode = clientMode;
        if(clientMode) {
            setServerUrl("");
        }
    }

    public QueuePersistenceStrategy getPersistenceStrategy()
    {
        return persistenceStrategy;
    }

    public void setPersistenceStrategy(QueuePersistenceStrategy persistenceStrategy)
    {
        this.persistenceStrategy = persistenceStrategy;
    }

    /**
     * Returns a clone of the default Connection strategy. The clone ensures
     * that the connection strategy can be manipulated without affecting other
     * connectors using the same strategy
     *
     * @return a clone of the default Connection strategy
     */
    public ConnectionStrategy getConnectionStrategy()
    {
        try {
            return (ConnectionStrategy) BeanUtils.cloneBean(connectionStrategy);
        } catch (Exception e) {
            throw new MuleRuntimeException(new Message(Messages.FAILED_TO_CLONE_X, "Connection Strategy"), e);
        }
    }

    /**
     * Sets the connection strategy used by all connectors managed in this Mule
     * instance if the connector has no connection strategy specifically set on
     * it.
     *
     * @param connectionStrategy the default strategy to use
     */
    public void setConnectionStrategy(ConnectionStrategy connectionStrategy)
    {
        this.connectionStrategy = connectionStrategy;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public void setEmbedded(boolean embedded) {
        this.embedded = embedded;
        if(embedded) {
            serverUrl=null;
        }
    }

    public String getModelType() {
        return modelType;
    }

    public void setModelType(String modelType) {
        this.modelType = modelType;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        if (StringUtils.isEmpty(encoding)) {
            logger.warn("Cannot set encoding to null or empty String");
            return;
        }
        this.encoding = encoding;
    }

    public String getOSEncoding() {
        return osEncoding;
    }

    public void setOSEncoding(String osEncoding) {
        this.osEncoding = osEncoding;
    }

    public boolean isEnableMessageEvents() {
        return enableMessageEvents;
    }

    public void setEnableMessageEvents(boolean enableMessageEvents) {
        this.enableMessageEvents = enableMessageEvents;
    }

    public WorkListener getWorkListener() {
        return workListener;
    }

    public void setWorkListener(WorkListener workListener) {
        if(workListener==null) {
            throw new NullPointerException("workListener");
        }
        this.workListener = workListener;
    }
}
