/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config;


import static java.lang.Boolean.valueOf;
import static java.lang.System.getProperty;
import org.mule.runtime.api.deployment.management.ComponentInitialStateManager;
import org.mule.runtime.api.util.MuleSystemProperties;

/**
 * <code>MuleProperties</code> is a set of constants pertaining to Mule properties.
 */
public class MuleProperties {

  /**
   * The prefix for any Mule-specific properties set on an event
   */
  public static final String PROPERTY_PREFIX = "MULE_";

  /**
   * The prefix for endpoint properties that should not be propagated to messages
   */
  public static final String ENDPOINT_PROPERTY_PREFIX = PROPERTY_PREFIX + "ENDPOINT__";
  // End System properties

  /**
   * ***************************************************************************** MuleEvent Level properties
   * *****************************************************************************
   */
  public static final String MULE_EVENT_PROPERTY = PROPERTY_PREFIX + "EVENT";
  public static final String MULE_EVENT_TIMEOUT_PROPERTY = PROPERTY_PREFIX + "EVENT_TIMEOUT";
  public static final String MULE_METHOD_PROPERTY = "method";

  // Deprecated. 'method' is now used consistently for all transports
  // public static final String MULE_METHOD_PROPERTY = PROPERTY_PREFIX + "SERVICE_METHOD";
  public static final String MULE_IGNORE_METHOD_PROPERTY = PROPERTY_PREFIX + "IGNORE_METHOD";
  public static final String MULE_ENDPOINT_PROPERTY = PROPERTY_PREFIX + "ENDPOINT";
  public static final String MULE_ROOT_MESSAGE_ID_PROPERTY = PROPERTY_PREFIX + "ROOT_MESSAGE_ID";
  public static final String MULE_ORIGINATING_ENDPOINT_PROPERTY = PROPERTY_PREFIX + "ORIGINATING_ENDPOINT";
  public static final String MULE_ERROR_CODE_PROPERTY = PROPERTY_PREFIX + "ERROR_CODE";
  public static final String MULE_REPLY_TO_PROPERTY = PROPERTY_PREFIX + "REPLYTO";
  /**
   * Prevents processing of the ReplyTo property by the Service. This is useful if you're component routed the message somewhere
   * else which processed the ReplyTo.
   */
  public static final String MULE_REPLY_TO_STOP_PROPERTY = PROPERTY_PREFIX + "REPLYTO_STOP";
  public static final String MULE_USER_PROPERTY = PROPERTY_PREFIX + "USER";
  public static final String MULE_REPLY_TO_REQUESTOR_PROPERTY = PROPERTY_PREFIX + "REPLYTO_REQUESTOR";
  @Deprecated // TODO MULE-9072
  public static final String MULE_SESSION_PROPERTY = PROPERTY_PREFIX + "SESSION";
  public static final String MULE_MESSAGE_ID_PROPERTY = PROPERTY_PREFIX + "MESSAGE_ID";
  public static final String MULE_CORRELATION_ID_PROPERTY = PROPERTY_PREFIX + "CORRELATION_ID";
  public static final String MULE_CORRELATION_GROUP_SIZE_PROPERTY = PROPERTY_PREFIX + "CORRELATION_GROUP_SIZE";
  public static final String MULE_CORRELATION_SEQUENCE_PROPERTY = PROPERTY_PREFIX + "CORRELATION_SEQUENCE";
  public static final String MULE_REMOTE_SYNC_PROPERTY = PROPERTY_PREFIX + "REMOTE_SYNC";
  public static final String MULE_REMOTE_CLIENT_ADDRESS = PROPERTY_PREFIX + "REMOTE_CLIENT_ADDRESS";
  public static final String MULE_PROXY_ADDRESS = PROPERTY_PREFIX + "PROXY_ADDRESS";
  public static final String MULE_SOAP_METHOD = PROPERTY_PREFIX + "SOAP_METHOD";
  public static final String MULE_MANAGEMENT_CONTEXT_PROPERTY = PROPERTY_PREFIX + "MANAGEMENT_CONTEXT";
  public static final String MULE_CREDENTIALS_PROPERTY = PROPERTY_PREFIX + "CREDENTIALS";
  public static final String MULE_DISABLE_TRANSPORT_TRANSFORMER_PROPERTY = PROPERTY_PREFIX + "DISABLE_TRANSPORT_TRANSFORMER";
  public static final String MULE_FORCE_SYNC_PROPERTY = PROPERTY_PREFIX + "FORCE_SYNC";
  // End MuleEvent Level properties

