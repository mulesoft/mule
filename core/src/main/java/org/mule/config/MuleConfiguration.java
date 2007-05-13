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

import org.mule.MuleRuntimeException;
import org.mule.RegistryContext;
import org.mule.config.i18n.CoreMessages;
import org.mule.providers.ConnectionStrategy;
import org.mule.providers.SingleAttemptConnectionStrategy;
import org.mule.umo.manager.DefaultWorkListener;
import org.mule.util.UUID;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.resource.spi.work.WorkListener;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>MuleConfiguration</code> holds the runtime configuration specific to the
 * <code>MuleManager</code>. Once the <code>MuleManager</code> has been
 * initialised this class is immutable.
 */
public class MuleConfiguration
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());


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


    /**
     * Default value for SYNCHRONOUS_PROPERTY
     */
    public static final boolean DEFAULT_SYNCHRONOUS = false;

    /**
     * Default value for MAX_OUTSTANDING_MESSAGES_PROPERTY
     */

    public static final int DEFAULT_TIMEOUT = 10000;

    public static final int DEFAULT_TRANSACTION_TIMEOUT = 30000;

    public static final String DEFAULT_SYSTEM_MODEL_TYPE = "seda";

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
     * The type of model used for the internal system model where system created services are registered
     */
    private String systemModelType = DEFAULT_SYSTEM_MODEL_TYPE;

    private String encoding = DEFAULT_ENCODING;

    private String osEncoding = DEFAULT_OS_ENCODING;

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

    /**
     * A helper thread pool configuration that is used for all other thread pool
     * configurations
     */
    private ThreadingProfile defaultThreadingProfile = new ThreadingProfile();

    /**
     * Where mule will store any runtime files to disk
     */
    private String workingDirectory;

    /**
     * The configuration resources used to configure the MuleManager instance
     */
    private String[] configResources = new String[]{};

    /**
     * The Mule Jar manifest object
     */
    private Manifest manifest = null;

    /**
     * Whether the server instance is running in client mode, which means that some
     * services will not be started
     */
    private boolean clientMode = false;

    /**
     * The unique Id for this ManagementContext
     */
    private String id;

    /**
     * The cluster Id for this ManagementContext
     */
    private String clusterId;

    /**
     * The Domain Id for this ManagementContext
     */
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

    /**
     * @return true if the model is running synchronously or false otherwise
     */
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
        return getThreadingProfile(messageDispatcherThreadingProfile);
    }

    public void setDefaultMessageDispatcherThreadingProfile(ThreadingProfile messageDispatcherThreadingProfile)
    {
        this.messageDispatcherThreadingProfile = messageDispatcherThreadingProfile;
    }

    public ThreadingProfile getDefaultMessageReceiverThreadingProfile()
    {
        return getThreadingProfile(messageReceiverThreadingProfile);
    }

    public void setDefaultMessageReceiverThreadingProfile(ThreadingProfile messageReceiverThreadingProfile)
    {
        this.messageReceiverThreadingProfile = messageReceiverThreadingProfile;
    }

    public ThreadingProfile getDefaultComponentThreadingProfile()
    {
        return getThreadingProfile(componentPoolThreadingProfile);
    }

    public void setDefaultComponentThreadingProfile(ThreadingProfile componentPoolThreadingProfile)
    {
        this.componentPoolThreadingProfile = componentPoolThreadingProfile;
    }

    public ThreadingProfile getDefaultThreadingProfile()
    {
        return getThreadingProfile(defaultThreadingProfile);
    }

    public void setDefaultThreadingProfile(ThreadingProfile defaultThreadingProfile)
    {
        if (defaultThreadingProfile == null)
        {
            return;
        }
        this.defaultThreadingProfile = defaultThreadingProfile;
    }

    private ThreadingProfile getThreadingProfile(ThreadingProfile profile)
    {
        if (profile != null)
        {
            return new ThreadingProfile(profile);
        }
        return new ThreadingProfile(defaultThreadingProfile);
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
        this.workingDirectory = new File(workingDirectory).getAbsolutePath().replaceAll("\\\\", "/");
        updateApplicationProperty(MuleProperties.MULE_WORKING_DIRECTORY_PROPERTY, this.workingDirectory);
    }

    public String[] getConfigResources()
    {
        return configResources;
    }

    public void setConfigResources(String[] configResources)
    {
        if (configResources != null)
        {
            int current = this.configResources.length;
            String[] newResources = new String[configResources.length + current];
            System.arraycopy(this.configResources, 0, newResources, 0, current);
            System.arraycopy(configResources, 0, newResources, current, configResources.length);
            this.configResources = newResources;
        }
        else
        {
            this.configResources = configResources;
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
        if (manifest == null)
        {
            manifest = new Manifest();

            InputStream is = null;
            try
            {
                // We want to load the MANIFEST.MF from the mule-core jar. Sine we
                // don't the version we're using
                // we have to search for the jar on the classpath
                URL url = (URL) AccessController.doPrivileged(new PrivilegedAction()
                {
                    public Object run()
                    {
                        try
                        {
                            Enumeration e = MuleConfiguration.class.getClassLoader().getResources(
                                ("META-INF/MANIFEST.MF"));
                            while (e.hasMoreElements())
                            {
                                URL url = (URL) e.nextElement();
                                if (url.toExternalForm().indexOf("mule-core") > -1)
                                {
                                    return url;
                                }
                            }
                        }
                        catch (IOException e1)
                        {
                            // TODO MULE-863: Is this sufficient (was printStackTrace) and correct?
                            logger.debug("Failure reading manifest: " + e1.getMessage(), e1);
                        }
                        return null;
                    }
                });

                if (url != null)
                {
                    is = url.openStream();
                }

                if (is != null)
                {
                    manifest.read(is);
                }

            }
            catch (IOException e)
            {
                // TODO MULE-863
                logger.warn("Failed to read manifest Info, Manifest information will not display correctly: "
                            + e.getMessage());
            }
        }
        return manifest;
    }

    protected String getManifestProperty(String name)
    {
        return getManifest().getMainAttributes().getValue(new Attributes.Name(name));
    }

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
        if(RegistryContext.getRegistry()!=null)
        {
            RegistryContext.getRegistry().registerProperty(name, value);
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
