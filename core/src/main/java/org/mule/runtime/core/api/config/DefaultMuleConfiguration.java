/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config;

import static org.mule.runtime.api.util.MuleSystemProperties.MULE_DISABLE_RESPONSE_TIMEOUT;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_ENCODING_SYSTEM_PROPERTY;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.initialisationFailure;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.propertyHasInvalidValue;
import static org.mule.runtime.core.api.util.ClassUtils.instantiateClass;
import static org.mule.runtime.core.internal.util.StandaloneServerUtils.getMuleBase;
import static org.mule.runtime.core.internal.util.StandaloneServerUtils.getMuleHome;

import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.util.Collections.unmodifiableList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.component.InternalComponent;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.FatalException;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.util.FileUtils;
import org.mule.runtime.core.api.util.NetworkUtils;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.core.api.util.UUID;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;

/**
 * Configuration info. which can be set when creating the MuleContext but becomes immutable after starting the MuleContext. TODO
 * MULE-13121 Cleanup MuleConfiguration removing redundant config in Mule 4
 */
@NoExtend
public class DefaultMuleConfiguration implements MuleConfiguration, MuleContextAware, InternalComponent {

  protected static final Logger LOGGER = getLogger(DefaultMuleConfiguration.class);

  private MuleVersion minMuleVersion;

  /**
   * The type of model used for the internal system model where system created services are registered
   */
  private String systemModelType = "seda";

  private String encoding = "UTF-8";

  /**
   * When running sychronously, return events can be received over transports that support ack or replyTo This property determines
   * how long to wait for a receive
   */
  private int responseTimeout = 10000;

  /**
   * The default transaction timeout value used if no specific transaction time out has been set on the transaction config
   */
  private int defaultTransactionTimeout = 30000;

  /**
   * The default queue timeout value used when polling queues.
   */
  private int defaultQueueTimeout = 200;

  /**
   * The default graceful shutdown timeout used when shutting stopping mule cleanly without message loss.
   */
  private long shutdownTimeout = 5000;

  /**
   * Where Mule stores any runtime files to disk. Note that in container mode each app will have its working dir set one level
   * under this dir (with app's name) in the {@link #setMuleContext} callback.
   *
   */
  private String workingDirectory = "./.mule";

  /**
   * Whether the server instance is running in client mode, which means that some services will not be started
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

  private boolean enableStreaming = true;

  private boolean autoWrapMessageAwareTransform = true;

  /**
   * Whether transports and processors timeouts should be disabled in order to allow step debugging of mule flows.
   */
  private boolean disableTimeouts = false;

  private MuleContext muleContext;
  private boolean containerMode;

  /**
   * By default the Mule Expression parser will perform basic syntax checking on expressions in order to provide early feedback if
   * an expression is malformed. Part of the check is checking that all open braces are closed at some point. For some expressions
   * such as groovy, there could be a scenario where a brace is deliberately used without being closed; this would cause the
   * validation to fail. Users can turn off validation using this flag.
   */
  private boolean validateExpressions = true;

  /**
   * Generic string/string map of properties in addition to standard Mule props. Used as an extension point e.g. in MMC.
   */
  private Map<String, String> extendedProperties = new HashMap<>();

  /**
   * Global exception strategy name to be used as default exception strategy for flows and services
   */
  private String defaultExceptionStrategyName;

  /**
   * List of extensions defined in the configuration element at the application.
   */
  private final List<ConfigurationExtension> extensions = new ArrayList<>();

  /**
   * The instance of {@link ObjectSerializer} to use by default
   *
   * @since 3.7.0
   */
  private ObjectSerializer defaultObjectSerializer;

  /**
   * The {@link ProcessingStrategyFactory factory} of the default {@link ProcessingStrategy} to be used by all {@link Flow flows}
   * which doesn't specify otherwise
   *
   * @since 3.7.0
   */
  private ProcessingStrategyFactory defaultProcessingStrategyFactory;

  /**
   * Maximum size (approximately) of the transaction log files. This applies to each set of files for local transactions and xa
   * transactions when using queues.
   *
   * @since 3.9.0
   */
  private int maxQueueTransactionFilesSizeInMegabytes = 500;

  /**
   * Whether streamed iterable objects should follow the repeatability strategy of the iterable or use the default one.
   *
   * @since 4.3.0
   */
  private boolean inheritIterableRepeatability = false;

  /**
   * Generator to override default correlation id
   *
   * @since 4.4.0
   */
  private Optional<CorrelationIdGenerator> correlationIdGenerationExpression = empty();