  /**
   * ***************************************************************************** Logging properties
   * *****************************************************************************
   */

  public static final String LOG_CONTEXT_SELECTOR_PROPERTY = "Log4jContextSelector";
  public static final String DEFAULT_LOG_CONTEXT_SELECTOR =
      "org.mule.runtime.core.module.launcher.log4j.ArtifactAwareContextSelector";
  public static final String LOG_CONFIGURATION_FACTORY_PROPERTY = "log4j.configurationFactory";
  public static final String DEFAULT_LOG_CONFIGURATION_FACTORY =
      "org.mule.runtime.core.module.launcher.log4j.MuleLoggerConfigurationFactory";

  /**
   * ***************************************************************************** Generic Service descriptor properties
   * *****************************************************************************
   */
  public static final String SERVICE_FINDER = "service.finder";

  /**
   * ***************************************************************************** Transport Service descriptor properties
   * *****************************************************************************
   */
  public static final String CONNECTOR_CLASS = "connector";
  public static final String CONNECTOR_MESSAGE_RECEIVER_CLASS = "message.receiver";
  public static final String CONNECTOR_TRANSACTED_MESSAGE_RECEIVER_CLASS = "transacted.message.receiver";
  public static final String CONNECTOR_XA_TRANSACTED_MESSAGE_RECEIVER_CLASS = "xa.transacted.message.receiver";
  public static final String CONNECTOR_DISPATCHER_FACTORY = "dispatcher.factory";
  public static final String CONNECTOR_REQUESTER_FACTORY = "requester.factory";
  public static final String CONNECTOR_TRANSACTION_FACTORY = "transaction.factory";
  public static final String CONNECTOR_MESSAGE_FACTORY = "message.factory";
  public static final String CONNECTOR_INBOUND_TRANSFORMER = "inbound.transformer";
  public static final String CONNECTOR_OUTBOUND_TRANSFORMER = "outbound.transformer";
  public static final String CONNECTOR_RESPONSE_TRANSFORMER = "response.transformer";
  public static final String CONNECTOR_ENDPOINT_BUILDER = "endpoint.builder";
  public static final String CONNECTOR_SERVICE_FINDER = "service.finder";
  public static final String CONNECTOR_SERVICE_ERROR = "service.error";
  public static final String CONNECTOR_SESSION_HANDLER = "session.handler";
  public static final String CONNECTOR_META_ENDPOINT_BUILDER = "meta.endpoint.builder";
  public static final String CONNECTOR_INBOUND_EXCHANGE_PATTERNS = "inbound.exchange.patterns";
  public static final String CONNECTOR_OUTBOUND_EXCHANGE_PATTERNS = "outbound.exchange.patterns";
  public static final String CONNECTOR_DEFAULT_EXCHANGE_PATTERN = "default.exchange.pattern";
  // End Connector Service descriptor properties

  public static final String MULE_WORKING_DIRECTORY_PROPERTY = "mule.working.dir";
  public static final String MULE_HOME_DIRECTORY_PROPERTY = "mule.home";
  public static final String MULE_BASE_DIRECTORY_PROPERTY = "mule.base";
  public static final String APP_HOME_DIRECTORY_PROPERTY = "app.home";
  public static final String DOMAIN_HOME_DIRECTORY_PROPERTY = "domain.home";
  public static final String APP_NAME_PROPERTY = "app.name";
  public static final String DOMAIN_NAME_PROPERTY = "domain.name";

