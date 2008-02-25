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

import org.mule.RegistryContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.agent.Agent;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.DefaultWorkListener;
import org.mule.api.lifecycle.FatalException;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleTransitionResult;
import org.mule.api.transport.ConnectionStrategy;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.SingleAttemptConnectionStrategy;
import org.mule.util.FileUtils;
import org.mule.util.StringMessageUtils;
import org.mule.util.StringUtils;
import org.mule.util.UUID;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import javax.resource.spi.work.WorkListener;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>MuleConfiguration</code> holds the runtime configuration specific to the
 * <code>MuleManager</code>. Once the <code>MuleManager</code> has been
 * initialised this class is immutable.
 */
public class MuleConfiguration implements Initialisable
{
    private static final String DEFAULT_LOG_DIRECTORY = "logs";

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

    /**
     * The type of model used for the internal system model where system created
     * services are registered
     */
    private String systemModelType = DEFAULT_SYSTEM_MODEL_TYPE;

    private String encoding = DEFAULT_ENCODING;

    private String osEncoding = DEFAULT_OS_ENCODING;

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
    private ConfigResource[] configResources = new ConfigResource[]{};

    /**
     * Whether the server instance is running in client mode, which means that some
     * services will not be started
     */
    private boolean clientMode = false;

    /** the unique id for this Mule instance */
    private String id = null;

    /** If this node is part of a cluster then this is the shared cluster Id */
    private String clusterId = null;

    /** The domain name that this instance belongs to. */
    private String domainId = null;

    protected String systemName;
    
