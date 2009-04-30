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

import org.mule.MuleServer;
import org.mule.api.MuleContext;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.config.MuleProperties;
import org.mule.api.lifecycle.FatalException;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.FileUtils;
import org.mule.util.NumberUtils;
import org.mule.util.StringUtils;
import org.mule.util.UUID;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Configuration info. which can be set when creating the MuleContext but becomes
 * immutable after starting the MuleContext.
 */
public class DefaultMuleConfiguration implements MuleConfiguration 
{
    private boolean synchronous = false;

    /**
     * The type of model used for the internal system model where system created
     * services are registered
     */
    private String systemModelType = "seda";

    private String encoding = "UTF-8";

    /**
     * When running sychonously, return events can be received over transports that
     * support ack or replyTo This property determines how long to wait for a receive
     */
    private int responseTimeout = 10000;

    /**
     * The default transaction timeout value used if no specific transaction time out
     * has been set on the transaction config
     */
    private int defaultTransactionTimeout = 30000;

    
    /** Where Mule stores any runtime files to disk */
    private String workingDirectory = "./.mule";

    /**
     * Whether the server instance is running in client mode, which means that some
     * services will not be started
     */
    private boolean clientMode = false;

    /** the unique id for this Mule instance */
    private String id;

    /** If this node is part of a cluster then this is the shared cluster Id */
    private String clusterId;

    /** The domain name that this instance belongs to. */
    private String domainId;

    // Debug options
    
    private boolean cacheMessageAsBytes = true;

    private boolean cacheMessageOriginalPayload = true;

    private boolean enableStreaming = true;

    private boolean autoWrapMessageAwareTransform = true;
    
    protected transient Log logger = LogFactory.getLog(DefaultMuleConfiguration.class);

    public DefaultMuleConfiguration()  
    {
        // Apply any settings which come from the JVM system properties.
        applySystemProperties();
        
        if (id == null)
        {
            id = UUID.getUUID();
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
                logger.warn(e);
                domainId = "org.mule";
            }
        }
        