  // Object Name Keys
  public static final String OBJECT_MULE_CONTEXT = "_muleContext";
  public static final String OBJECT_MULE_CONTEXT_PROCESSOR = "_muleContextProcessor";
  public static final String OBJECT_PROPERTY_PLACEHOLDER_PROCESSOR = "_mulePropertyPlaceholderProcessor";
  public static final String OBJECT_OBJECT_NAME_PROCESSOR = "_muleObjectNameProcessor";
  public static final String OBJECT_LIFECYCLE_MANAGER = "_muleLifecycleManager";
  public static final String OBJECT_CLASSLOADER_REPOSITORY = "_muleClassLoaderRepository";
  public static final String OBJECT_SECURITY_MANAGER = "_muleSecurityManager";
  public static final String OBJECT_TRANSACTION_MANAGER = "_muleTransactionManager";
  public static final String OBJECT_QUEUE_MANAGER = "_muleQueueManager";
  public static final String OBJECT_LOCAL_QUEUE_MANAGER = "_localQueueManager";
  public static final String OBJECT_LOCAL_STORE_IN_MEMORY = "_localInMemoryObjectStore";
  public static final String OBJECT_LOCAL_STORE_PERSISTENT = "_localPersistentObjectStore";
  public static final String OBJECT_STORE_MANAGER = "_muleObjectStoreManager";
  public static final String LOCAL_OBJECT_STORE_MANAGER = "_muleLocalObjectStoreManager";
  public static final String OBJECT_MULE_APPLICATION_PROPERTIES = "_muleProperties";
  public static final String OBJECT_MULE_OUTBOUND_ENDPOINT_EXECUTOR_FACTORY = "_muleOutboundEndpointExecutorFactory";
  public static final String OBJECT_MULE_STREAM_CLOSER_SERVICE = "_muleStreamCloserService";
  public static final String OBJECT_MULE_SIMPLE_REGISTRY_BOOTSTRAP = "_muleSimpleRegistryBootstrap";
  public static final String OBJECT_DEFAULT_GLOBAL_EXCEPTION_STRATEGY = "_defaultGlobalExceptionStrategy";
  public static final String OBJECT_MULE_CONFIGURATION = "_muleConfiguration";
  public static final String OBJECT_MULE_NAMESPACE_MANAGER = "_muleNamespaceManager";
  public static final String OBJECT_CONVERTER_RESOLVER = "_converterResolver";
  public static final String OBJECT_EXPRESSION_LANGUAGE = "_muleExpressionLanguage";
  public static final String OBJECT_EXPRESSION_MANAGER = "_muleExpressionManager";
  public static final String OBJECT_LOCK_FACTORY = "_muleLockFactory";
  public static final String LOCAL_OBJECT_LOCK_FACTORY = "_muleLocalLockFactory";
  public static final String OBJECT_LOCK_PROVIDER = "_muleLockProvider";
  public static final String OBJECT_DEFAULT_MESSAGE_PROCESSING_MANAGER = "_muleMessageProcessingManager";
  public static final String OBJECT_PROCESSING_TIME_WATCHER = "_muleProcessingTimeWatcher";
  public static final String OBJECT_POLLING_CONTROLLER = "_mulePollingController";
  public static final String OBJECT_CLUSTER_CONFIGURATION = "_muleClusterConfiguration";
  public static final String OBJECT_EXTENSION_MANAGER = "_muleExtensionManager";
  public static final String OBJECT_EXTENSION_OAUTH_MANAGER = "extensions.oauth.manager";
  public static final String OBJECT_TIME_SUPPLIER = "_muleTimeSupplier";
  public static final String OBJECT_CONNECTION_MANAGER = "_muleConnectionManager";
  public static final String OBJECT_EXCEPTION_LOCATION_PROVIDER = "_muleExceptionLocationProvider";
  public static final String OBJECT_MESSAGE_PROCESSING_FLOW_TRACE_MANAGER = "_muleMessageProcessingFlowTraceManager";
  public static final String OBJECT_CONFIGURATION_PROPERTIES = "_muleConfigurationAttributesResolver";
  public static final String OBJECT_POLICY_MANAGER = "_mulePolicyManager";
  public static final String OBJECT_POLICY_PROVIDER = "_mulePolicyProvider";
  public static final String OBJECT_POLICY_MANAGER_STATE_HANDLER = "_mulePolicyStateHandler";
  public static final String OBJECT_STREAMING_MANAGER = "_muleStreamingManager";
  public static final String OBJECT_REGISTRY = "_muleRegistry";
  public static final String OBJECT_CURSOR_MANAGER = "_muleStreamingManager";
  public static final String OBJECT_TRANSFORMATION_SERVICE = "_muleTransformationService";
  public static final String OBJECT_COMPONENT_INITIAL_STATE_MANAGER = ComponentInitialStateManager.SERVICE_ID;
  public static final String DEFAULT_TLS_CONTEXT_FACTORY_REGISTRY_KEY = "_muleDefaultTlsContextFactory";
  public static final String OBJECT_SCHEDULER_POOLS_CONFIG = "_muleSchedulerPoolsConfig";
  public static final String OBJECT_SCHEDULER_BASE_CONFIG = "_muleSchedulerBaseConfig";
  public static final String OBJECT_CLUSTER_SERVICE = "_muleClusterService";
  public static final String OBJECT_NOTIFICATION_DISPATCHER = "_muleNotificationDispatcher";
  public static final String OBJECT_NOTIFICATION_LISTENER_REGISTRY = "_muleNotificationListenerRegistry";
  public static final String OBJECT_TRANSACTION_FACTORY_LOCATOR = "_muleTransactionFactoryLocator";
  public static final String OBJECT_STATISTICS = "_muleStatistics";
  public static final String OBJECT_RESOURCE_LOCATOR = "_muleResourceLocator";
  public static final String COMPATIBILITY_PLUGIN_INSTALLED = "_compatibilityPluginInstalled";