    /** the date in milliseconds from when the server was started */
    private long startDate = 0;

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
        startDate = System.currentTimeMillis();
    }

    public LifecycleTransitionResult initialise() throws InitialisationException
    {
        setupIds();
        try
        {
            validateEncoding();
            validateOSEncoding();
            validateXML();
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }        
        return LifecycleTransitionResult.OK;
    }

    protected void setupIds() throws InitialisationException
    {
        if (id == null)
        {
            throw new InitialisationException(CoreMessages.objectIsNull("Instance ID"), this);
        }
        if (clusterId == null)
        {
            clusterId = CoreMessages.notClustered().getMessage();
        }
        if (domainId == null)
        {
            try
            {
                domainId = InetAddress.getLocalHost().getHostName();
            }
            catch (UnknownHostException e)
            {
                throw new InitialisationException(e, this);
            }
        }
        systemName = domainId + "." + clusterId + "." + id;
    }

    protected void validateEncoding() throws FatalException
    {
        String encoding = System.getProperty(MuleProperties.MULE_ENCODING_SYSTEM_PROPERTY);
        if (encoding == null)
        {
            encoding = getDefaultEncoding();
            System.setProperty(MuleProperties.MULE_ENCODING_SYSTEM_PROPERTY, encoding);
        }
        else
        {
            setDefaultEncoding(encoding);
        }
        //Check we have a valid and supported encoding
        if (!Charset.isSupported(getDefaultEncoding()))
        {
            throw new FatalException(CoreMessages.propertyHasInvalidValue("encoding", getDefaultEncoding()), this);
        }
    }

    protected void validateOSEncoding() throws FatalException
    {
        String encoding = System.getProperty(MuleProperties.MULE_OS_ENCODING_SYSTEM_PROPERTY);
        if (encoding == null)
        {
            encoding = getDefaultOSEncoding();
            System.setProperty(MuleProperties.MULE_OS_ENCODING_SYSTEM_PROPERTY, encoding);
        }
        else
        {
            setDefaultOSEncoding(encoding);
        }
        // Check we have a valid and supported encoding
        if (!Charset.isSupported(getDefaultOSEncoding()))
        {
            throw new FatalException(CoreMessages.propertyHasInvalidValue("osEncoding",
                    getDefaultOSEncoding()), this);
        }
    }

    /**
     * Mule needs a proper JAXP implementation and will complain when run with a plain JDK
     * 1.4. Use the supplied launcher or specify a proper JAXP implementation via
     * <code>-Djava.endorsed.dirs</code>. See the following URLs for more information:
     * <ul>
     * <li> {@link http://xerces.apache.org/xerces2-j/faq-general.html#faq-4}
     * <li> {@link http://xml.apache.org/xalan-j/faq.html#faq-N100D6}
     * <li> {@link http://java.sun.com/j2se/1.4.2/docs/guide/standards/}
     * </ul>
     */
    protected void validateXML() throws FatalException
    {
        SAXParserFactory f = SAXParserFactory.newInstance();
        if (f == null || f.getClass().getName().indexOf("crimson") != -1)
        {
            throw new FatalException(CoreMessages.valueIsInvalidFor(f.getClass().getName(),
                "javax.xml.parsers.SAXParserFactory"), this);
        }
    }

    /**
     * Returns a formatted string that is a summary of the configuration of the
     * server. This is the brock of information that gets displayed when the server
     * starts
     *
     * @return a string summary of the server information
     */
    public String getStartSplash()
    {
        String notset = CoreMessages.notSet().getMessage();

        // Mule Version, Timestamp, and Server ID
        List message = new ArrayList();
        Manifest mf = MuleManifest.getManifest();
        Map att = mf.getMainAttributes();
        if (att.values().size() > 0)
        {
            message.add(StringUtils.defaultString(MuleManifest.getProductDescription(), notset));
            message.add(CoreMessages.version().getMessage() + " Build: "
                    + StringUtils.defaultString(MuleManifest.getBuildNumber(), notset));

            message.add(StringUtils.defaultString(MuleManifest.getVendorName(), notset));
            message.add(StringUtils.defaultString(MuleManifest.getProductMoreInfo(), notset));
        }
        else
        {
            message.add(CoreMessages.versionNotSet().getMessage());
        }
        message.add(" ");
        message.add(CoreMessages.serverStartedAt(getStartDate()).getMessage());
        message.add("Server ID: " + id);

        // JDK, OS, and Host
        message.add("JDK: " + System.getProperty("java.version") + " (" + System.getProperty("java.vm.info")
                + ")");
        String patch = System.getProperty("sun.os.patch.level", null);
        message.add("OS: " + System.getProperty("os.name")
                + (patch != null && !"unknown".equalsIgnoreCase(patch) ? " - " + patch : "") + " ("
                + System.getProperty("os.version") + ", " + System.getProperty("os.arch") + ")");
        try
        {
            InetAddress host = InetAddress.getLocalHost();
            message.add("Host: " + host.getHostName() + " (" + host.getHostAddress() + ")");
        }
        catch (UnknownHostException e)
        {
            // ignore
        }

        // Mule Agents
        message.add(" ");
        //List agents
        Collection agents = RegistryContext.getRegistry().lookupObjects(Agent.class);
        if (agents.size() == 0)
        {
            message.add(CoreMessages.agentsRunning().getMessage() + " "
                    + CoreMessages.none().getMessage());
        }
        else
        {
            message.add(CoreMessages.agentsRunning().getMessage());
            Agent umoAgent;
            for (Iterator iterator = agents.iterator(); iterator.hasNext();)
            {
                umoAgent = (Agent) iterator.next();
                message.add("  " + umoAgent.getDescription());
            }
        }
        return StringMessageUtils.getBoilerPlate(message, '*', 70);
    }

    public String getEndSplash()
    {
        List message = new ArrayList(2);
        long currentTime = System.currentTimeMillis();
        message.add(CoreMessages.shutdownNormally(new Date()).getMessage());
        long duration = 10;
        if (startDate > 0)
        {
            duration = currentTime - startDate;
        }
        message.add(CoreMessages.serverWasUpForDuration(duration).getMessage());

        return StringMessageUtils.getBoilerPlate(message, '*', 78);
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

    public String getMuleHomeDirectory()
    {
        return System.getProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY);
    }

    public String getLogDirectory()
    {
        return getMuleHomeDirectory() + File.separator + DEFAULT_LOG_DIRECTORY;
    }

    public void setWorkingDirectory(String workingDirectory)
    {
        // fix windows backslashes in absolute paths, convert them to forward ones
        this.workingDirectory = FileUtils.newFile(workingDirectory).getAbsolutePath().replaceAll("\\\\", "/");
    }

    public ConfigResource[] getConfigResources()
    {
        return configResources;
    }

    public void setConfigResources(ConfigResource[] configResources)
    {
        if (configResources != null)
        {
            int current = this.configResources.length;
            ConfigResource[] newResources = new ConfigResource[configResources.length + current];
            System.arraycopy(this.configResources, 0, newResources, 0, current);
            System.arraycopy(configResources, 0, newResources, current, configResources.length);
            this.configResources = newResources;
        }
        else
        {
            this.configResources = configResources;
        }
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
        if (StringUtils.isBlank(id))
        {
            throw new RuntimeException("Cannot set server id to null/blank");
        }
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

    public String getSystemModelType()
    {
        return systemModelType;
    }

    public void setSystemModelType(String systemModelType)
    {
        this.systemModelType = systemModelType;
    }

    public void setClientMode(boolean clientMode)
    {
        this.clientMode = clientMode;
    }

    public String getSystemName()
    {
        return systemName;
    }

    public void setSystemName(String systemName)
    {
        this.systemName = systemName;
    }

    /**
     * Returns the long date when the server was started
     *
     * @return the long date when the server was started
     */
    public long getStartDate()
    {
        return startDate;
    }
}
