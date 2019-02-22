/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.FatalException;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.processor.ProcessingStrategy;
import org.mule.api.serialization.ObjectSerializer;
import org.mule.config.i18n.CoreMessages;
import org.mule.construct.Flow;
import org.mule.util.FileUtils;
import org.mule.util.NetworkUtils;
import org.mule.util.NumberUtils;
import org.mule.util.StringUtils;
import org.mule.util.UUID;
import org.mule.util.xmlsecurity.XMLSecureFactories;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Configuration info. which can be set when creating the MuleContext but becomes
 * immutable after starting the MuleContext.
 */
public class DefaultMuleConfiguration implements MuleConfiguration, MuleContextAware
{

    public static final boolean DEFAULT_TRANSFORMATION_RESOLVE_NON_DIRECT = true;

    public static final String[] DEFAULT_STACKTRACE_FILTER = (
            "org.mule.processor.AbstractInterceptingMessageProcessor," +
            "org.mule.processor.chain")
            .split(",");

    /**
     * When false (default), some internal Mule entries are removed from exception stacktraces for readability.
     * @see #stackTraceFilter
     */
    public static boolean fullStackTraces = false;

    /**
     * When false (default), only a summary of the root exception
     * and trail is provided. If this flag is false, full exception information is reported.
     * Switching on DEBUG level logging with automatically set this flag to true.
     */
    public static boolean verboseExceptions = false;

    /**
     * When true, each event will keep trace information of the flows and components it traverses
     * to be shown as part of an exception message if an exception occurs.
     * Switching on DEBUG level logging with automatically set this flag to true.
     */
    public static boolean flowTrace = false;

    /**
     * If true, an exception will be thrown when mule attempts to delete an already open file.
     */
    public static boolean failIfDeleteOpenFile = false;

    /**
     * A comma-separated list of internal packages/classes which are removed from sanitized stacktraces.
     * Matching is done via string.startsWith().
     * @see #fullStackTraces
     */
    public static String[] stackTraceFilter = DEFAULT_STACKTRACE_FILTER;

    private boolean synchronous = false;

    /**
     * The type of model used for the internal system model where system created
     * services are registered
     */
    private String systemModelType = "seda";

    private String encoding = "UTF-8";

    /**
     * When running sychronously, return events can be received over transports that
     * support ack or replyTo This property determines how long to wait for a receive
     */
    private int responseTimeout = 10000;

    /**
     * The default transaction timeout value used if no specific transaction time out
     * has been set on the transaction config
     */
    private int defaultTransactionTimeout = 30000;

    /**
     * The default queue timeout value used when polling queues.
     */
    private int defaultQueueTimeout = 200;

    /**
     * The default graceful shutdown timeout used when shutting stopping mule cleanly
     * without message loss.
     */
    private int shutdownTimeout = 5000;

    /**
     * Where Mule stores any runtime files to disk. Note that in container
     * mode each app will have its working dir set one level under this dir
     * (with app's name) in the {@link #setMuleContext} callback.
     *
     */
    private String workingDirectory = "./.mule";

    /**
     * Whether the server instance is running in client mode, which means that some
     * services will not be started
     */
    private boolean clientMode = false;

    /** the unique id for this Mule instance */
    private String id;

    /** If this node is part of a cluster then this is the shared cluster Id */
    private String clusterId = "";

    /** The domain name that this instance belongs to. */
    private String domainId;

    // Debug options

    private boolean cacheMessageAsBytes = true;

    private boolean cacheMessageOriginalPayload = true;

    private boolean enableStreaming = true;

    private boolean autoWrapMessageAwareTransform = true;

    /**
     * Whether transports and processors timeouts should be disabled in order
     * to allow step debugging of mule flows.
     */
    private boolean disableTimeouts = false;

    protected static transient Log logger = LogFactory.getLog(DefaultMuleConfiguration.class);

    private MuleContext muleContext;
    private boolean containerMode;

    /**
     * By default the Mule Expression parser will perform basic syntax checking on expressions in order to provide
     * early feedback if an expression is malformed.  Part of the check is checking that all open braces are closed at
     * some point. For some expressions such as groovy, there could be a scenario where a brace is deliberately used without
     * being closed; this would cause the validation to fail.  Users can turn off validation using this flag.
     */
    private boolean validateExpressions = true;