  /**
   * The {@link ArtifactCoordinates} for the deployed app
   *
   * @since 4.5.0
   */
  private ArtifactCoordinates artifactCoordinates;

  private DynamicConfigExpiration dynamicConfigExpiration =
      DynamicConfigExpiration.getDefault();
  private String dataFolderName;

  public DefaultMuleConfiguration() {
    this(false);
  }

  public DefaultMuleConfiguration(boolean containerMode) {
    this.containerMode = containerMode;

    // Apply any settings which come from the JVM system properties.
    applySystemProperties();

    if (id == null) {
      id = UUID.getUUID();
    }

    if (domainId == null) {
      try {
        domainId = NetworkUtils.getLocalHost().getHostName();
      } catch (UnknownHostException e) {
        LOGGER.warn("Unable to obtain hostname", e);
        domainId = "org.mule.runtime.core";
      }
    }

    try {
      validateEncoding();
    } catch (FatalException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  private void updateWorkingDirectory() {
    if (containerMode) {
      final String muleBase = getMuleBase().map(File::getAbsolutePath).orElse(null);
      // in container mode the id is the app name, have each app isolate its work dir
      if (!isStandalone()) {
        // fallback to current dir as a parent
        this.workingDirectory = format("%s/%s", getWorkingDirectory(), getDataFolderName());
      } else {
        this.workingDirectory = format("%s/%s/%s", muleBase.trim(), getWorkingDirectory(), getDataFolderName());
      }
    } else if (isStandalone()) {
      this.workingDirectory = format("%s/%s", getWorkingDirectory(), getDataFolderName());
    }
  }

  /**
   * Apply any settings which come from the JVM system properties.
   */
  protected void applySystemProperties() {
    String p;

    p = getProperty(MULE_ENCODING_SYSTEM_PROPERTY);
    if (p != null) {
      encoding = p;
    } else {
      System.setProperty(MULE_ENCODING_SYSTEM_PROPERTY, encoding);
    }
    p = getProperty(SYSTEM_PROPERTY_PREFIX + "systemModelType");
    if (p != null) {
      systemModelType = p;
    }
    p = getProperty(SYSTEM_PROPERTY_PREFIX + "workingDirectory");
    if (p != null) {
      workingDirectory = p;
    }
    p = getProperty(SYSTEM_PROPERTY_PREFIX + "clientMode");
    if (p != null) {
      clientMode = BooleanUtils.toBoolean(p);
    }
    p = getProperty(SYSTEM_PROPERTY_PREFIX + "serverId");
    if (p != null) {
      id = p;
    }
    p = getProperty(SYSTEM_PROPERTY_PREFIX + "domainId");
    if (p != null) {
      domainId = p;
    }
    p = getProperty(SYSTEM_PROPERTY_PREFIX + "message.cacheBytes");
    if (p != null) {
      cacheMessageAsBytes = BooleanUtils.toBoolean(p);
    }
    p = getProperty(SYSTEM_PROPERTY_PREFIX + "streaming.enable");
    if (p != null) {
      enableStreaming = BooleanUtils.toBoolean(p);
    }
    p = getProperty(SYSTEM_PROPERTY_PREFIX + "transform.autoWrap");
    if (p != null) {
      autoWrapMessageAwareTransform = BooleanUtils.toBoolean(p);
    }

    p = getProperty(SYSTEM_PROPERTY_PREFIX + "validate.expressions");
    if (p != null) {
      validateExpressions = Boolean.valueOf(p);
    }

    p = getProperty(MULE_DISABLE_RESPONSE_TIMEOUT);
    if (p != null) {
      disableTimeouts = Boolean.valueOf(p);
    }
    try {
      p = getProperty(ProcessingStrategyFactory.class.getName());
      if (p != null) {
        defaultProcessingStrategyFactory = (ProcessingStrategyFactory) instantiateClass(p);
      }
    } catch (Throwable e) {
      LOGGER.warn("Unable to instantiate ProcessingStrategyFactory '{}', default will be used instead.", p);
    }
  }

  /**
   * @return {@code true} if the log is set to debug or if the system property {@code mule.flowTrace} is set to {@code true}.
   *         {@code false} otherwise.
   * @deprecated
   */
  @Deprecated(since = "4.7.0")
  public static boolean isFlowTrace() {
    return false;
  }

  protected void validateEncoding() throws FatalException {
    // Check we have a valid and supported encoding
    if (!Charset.isSupported(encoding)) {
      throw new FatalException(propertyHasInvalidValue("encoding", encoding), this);
    }
  }

  /**
   * @deprecated this is a leftover from Mule 3
   */
  @Deprecated(since = "4.4.0")
  public void setDefaultSynchronousEndpoints(boolean synchronous) {
    // Nothing to do
  }

  @Override
  public int getDefaultResponseTimeout() {
    return responseTimeout;
  }

  public void setDefaultResponseTimeout(int responseTimeout) {
    if (verifyContextNotStarted()) {
      this.responseTimeout = responseTimeout;
    }
  }

  @Override
  public String getWorkingDirectory() {
    return workingDirectory;
  }

  @Override
  public String getMuleHomeDirectory() {
    return getMuleHome().map(File::getAbsolutePath).orElse(null);
  }

  public void setWorkingDirectory(String workingDirectory) {
    if (verifyContextNotInitialized()) {
      try {
        File canonicalFile = FileUtils.openDirectory(workingDirectory);
        this.workingDirectory = canonicalFile.getCanonicalPath();
      } catch (IOException e) {
        throw new IllegalArgumentException(initialisationFailure("Invalid working directory").getMessage(), e);
      }
    }
  }

  /**
   * Sets the {@link ArtifactCoordinates} for the deployed app
   *
   * @param artifactCoordinates the app's {@link ArtifactCoordinates}
   * @since 4.5.0
   */
  public void setArtifactCoordinates(ArtifactCoordinates artifactCoordinates) {
    this.artifactCoordinates = artifactCoordinates;
  }

  @Override
  public int getDefaultTransactionTimeout() {
    return defaultTransactionTimeout;
  }

  public void setDefaultTransactionTimeout(int defaultTransactionTimeout) {
    if (verifyContextNotStarted()) {
      this.defaultTransactionTimeout = defaultTransactionTimeout;
    }
  }

  @Override
  public boolean isValidateExpressions() {
    return validateExpressions;
  }

  @Override
  public boolean isClientMode() {
    return clientMode;
  }

  @Override
  public String getDefaultEncoding() {
    return encoding;
  }

  public void setDefaultEncoding(String encoding) {
    if (verifyContextNotInitialized()) {
      this.encoding = encoding;
    }
  }

  @Override
  public String getId() {
    return id;
  }

  public void setId(String id) {
    if (verifyContextNotInitialized()) {
      if (StringUtils.isBlank(id)) {
        throw new IllegalArgumentException("Cannot set server id to null/blank");
      }
      this.id = id;
    }
  }

  public void setClusterId(String clusterId) {
    this.clusterId = clusterId;
  }

  @Override
  public String getDomainId() {
    return domainId;
  }

  public void setDomainId(String domainId) {
    if (verifyContextNotInitialized()) {
      this.domainId = domainId;
    }
  }

  @Override
  public String getSystemModelType() {
    return systemModelType;
  }

  /**
   * @deprecated this is a leftover from Mule 3
   */
  @Deprecated(since = "4.4.0")
  public void setSystemModelType(String systemModelType) {
    if (verifyContextNotStarted()) {
      this.systemModelType = systemModelType;
    }
  }

  /**
   * @deprecated this is a leftover from Mule 3
   */
  @Deprecated(since = "4.4.0")
  public void setClientMode(boolean clientMode) {
    if (verifyContextNotStarted()) {
      this.clientMode = clientMode;
    }
  }

  @Override
  public String getSystemName() {
    return domainId + "." + clusterId + "." + id;
  }

  @Override
  public boolean isAutoWrapMessageAwareTransform() {
    return autoWrapMessageAwareTransform;
  }

  public void setAutoWrapMessageAwareTransform(boolean autoWrapMessageAwareTransform) {
    if (verifyContextNotStarted()) {
      this.autoWrapMessageAwareTransform = autoWrapMessageAwareTransform;
    }
  }

  @Override
  public boolean isCacheMessageAsBytes() {
    return cacheMessageAsBytes;
  }

  /**
   * @deprecated this is a leftover from Mule 3
   */
  @Deprecated(since = "4.4.0")
  public void setCacheMessageAsBytes(boolean cacheMessageAsBytes) {
    if (verifyContextNotStarted()) {
      this.cacheMessageAsBytes = cacheMessageAsBytes;
    }
  }

  @Override
  public boolean isEnableStreaming() {
    return enableStreaming;
  }

  /**
   * @deprecated this is a leftover from Mule 3
   */
  @Deprecated(since = "4.4.0")
  public void setEnableStreaming(boolean enableStreaming) {
    if (verifyContextNotStarted()) {
      this.enableStreaming = enableStreaming;
    }
  }

  @Override
  public boolean isLazyInit() {
    return false;
  }

  /**
   * @deprecated Since 4.4 this is a no-op.
   */
  @Deprecated(since = "4.4.0")
  public void setLazyInit(boolean lazyInit) {}

  protected boolean verifyContextNotInitialized() {
    if (muleContext != null && muleContext.getLifecycleManager().isPhaseComplete(Initialisable.PHASE_NAME)) {
      LOGGER.warn("Cannot modify MuleConfiguration once the MuleContext has been initialized.  Modification will be ignored.");
      return false;
    } else {
      return true;
    }
  }

  protected boolean verifyContextNotStarted() {
    if (muleContext != null && muleContext.getLifecycleManager().isPhaseComplete(Startable.PHASE_NAME)) {
      LOGGER.warn("Cannot modify MuleConfiguration once the MuleContext has been started.  Modification will be ignored.");
      return false;
    } else {
      return true;
    }
  }

  @Override
  public int getDefaultQueueTimeout() {
    return defaultQueueTimeout;
  }

  public void setDefaultQueueTimeout(int defaultQueueTimeout) {
    if (verifyContextNotStarted()) {
      this.defaultQueueTimeout = defaultQueueTimeout;
    }
  }

  @Override
  public long getShutdownTimeout() {
    return shutdownTimeout;
  }


  @Override
  public int getMaxQueueTransactionFilesSizeInMegabytes() {
    return maxQueueTransactionFilesSizeInMegabytes;
  }

  public void setShutdownTimeout(long shutdownTimeout) {
    if (verifyContextNotStarted()) {
      if (shutdownTimeout < 0) {
        throw new IllegalArgumentException(format("'shutdownTimeout' must be a possitive long. %d passed", shutdownTimeout));
      }
      this.shutdownTimeout = shutdownTimeout;
    }
  }

  @Override
  public boolean isContainerMode() {
    return this.containerMode;
  }

  /**
   * The setting is only editable before the context has been initialized, change requests ignored afterwards.
   */
  public void setContainerMode(boolean containerMode) {
    if (verifyContextNotInitialized()) {
      this.containerMode = containerMode;
    }
  }

  @Override
  public boolean isStandalone() {
    // this is our best guess
    return getMuleHomeDirectory() != null;
  }

  public Map<String, String> getExtendedProperties() {
    return extendedProperties;
  }

  public void setExtendedProperties(Map<String, String> extendedProperties) {
    this.extendedProperties = extendedProperties;
  }

  public void setExtendedProperty(String name, String value) {
    this.extendedProperties.put(name, value);
  }

  public String getExtendedProperty(String name) {
    return this.extendedProperties.get(name);
  }

  @Override
  public String getDefaultErrorHandlerName() {
    return defaultExceptionStrategyName;
  }

  public void setDefaultErrorHandlerName(String defaultExceptionStrategyName) {
    this.defaultExceptionStrategyName = defaultExceptionStrategyName;
  }

  public void setMaxQueueTransactionFilesSize(int maxQueueTransactionFilesSizeInMegabytes) {
    this.maxQueueTransactionFilesSizeInMegabytes = maxQueueTransactionFilesSizeInMegabytes;
  }

  @Override
  public boolean isDisableTimeouts() {
    return disableTimeouts;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ObjectSerializer getDefaultObjectSerializer() {
    return defaultObjectSerializer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ProcessingStrategyFactory getDefaultProcessingStrategyFactory() {
    return defaultProcessingStrategyFactory;
  }

  public void setDefaultProcessingStrategyFactory(ProcessingStrategyFactory defaultProcessingStrategy) {
    this.defaultProcessingStrategyFactory = defaultProcessingStrategy;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DynamicConfigExpiration getDynamicConfigExpiration() {
    return dynamicConfigExpiration;
  }

  public void setDynamicConfigExpiration(DynamicConfigExpiration dynamicConfigExpiration) {
    this.dynamicConfigExpiration = dynamicConfigExpiration;
  }

  public void setDefaultObjectSerializer(ObjectSerializer defaultObjectSerializer) {
    this.defaultObjectSerializer = defaultObjectSerializer;
  }

  /**
   * @return the name of the folder associated to this artifact to use for storing artifact related data
   */
  public String getDataFolderName() {
    return dataFolderName != null ? dataFolderName : getId();
  }

  /**
   * @param dataFolderName the name of the folder associated to this artifact to use for storing artifact related data
   */
  public void setDataFolderName(String dataFolderName) {
    this.dataFolderName = dataFolderName;
    updateWorkingDirectory();
  }

  public void setInheritIterableRepeatability(String inheritIterableRepeatability) {
    this.inheritIterableRepeatability = parseBoolean(inheritIterableRepeatability);
  }

  public void setInheritIterableRepeatability(boolean inheritIterableRepeatability) {
    this.inheritIterableRepeatability = inheritIterableRepeatability;
  }

  public void addExtensions(List<ConfigurationExtension> extensions) {
    this.extensions.addAll(extensions);
  }

  @Override
  public <T> T getExtension(final Class<T> extensionType) {
    return (T) extensions
        .stream()
        .filter(object -> extensionType.isAssignableFrom(object.getClass()))
        .findFirst()
        .orElse(null);
  }

  public List<ConfigurationExtension> getExtensions() {
    return unmodifiableList(extensions);
  }

  @Override
  public boolean isInheritIterableRepeatability() {
    return inheritIterableRepeatability;
  }

  @Override
  public Optional<MuleVersion> getMinMuleVersion() {
    return Optional.ofNullable(minMuleVersion);
  }

  public void setMinMuleVersion(MuleVersion minMuleversion) {
    this.minMuleVersion = minMuleversion;
  }

  @Override
  public Optional<CorrelationIdGenerator> getDefaultCorrelationIdGenerator() {
    return correlationIdGenerationExpression;
  }

  @Override
  public Optional<ArtifactCoordinates> getArtifactCoordinates() {
    return ofNullable(artifactCoordinates);
  }

  public void setDefaultCorrelationIdGenerator(CorrelationIdGenerator generator) {
    this.correlationIdGenerationExpression = of(generator);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (autoWrapMessageAwareTransform ? 1231 : 1237);
    result = prime * result + (cacheMessageAsBytes ? 1231 : 1237);
    result = prime * result + (clientMode ? 1231 : 1237);
    result = prime * result + defaultQueueTimeout;
    result = prime * result + defaultTransactionTimeout;
    result = prime * result + maxQueueTransactionFilesSizeInMegabytes;
    result = prime * result + ((domainId == null) ? 0 : domainId.hashCode());
    result = prime * result + (enableStreaming ? 1231 : 1237);
    result = prime * result + ((encoding == null) ? 0 : encoding.hashCode());
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + responseTimeout;
    result = prime * result + ((Long) shutdownTimeout).hashCode();
    result = prime * result + ((systemModelType == null) ? 0 : systemModelType.hashCode());
    result = prime * result + ((workingDirectory == null) ? 0 : workingDirectory.hashCode());
    result = prime * result + (containerMode ? 1231 : 1237);
    result = prime * result + (inheritIterableRepeatability ? 1231 : 1237);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    DefaultMuleConfiguration other = (DefaultMuleConfiguration) obj;
    if (autoWrapMessageAwareTransform != other.autoWrapMessageAwareTransform) {
      return false;
    }
    if (cacheMessageAsBytes != other.cacheMessageAsBytes) {
      return false;
    }
    if (clientMode != other.clientMode) {
      return false;
    }
    if (defaultQueueTimeout != other.defaultQueueTimeout) {
      return false;
    }
    if (defaultTransactionTimeout != other.defaultTransactionTimeout) {
      return false;
    }
    if (domainId == null) {
      if (other.domainId != null) {
        return false;
      }
    } else if (!domainId.equals(other.domainId)) {
      return false;
    }
    if (enableStreaming != other.enableStreaming) {
      return false;
    }
    if (encoding == null) {
      if (other.encoding != null) {
        return false;
      }
    } else if (!encoding.equals(other.encoding)) {
      return false;
    }
    if (responseTimeout != other.responseTimeout) {
      return false;
    }
    if (shutdownTimeout != other.shutdownTimeout) {
      return false;
    }
    if (systemModelType == null) {
      if (other.systemModelType != null) {
        return false;
      }
    } else if (!systemModelType.equals(other.systemModelType)) {
      return false;
    }
    if (workingDirectory == null) {
      if (other.workingDirectory != null) {
        return false;
      }
    } else if (!workingDirectory.equals(other.workingDirectory)) {
      return false;
    }

    if (containerMode != other.containerMode) {
      return false;
    }
    if (maxQueueTransactionFilesSizeInMegabytes != other.maxQueueTransactionFilesSizeInMegabytes) {
      return false;
    }
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    return inheritIterableRepeatability == other.inheritIterableRepeatability;
  }

}