  // Not currently used as these need to be instance variables of the MuleContext.
  public static final String OBJECT_NOTIFICATION_MANAGER = "_muleNotificationManager";
  public static final String OBJECT_NOTIFICATION_HANDLER = "_muleNotificationHandler";

  /**
   * Specifies whether mule should process messages synchronously, i.e. that a mule-model can only process one message at a time,
   * or asynchronously. The default value is 'false'.
   */
  // TODO BL-76: remove me!
  public static final String SYNCHRONOUS_PROPERTY = "synchronous";
  public static final String EXCHANGE_PATTERN = "exchange-pattern";
  public static final String EXCHANGE_PATTERN_CAMEL_CASE = "exchangePattern";

  /**
   * The prefix for any Mule-specific properties set in the system properties
   * @deprecated since 4.2. Use {@link MuleSystemProperties#SYSTEM_PROPERTY_PREFIX} instead
   */
  @Deprecated
  public static final String SYSTEM_PROPERTY_PREFIX = MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;

  /**
   * @deprecated since 4.2.0. Use {@link MuleSystemProperties#MULE_CONTEXT_PROPERTY} instead
   */
  @Deprecated
  public static final String MULE_CONTEXT_PROPERTY = MuleSystemProperties.MULE_CONTEXT_PROPERTY;

  /**
   * @deprecated since 4.2.0. Use {@link MuleSystemProperties#MULE_ENCODING_SYSTEM_PROPERTY} instead
   */
  @Deprecated
  public static final String MULE_ENCODING_SYSTEM_PROPERTY = MuleSystemProperties.MULE_ENCODING_SYSTEM_PROPERTY;

  /**
   * @deprecated since 4.2.0. Use {@link MuleSystemProperties#MULE_SECURITY_SYSTEM_PROPERTY} instead
   */
  @Deprecated
  public static final String MULE_SECURITY_SYSTEM_PROPERTY = MuleSystemProperties.MULE_SECURITY_SYSTEM_PROPERTY;

  /**
   * @deprecated since 4.2.0. Use {@link MuleSystemProperties#MULE_SECURITY_PROVIDER_PROPERTY} instead
   */
  @Deprecated
  public static final String MULE_SECURITY_PROVIDER_PROPERTY = MuleSystemProperties.MULE_SECURITY_PROVIDER_PROPERTY;

  /**
   * @deprecated since 4.2.0. Use {@link MuleSystemProperties#MULE_STREAMING_BUFFER_SIZE} instead
   */
  @Deprecated
  public static final String MULE_STREAMING_BUFFER_SIZE = MuleSystemProperties.MULE_STREAMING_BUFFER_SIZE;

  /**
   * System property key for the default size of a streaming buffer bucket
   * @since 4.1.4
   * @deprecated since 4.2.0. Use {@link MuleSystemProperties#MULE_STREAMING_BUCKET_SIZE} instead
   */
  @Deprecated
  public static final String MULE_STREAMING_BUCKET_SIZE = SYSTEM_PROPERTY_PREFIX + "streaming.bucketSize";

  /**
   * @deprecated since 4.2.0. Use {@link MuleSystemProperties#MULE_STREAMING_MAX_MEMORY} instead
   */
  @Deprecated
  public static final String MULE_STREAMING_MAX_MEMORY = MuleSystemProperties.MULE_STREAMING_MAX_MEMORY;

  /**
   * @deprecated since 4.2.0. Use {@link MuleSystemProperties#MULE_SIMPLE_LOG} instead
   */
  @Deprecated
  public static final String MULE_SIMPLE_LOG = MuleSystemProperties.MULE_SIMPLE_LOG;