        try
        {
            validateEncoding();
            validateXML();
        }
        catch (FatalException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Apply any settings which come from the JVM system properties.
     */
    protected void applySystemProperties() 
    {
        String p;
        
        p = System.getProperty(SYSTEM_PROPERTY_PREFIX + "encoding");
        if (p != null)
        {
            encoding = p;
        }
        p = System.getProperty(SYSTEM_PROPERTY_PREFIX + "endpoints.synchronous");
        if (p != null)
        {
            synchronous = BooleanUtils.toBoolean(p);
        }
        p = System.getProperty(SYSTEM_PROPERTY_PREFIX + "systemModelType");
        if (p != null)
        {
            systemModelType = p;
        }
        p = System.getProperty(SYSTEM_PROPERTY_PREFIX + "timeout.synchronous");
        if (p != null)
        {
            responseTimeout = NumberUtils.toInt(p);
        }
        p = System.getProperty(SYSTEM_PROPERTY_PREFIX + "timeout.transaction");
        if (p != null)
        {
            defaultTransactionTimeout = NumberUtils.toInt(p);
        }

        p = System.getProperty(SYSTEM_PROPERTY_PREFIX + "workingDirectory");
        if (p != null)
        {
            workingDirectory = p;
        }
        p = System.getProperty(SYSTEM_PROPERTY_PREFIX + "clientMode");
        if (p != null)
        {
            clientMode = BooleanUtils.toBoolean(p);
        }
        p = System.getProperty(SYSTEM_PROPERTY_PREFIX + "serverId");
        if (p != null)
        {
            id = p;
        }
        p = System.getProperty(SYSTEM_PROPERTY_PREFIX + "clusterId");
        if (p != null)
        {
            clusterId = p;
        }
        p = System.getProperty(SYSTEM_PROPERTY_PREFIX + "domainId");
        if (p != null)
        {
            domainId = p;
        }
        p = System.getProperty(SYSTEM_PROPERTY_PREFIX + "message.cacheBytes");
        if (p != null)
        {
            cacheMessageAsBytes = BooleanUtils.toBoolean(p);
        }
        p = System.getProperty(SYSTEM_PROPERTY_PREFIX + "message.cacheOriginal");
        if (p != null)
        {
            cacheMessageOriginalPayload = BooleanUtils.toBoolean(p);
        }
        p = System.getProperty(SYSTEM_PROPERTY_PREFIX + "streaming.enable");
        if (p != null)
        {
            enableStreaming = BooleanUtils.toBoolean(p);
        }
        p = System.getProperty(SYSTEM_PROPERTY_PREFIX + "transform.autoWrap");
        if (p != null)
        {
            autoWrapMessageAwareTransform = BooleanUtils.toBoolean(p);
        }
    }

    protected void validateEncoding() throws FatalException
    {
        //Check we have a valid and supported encoding
        if (!Charset.isSupported(encoding))
        {
            throw new FatalException(CoreMessages.propertyHasInvalidValue("encoding", encoding), this);
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

    public boolean isDefaultSynchronousEndpoints()
    {
        return synchronous;
    }

    public void setDefaultSynchronousEndpoints(boolean synchronous)
    {
        if (verifyContextNotStarted())
        {
            this.synchronous = synchronous;
        }
    }

    public int getDefaultResponseTimeout()
    {
        return responseTimeout;
    }

    public void setDefaultResponseTimeout(int responseTimeout)
    {
        if (verifyContextNotStarted())
        {
            this.responseTimeout = responseTimeout;
        }
    }

    public String getWorkingDirectory()
    {
        return workingDirectory;
    }

    public String getMuleHomeDirectory()
    {
        return System.getProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY);
    }

    public void setWorkingDirectory(String workingDirectory)
    {
        if (verifyContextNotInitialized())
        {
            // fix windows backslashes in absolute paths, convert them to forward ones
            this.workingDirectory = FileUtils.newFile(workingDirectory).getAbsolutePath().replaceAll("\\\\", "/");
        }
    }

    public int getDefaultTransactionTimeout()
    {
        return defaultTransactionTimeout;
    }

    public void setDefaultTransactionTimeout(int defaultTransactionTimeout)
    {
        if (verifyContextNotStarted())
        {
            this.defaultTransactionTimeout = defaultTransactionTimeout;
        }
    }

    public boolean isClientMode()
    {
        return clientMode;
    }

    public String getDefaultEncoding()
    {
        return encoding;
    }

    public void setDefaultEncoding(String encoding)
    {
        if (verifyContextNotInitialized())
        {
            this.encoding = encoding;
        }
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        if (verifyContextNotInitialized())
        {
            if (StringUtils.isBlank(id))
            {
                throw new IllegalArgumentException("Cannot set server id to null/blank");
            }
            this.id = id;
        }
    }

    public String getClusterId()
    {
        return clusterId;
    }

    public void setClusterId(String clusterId)
    {
        if (verifyContextNotInitialized())
        {
            this.clusterId = clusterId;
        }
    }

    public String getDomainId()
    {
        return domainId;
    }

    public void setDomainId(String domainId)
    {
        if (verifyContextNotInitialized())
        {
            this.domainId = domainId;
        }
    }

    public String getSystemModelType()
    {
        return systemModelType;
    }

    public void setSystemModelType(String systemModelType)
    {
        if (verifyContextNotStarted())
        {
            this.systemModelType = systemModelType;
        }
    }

    public void setClientMode(boolean clientMode)
    {
        if (verifyContextNotStarted())
        {
            this.clientMode = clientMode;
        }
    }

    public String getSystemName()
    {
        return domainId + "." + clusterId + "." + id;
    }
    
    public boolean isAutoWrapMessageAwareTransform()
    {
        return autoWrapMessageAwareTransform;
    }

    public void setAutoWrapMessageAwareTransform(boolean autoWrapMessageAwareTransform)
    {
        if (verifyContextNotStarted())
        {
            this.autoWrapMessageAwareTransform = autoWrapMessageAwareTransform;
        }
    }

    public boolean isCacheMessageAsBytes()
    {
        return cacheMessageAsBytes;
    }

    public void setCacheMessageAsBytes(boolean cacheMessageAsBytes)
    {
        if (verifyContextNotStarted())
        {
            this.cacheMessageAsBytes = cacheMessageAsBytes;
        }
    }

    public boolean isCacheMessageOriginalPayload()
    {
        return cacheMessageOriginalPayload;
    }

    public void setCacheMessageOriginalPayload(boolean cacheMessageOriginalPayload)
    {
        if (verifyContextNotStarted())
        {
            this.cacheMessageOriginalPayload = cacheMessageOriginalPayload;
        }
    }

    public boolean isEnableStreaming()
    {
        return enableStreaming;
    }

    public void setEnableStreaming(boolean enableStreaming)
    {
        if (verifyContextNotStarted())
        {
            this.enableStreaming = enableStreaming;
        }
    }

    protected boolean verifyContextNotInitialized()
    {
        MuleContext context = MuleServer.getMuleContext();
        if (context != null && context.getLifecycleManager().isPhaseComplete(Initialisable.PHASE_NAME))
        {
            logger.warn("Cannot modify MuleConfiguration once the MuleContext has been initialized.  Modification will be ignored.");
            return false;
        }
        else
        {
            return true;
        }
    }
    
    protected boolean verifyContextNotStarted()
    {
        MuleContext context = MuleServer.getMuleContext();
        if (context != null && context.getLifecycleManager().isPhaseComplete(Startable.PHASE_NAME))
        {
            logger.warn("Cannot modify MuleConfiguration once the MuleContext has been started.  Modification will be ignored.");
            return false;
        }
        else
        {
            return true;
        }
    }
}
