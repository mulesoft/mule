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

package org.mule.config;

import org.mule.util.ClassHelper;
import org.mule.util.queue.SerialisationPersistence;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * <code>MuleConfiguration</code> holds the runtime configuration specific to the
 * <code>MuleManager</code>. Once the <code>MuleManager</code> has been initialised
 * this class is immutable.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleConfiguration
{
    /**
     * The default serverUrl used to receive incoming requests from clients
     */
    public static final String DEFAULT_SERVER_URL = "tcp://localhost:60504";

    /**
     * Specifies that the transformer properties should be obtained from the Mule
     * Manager properties
     */
    public static final String USE_MANAGER_PROPERTIES = "org.mule.useManagerProperties";

    /**
     * Specifies whether mule should process messages sysnchonously, i.e. that a mule-model
     * can only processone message at a time, or asynchonously.  The default value is
     * 'false'.
     */
    public static final String SYNCHRONOUS_PROPERTY = "synchronous";
//    /**
//     * Determines the default inboundProvider that Mule uses to communicate between MuelUMO's when an
//     * inboundProvider is not specified on a mule.
//     * If this value is not specifed, Mule will create a default VM connection
//     * called 'muleVMInboundProvider' which will use a VMConnector.
//     */
//    public static final String DEFAULT_INBOUND_PROVIDER_PROPERTY = "defaultInboundProvider";
//
//    /**
//     * Determines the default outboundProvider that Mule uses to communicate between MuelUMO's when an
//     * outboundProvider is not specified on a mule.
//     * If this value is not specifed, Mule will create a default VM connection
//     * called 'muleVMOutbound' which will use a VMConnector.
//     */
//    public static final String DEFAULT_OUTBOUND_PROVIDER_PROPERTY = "defaultOutboundProvider";

    /**
     * Default value for SYNCHRONOUS_PROPERTY
     */
    public static final boolean DEFAULT_SYNCHRONOUS = false;

    /**
     * Default value for MAX_OUTSTANDING_MESSAGES_PROPERTY
     */
    public static final int DEFAULT_MAX_OUTSTANDING_MESSAGES = 1000;

    public static final int DEFAULT_SYNCHRONOUS_EVENT_TIMEOUT = 5000;

    public static final int DEFAULT_TRANSACTION_TIMEOUT = 30000;

    /**
     * Where Mule stores any runtime files to disk
     */
    public static final String DEFAULT_WORKING_DIRECTORY = "./.mule";
    /**
     * holds the value for SYNCHRONOUS
     */
    private boolean synchronous = DEFAULT_SYNCHRONOUS;

    /**
     * Name of the model to use. If blank the first model will be used
     */
    private String model = null;

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

    private QueueProfile queueProfile = new QueueProfile(DEFAULT_MAX_OUTSTANDING_MESSAGES, null);

    /**
     * When running sychonously, return events can be received over transports that support ack or replyTo
     * This property determines how long to wait for a receive
     */
    private int synchronousEventTimeout = DEFAULT_SYNCHRONOUS_EVENT_TIMEOUT;

    /**
     * The default transaction timeout value used if no specific transaction time out has been
     * set on the transaction config
     */
    private int transactionTimeout = DEFAULT_TRANSACTION_TIMEOUT;

    /**
     * Determines whether when running synchronously, return events are received before
     * returning the call. i.e. in jms wait for a replyTo. Vm queues do this automatically
     */
    private boolean synchronousReceive = false;

    /**
     * Determines whether internal vm queues are persistent.  If they are, if the server dies
     * unexpectedly it can resume it's current state and continue processing
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
    private String workingDirectoy = DEFAULT_WORKING_DIRECTORY;

    /**
     * The configuration resources used to configure the MuleManager instance
     */
    private String[] configResources = new String[]{};

    /**
     * This is the url used by the server itself to recieve incomming requests.
     * This enables clients such as the Mule Client to marshal remote requests to
     * a MuleManager instance. The default value is tcp://localhost:61616
     */
    private String serverUrl = DEFAULT_SERVER_URL;

    private Manifest manifest = null;

    private boolean clientMode = false;

    public MuleConfiguration()
    {

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
        if(defaultThreadingProfile==null) return;
        this.defaultThreadingProfile = defaultThreadingProfile;
    }

    private ThreadingProfile getThreadingProfile(ThreadingProfile profile) {
        if(profile != null) return new ThreadingProfile((ThreadingProfile)profile);
        return new ThreadingProfile((ThreadingProfile)defaultThreadingProfile);
    }

    public PoolingProfile getPoolingProfile()
    {
        return new PoolingProfile((PoolingProfile)poolingProfile);
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

    public boolean isSynchronousReceive()
    {
        return synchronousReceive;
    }

    public void setSynchronousReceive(boolean synchronousReceive)
    {
        this.synchronousReceive = synchronousReceive;
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
        if(recoverableMode && queueProfile.getPersistenceStrategy() == null) {
            queueProfile = new QueueProfile(queueProfile.getMaxOutstandingMessages(), new SerialisationPersistence());
        }
    }

    public String getWorkingDirectoy()
    {
        return workingDirectoy;
    }

    public void setWorkingDirectory(String workingDirectory)
    {
        this.workingDirectoy = workingDirectory;
    }

    public String[] getConfigResources()
    {
        return configResources;
    }

    public void setConfigResources(String[] configResources)
    {
        if(configResources!=null) {
            int current = this.configResources.length;
            String newResources[] = new String[configResources.length + current];
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
        this.serverUrl = serverUrl;
    }

    public String getProductVersion() {
        return getManifestProperty("Implementation-Version");
    }

    public String getVendorName() {
        return getManifestProperty("Specification-Vendor");
    }

    public String getProductName() {
        return getManifestProperty("Implementation-Title");
    }

    public Manifest getManifest() {

        if(manifest==null) {
            manifest = new Manifest();
            InputStream is = ClassHelper.getResourceAsStream("META-INF/Mule.mf", getClass());
            //a bit of a kludge as depending on how the jar is build the Meta-inf can be lower or upper case...
            if(is==null) {
                is = ClassHelper.getResourceAsStream("meta-inf/Mule.mf", getClass());
            }

            if(is!=null) {
                try
                {
                    manifest.read(is);
                } catch (IOException e)
                {
                    //ignore
                }
            }
        }
        return manifest;
    }

    protected String getManifestProperty(String name) {
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

    public boolean isClientMode() {
        return clientMode;
    }

    public void setClientMode(boolean clientMode) {
        this.clientMode = clientMode;
        setServerUrl("");
    }
}