  /**
   * @deprecated since 4.2.0. Use {@link MuleSystemProperties#MULE_FORCE_CONSOLE_LOG} instead
   */
  @Deprecated
  public static final String MULE_FORCE_CONSOLE_LOG = MuleSystemProperties.MULE_FORCE_CONSOLE_LOG;

  /**
   * @deprecated since 4.2.0. Use {@link MuleSystemProperties#MULE_LOG_CONTEXT_DISPOSE_DELAY_MILLIS} instead
   */
  @Deprecated
  public static final String MULE_LOG_CONTEXT_DISPOSE_DELAY_MILLIS = MuleSystemProperties.MULE_LOG_CONTEXT_DISPOSE_DELAY_MILLIS;

  /**
   * @deprecated since 4.2.0. Use {@link MuleSystemProperties#MULE_LOG_DEFAULT_POLICY_INTERVAL} instead
   */
  @Deprecated
  public static final String MULE_LOG_DEFAULT_POLICY_INTERVAL = MuleSystemProperties.MULE_LOG_DEFAULT_POLICY_INTERVAL;

  /**
   * @deprecated since 4.2.0. Use {@link MuleSystemProperties#MULE_LOG_DEFAULT_STRATEGY_MAX} instead
   */
  @Deprecated
  public static final String MULE_LOG_DEFAULT_STRATEGY_MAX = MuleSystemProperties.MULE_LOG_DEFAULT_STRATEGY_MAX;

  /**
   * @deprecated since 4.2.0. Use {@link MuleSystemProperties#MULE_LOG_DEFAULT_STRATEGY_MIN} instead
   */
  @Deprecated
  public static final String MULE_LOG_DEFAULT_STRATEGY_MIN = MuleSystemProperties.MULE_LOG_DEFAULT_STRATEGY_MIN;

  /**
   * @deprecated since 4.2.0. Use {@link MuleSystemProperties#MULE_FLOW_TRACE} instead
   */
  @Deprecated
  public static final String MULE_FLOW_TRACE = MuleSystemProperties.MULE_FLOW_TRACE;

  /**
   * @deprecated since 4.2.0. Use {@link MuleSystemProperties#MULE_LOG_VERBOSE_CLASSLOADING} instead
   */
  @Deprecated
  public static final String MULE_LOG_VERBOSE_CLASSLOADING = MuleSystemProperties.MULE_LOG_VERBOSE_CLASSLOADING;

  /**
   * @deprecated since 4.2.0. Use {@link MuleSystemProperties#MULE_MEL_AS_DEFAULT} instead
   */
  @Deprecated
  public static final String MULE_MEL_AS_DEFAULT = MuleSystemProperties.MULE_MEL_AS_DEFAULT;

  /**
   * @deprecated since 4.2.0. Use {@link MuleSystemProperties#MULE_DISABLE_RESPONSE_TIMEOUT} instead
   */
  @Deprecated
  public static final String MULE_DISABLE_RESPONSE_TIMEOUT = MuleSystemProperties.MULE_DISABLE_RESPONSE_TIMEOUT;

  /**
   * @deprecated since 4.2.0. Use {@link MuleSystemProperties#MULE_ALLOW_JRE_EXTENSION} instead
   */
  @Deprecated
  public static final String MULE_ALLOW_JRE_EXTENSION = MuleSystemProperties.MULE_ALLOW_JRE_EXTENSION;

  /**
   * @deprecated since 4.2.0. Use {@link MuleSystemProperties#MULE_JRE_EXTENSION_PACKAGES} instead
   */
  @Deprecated
  public static final String MULE_JRE_EXTENSION_PACKAGES = MuleSystemProperties.MULE_JRE_EXTENSION_PACKAGES;

  /**
   * @deprecated since 4.2.0. Use {@link MuleSystemProperties#MULE_LOGGING_INTERVAL_SCHEDULERS_LATENCY_REPORT} instead
   */
  @Deprecated
  public static final String MULE_LOGGING_INTERVAL_SCHEDULERS_LATENCY_REPORT =
      MuleSystemProperties.MULE_LOGGING_INTERVAL_SCHEDULERS_LATENCY_REPORT;

  /**
   * @return Whether MEL is set as the default EL according to {@link #MULE_MEL_AS_DEFAULT}
   * @since 4.2.0
   */
  public static boolean isMelDefault() {
    return valueOf(getProperty(MULE_MEL_AS_DEFAULT, "false"));
  }

  private MuleProperties() {}
}