    private boolean useExtendedTransformations = DEFAULT_TRANSFORMATION_RESOLVE_NON_DIRECT;

    private boolean flowEndingWithOneWayEndpointReturnsNull;
    
    private boolean enricherPropagatesSessionVariableChanges;

    /**
     * Generic string/string map of properties in addition to standard Mule props.
     * Used as an extension point e.g. in MMC.
     */
    private Map<String, String> extendedProperties = new HashMap<String, String>();

    /**
     * Global exception strategy name to be used as default exception strategy for flows and services
     */
    private String defaultExceptionStrategyName;

    /**
     * List of extensions defined in the configuration element at the application.
     */
    private List<Object> extensions;

    /**
     * The instance of {@link ObjectSerializer} to use by default
     *
     * @since 3.7.0
     */
    private ObjectSerializer defaultObjectSerializer;

    /**
     * The default {@link ProcessingStrategy} to be used by all
     * {@link Flow}s which doesn't specify otherwise
     *
     * @since 3.7.0
     */
    private ProcessingStrategy defaultProcessingStrategy;

    public DefaultMuleConfiguration()
    {
        this(false);
    }

    public DefaultMuleConfiguration(boolean containerMode)
    {
        this.containerMode = containerMode;

        // Apply any settings which come from the JVM system properties.
        applySystemProperties();

        if (id == null)
        {
            id = UUID.getUUID();
        }

        if (domainId == null)
        {
            try
            {
                domainId = NetworkUtils.getLocalHost().getHostName();
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

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
        if (containerMode)
        {
            final String muleHome = System.getProperty("mule.home");
            // in container mode the id is the app name, have each app isolate its work dir
            if (!isStandalone()) {
                // fallback to current dir as a parent
                this.workingDirectory = String.format("%s/%s", getWorkingDirectory(), getId());
            }
            else
            {
                this.workingDirectory = String.format("%s/%s/%s", muleHome.trim(), getWorkingDirectory(), getId());
            }
        }
        else if (isStandalone())
        {
            this.workingDirectory = String.format("%s/%s", getWorkingDirectory(), getId());
        }
    }

    /**
     * Apply any settings which come from the JVM system properties.
     */
    protected void applySystemProperties()
    {
        String p;

        p = System.getProperty(MuleProperties.MULE_ENCODING_SYSTEM_PROPERTY);
        if (p != null)
        {
            encoding = p;
        }
        else
        {
            System.setProperty(MuleProperties.MULE_ENCODING_SYSTEM_PROPERTY, encoding);
        }
        p = System.getProperty(MuleProperties.SYSTEM_PROPERTY_PREFIX + "endpoints.synchronous");
        if (p != null)
        {
            synchronous = BooleanUtils.toBoolean(p);
        }
        p = System.getProperty(MuleProperties.SYSTEM_PROPERTY_PREFIX + "systemModelType");
        if (p != null)
        {
            systemModelType = p;
        }
        p = System.getProperty(MuleProperties.SYSTEM_PROPERTY_PREFIX + "workingDirectory");
        if (p != null)
        {
            workingDirectory = p;
        }
        p = System.getProperty(MuleProperties.SYSTEM_PROPERTY_PREFIX + "clientMode");
        if (p != null)
        {
            clientMode = BooleanUtils.toBoolean(p);
        }
        p = System.getProperty(MuleProperties.SYSTEM_PROPERTY_PREFIX + "serverId");
        if (p != null)
        {
            id = p;
        }
        p = System.getProperty(MuleProperties.SYSTEM_PROPERTY_PREFIX + "domainId");
        if (p != null)
        {
            domainId = p;
        }
        p = System.getProperty(MuleProperties.SYSTEM_PROPERTY_PREFIX + "message.cacheBytes");
        if (p != null)
        {
            cacheMessageAsBytes = BooleanUtils.toBoolean(p);
        }
        p = System.getProperty(MuleProperties.SYSTEM_PROPERTY_PREFIX + "message.cacheOriginal");
        if (p != null)
        {
            cacheMessageOriginalPayload = BooleanUtils.toBoolean(p);
        }
        p = System.getProperty(MuleProperties.SYSTEM_PROPERTY_PREFIX + "streaming.enable");
        if (p != null)
        {
            enableStreaming = BooleanUtils.toBoolean(p);
        }
        p = System.getProperty(MuleProperties.SYSTEM_PROPERTY_PREFIX + "transform.autoWrap");
        if (p != null)
        {
            autoWrapMessageAwareTransform = BooleanUtils.toBoolean(p);
        }
        p = System.getProperty(MuleProperties.SYSTEM_PROPERTY_PREFIX + "stacktrace.full");
        if (p != null)
        {
            fullStackTraces = false;
        }
        p = System.getProperty(MuleProperties.SYSTEM_PROPERTY_PREFIX + "stacktrace.filter");
        if (p != null)
        {
            stackTraceFilter = p.split(",");
        }
        else
        {
            stackTraceFilter = DEFAULT_STACKTRACE_FILTER;
        }

        p = System.getProperty(MuleProperties.SYSTEM_PROPERTY_PREFIX + "verbose.exceptions");
        if (p != null)
        {
            verboseExceptions = BooleanUtils.toBoolean(p);
        }
        else
        {
            verboseExceptions = false;
        }

        p = System.getProperty(MuleProperties.MULE_FLOW_TRACE);
        if (p != null)
        {
            flowTrace = BooleanUtils.toBoolean(p);
        }
        else
        {
            flowTrace = false;
        }

        p = System.getProperty(MuleProperties.MULE_FAIL_IF_DELETE_OPEN_FILE);
        if (p != null)
        {
            failIfDeleteOpenFile = BooleanUtils.toBoolean(p);
        }
        else
        {
            failIfDeleteOpenFile = false;
        }

        p = System.getProperty(MuleProperties.SYSTEM_PROPERTY_PREFIX + "validate.expressions");
        if (p != null)
        {
            validateExpressions = Boolean.valueOf(p);
        }

        p = System.getProperty(MuleProperties.SYSTEM_PROPERTY_PREFIX + "timeout.disable");
        if (p != null)
        {
            disableTimeouts = Boolean.valueOf(p);
        }
    }
    
    public static boolean isVerboseExceptions()
    {
        return verboseExceptions || logger.isDebugEnabled();
    }
    
    /**
     * @return {@code true} if the log is set to debug or if the system property {@code mule.flowTrace} is set to
     *         {@code true}. {@code false} otherwise.
     */
    public static boolean isFlowTrace()
    {
        return flowTrace || logger.isDebugEnabled();
    }

    public static boolean shouldFailIfDeleteOpenFile()
    {
        return failIfDeleteOpenFile;
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
     * <li><a href="http://xerces.apache.org/xerces2-j/faq-general.html#faq-4">Xerces</a>
     * <li><a href="http://xml.apache.org/xalan-j/faq.html#faq-N100D6">Xalan</a>
     * <li><a href="http://java.sun.com/j2se/1.4.2/docs/guide/standards/">Endorsed Standards Override Mechanism</a>
     * </ul>
     */
    protected void validateXML() throws FatalException
    {
        SAXParserFactory f = XMLSecureFactories.createDefault().getSAXParserFactory();
        if (f == null || f.getClass().getName().indexOf("crimson") != -1)
        {
            throw new FatalException(CoreMessages.valueIsInvalidFor(f.getClass().getName(),
                "javax.xml.parsers.SAXParserFactory"), this);
        }
    }

    public void setDefaultSynchronousEndpoints(boolean synchronous)
    {
        if (verifyContextNotStarted())
        {
            this.synchronous = synchronous;
        }
    }

    @Override
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

    @Override
    public String getWorkingDirectory()
    {
        return workingDirectory;
    }

    @Override
    public String getMuleHomeDirectory()
    {
        return System.getProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY);
    }

    public void setWorkingDirectory(String workingDirectory)
    {
        if (verifyContextNotInitialized())
        {
            try
            {
                File canonicalFile = FileUtils.openDirectory(workingDirectory);
                this.workingDirectory = canonicalFile.getCanonicalPath();
            }
            catch (IOException e)
            {
                throw new IllegalArgumentException(CoreMessages.initialisationFailure(
                    "Invalid working directory").getMessage(), e);
            }
        }
    }

    @Override
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

    @Override
    public boolean isValidateExpressions()
    {
        return validateExpressions;
    }

    @Override
    public boolean isClientMode()
    {
        return clientMode;
    }

    @Override
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

    @Override
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

    public void setClusterId(String clusterId)
    {
        this.clusterId = clusterId;
    }

    @Override
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

    @Override
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

    @Override
    public String getSystemName()
    {
        return domainId + "." + clusterId + "." + id;
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
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
        if (muleContext != null && muleContext.getLifecycleManager().isPhaseComplete(Initialisable.PHASE_NAME))
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
        if (muleContext != null && muleContext.getLifecycleManager().isPhaseComplete(Startable.PHASE_NAME))
        {
            logger.warn("Cannot modify MuleConfiguration once the MuleContext has been started.  Modification will be ignored.");
            return false;
        }
        else
        {
            return true;
        }
    }

    @Override
    public int getDefaultQueueTimeout()
    {
        return defaultQueueTimeout;
    }

    public void setDefaultQueueTimeout(int defaultQueueTimeout)
    {
        if (verifyContextNotStarted())
        {
            this.defaultQueueTimeout = defaultQueueTimeout;
        }
    }

    @Override
    public int getShutdownTimeout()
    {
        return shutdownTimeout;
    }

    public void setShutdownTimeout(int shutdownTimeout)
    {
        if (verifyContextNotStarted())
        {
            this.shutdownTimeout = shutdownTimeout;
        }
    }

    @Override
    public boolean isContainerMode()
    {
        return this.containerMode;
    }

    /**
     * The setting is only editable before the context has been initialized, change requests ignored afterwards.
     */
    public void setContainerMode(boolean containerMode)
    {
        if (verifyContextNotInitialized())
        {
            this.containerMode = containerMode;
        }
    }

    @Override
    public boolean isStandalone()
    {
        // this is our best guess
        return getMuleHomeDirectory() != null;
    }

    public Map<String, String> getExtendedProperties()
    {
        return extendedProperties;
    }

    public void setExtendedProperties(Map<String, String> extendedProperties)
    {
        this.extendedProperties = extendedProperties;
    }

    public void setExtendedProperty(String name, String value)
    {
        this.extendedProperties.put(name, value);
    }

    public String getExtendedProperty(String name)
    {
        return this.extendedProperties.get(name);
    }

    @Override
    public String getDefaultExceptionStrategyName()
    {
        return defaultExceptionStrategyName;
    }

    public void setUseExtendedTransformations(boolean useExtendedTransformations)
    {
        if (verifyContextNotStarted())
        {
            this.useExtendedTransformations = useExtendedTransformations;
        }
    }

    @Override
    public boolean useExtendedTransformations()
    {
        return useExtendedTransformations;
    }

    public void setFlowEndingWithOneWayEndpointReturnsNull(boolean flowEndingWithOneWayEndpointReturnsNull)
    {
        this.flowEndingWithOneWayEndpointReturnsNull = flowEndingWithOneWayEndpointReturnsNull;
    }

    @Override
    public boolean isFlowEndingWithOneWayEndpointReturnsNull()
    {
        return flowEndingWithOneWayEndpointReturnsNull;
    }

    public void setDefaultExceptionStrategyName(String defaultExceptionStrategyName)
    {
        this.defaultExceptionStrategyName = defaultExceptionStrategyName;
    }
    
    @Override
    public boolean isEnricherPropagatesSessionVariableChanges()
    {
        return enricherPropagatesSessionVariableChanges;
    }

    public void setEnricherPropagatesSessionVariableChanges(boolean enricherPropagatesSessionVariableChanges)
    {
        this.enricherPropagatesSessionVariableChanges = enricherPropagatesSessionVariableChanges;
    }

    @Override
    public boolean isDisableTimeouts()
    {
        return disableTimeouts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ObjectSerializer getDefaultObjectSerializer()
    {
        return defaultObjectSerializer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessingStrategy getDefaultProcessingStrategy()
    {
        return defaultProcessingStrategy;
    }

    public void setDefaultProcessingStrategy(ProcessingStrategy defaultProcessingStrategy)
    {
        this.defaultProcessingStrategy = defaultProcessingStrategy;
    }

    public void setDefaultObjectSerializer(ObjectSerializer defaultObjectSerializer)
    {
        this.defaultObjectSerializer = defaultObjectSerializer;
    }

    public void setExtensions(List<Object> extensions)
    {
        this.extensions = extensions;
    }

    @Override
    public <T> T getExtension(final Class<T> extensionType)
    {
        return (T) CollectionUtils.find(extensions, new Predicate()
        {
            @Override
            public boolean evaluate(Object object)
            {
                return extensionType.isAssignableFrom(object.getClass());
            }
        });
    }

    public List<Object> getExtensions()
    {
        if (extensions == null)
        {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(extensions);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (autoWrapMessageAwareTransform ? 1231 : 1237);
        result = prime * result + (cacheMessageAsBytes ? 1231 : 1237);
        result = prime * result + (cacheMessageOriginalPayload ? 1231 : 1237);
        result = prime * result + (clientMode ? 1231 : 1237);
        result = prime * result + defaultQueueTimeout;
        result = prime * result + defaultTransactionTimeout;
        result = prime * result + ((domainId == null) ? 0 : domainId.hashCode());
        result = prime * result + (enableStreaming ? 1231 : 1237);
        result = prime * result + ((encoding == null) ? 0 : encoding.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + responseTimeout;
        result = prime * result + shutdownTimeout;
        result = prime * result + (useExtendedTransformations ? 1231 : 1237);
        result = prime * result + (flowEndingWithOneWayEndpointReturnsNull ? 1231 : 1237);
        result = prime * result + (synchronous ? 1231 : 1237);
        result = prime * result + ((systemModelType == null) ? 0 : systemModelType.hashCode());
        result = prime * result + ((workingDirectory == null) ? 0 : workingDirectory.hashCode());
        result = prime * result + (containerMode ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        DefaultMuleConfiguration other = (DefaultMuleConfiguration) obj;
        if (autoWrapMessageAwareTransform != other.autoWrapMessageAwareTransform)
        {
            return false;
        }
        if (cacheMessageAsBytes != other.cacheMessageAsBytes)
        {
            return false;
        }
        if (cacheMessageOriginalPayload != other.cacheMessageOriginalPayload)
        {
            return false;
        }
        if (clientMode != other.clientMode)
        {
            return false;
        }
        if (defaultQueueTimeout != other.defaultQueueTimeout)
        {
            return false;
        }
        if (defaultTransactionTimeout != other.defaultTransactionTimeout)
        {
            return false;
        }
        if (domainId == null)
        {
            if (other.domainId != null)
            {
                return false;
            }
        }
        else if (!domainId.equals(other.domainId))
        {
            return false;
        }
        if (enableStreaming != other.enableStreaming)
        {
            return false;
        }
        if (encoding == null)
        {
            if (other.encoding != null)
            {
                return false;
            }
        }
        else if (!encoding.equals(other.encoding))
        {
            return false;
        }
        if (id == null)
        {
            if (other.id != null)
            {
                return false;
            }
        }
        else if (!id.equals(other.id))
        {
            return false;
        }
        if (responseTimeout != other.responseTimeout)
        {
            return false;
        }
        if (shutdownTimeout != other.shutdownTimeout)
        {
            return false;
        }
        if (useExtendedTransformations != other.useExtendedTransformations)
        {
            return false;
        }
        if (flowEndingWithOneWayEndpointReturnsNull != other.flowEndingWithOneWayEndpointReturnsNull)
        {
            return false;
        }
        if (synchronous != other.synchronous)
        {
            return false;
        }
        if (systemModelType == null)
        {
            if (other.systemModelType != null)
            {
                return false;
            }
        }
        else if (!systemModelType.equals(other.systemModelType))
        {
            return false;
        }
        if (workingDirectory == null)
        {
            if (other.workingDirectory != null)
            {
                return false;
            }
        }
        else if (!workingDirectory.equals(other.workingDirectory))
        {
            return false;
        }

        if (containerMode != other.containerMode)
        {
            return false;
        }

        return true;
    }

}
