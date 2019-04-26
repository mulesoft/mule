/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.config;


/**
 * <code>MuleProperties</code> is a set of constants pertaining to Mule properties.
 */
public class MuleProperties
{
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
     * *****************************************************************************
     * MuleEvent Level properties
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
     * Prevents processing of the ReplyTo property by the Service. This is useful
     * if you're component routed the message somewhere else which processed the
     * ReplyTo.
     */
    public static final String MULE_REPLY_TO_STOP_PROPERTY = PROPERTY_PREFIX + "REPLYTO_STOP";
    public static final String MULE_USER_PROPERTY = PROPERTY_PREFIX + "USER";
    public static final String MULE_ENCODING_PROPERTY = PROPERTY_PREFIX + "ENCODING";
    public static final String MULE_REPLY_TO_REQUESTOR_PROPERTY = PROPERTY_PREFIX + "REPLYTO_REQUESTOR";
    /**
     * @deprecated This property is no longer needed and will be removed in the next major release
     */
    @Deprecated
    public static final String MULE_SESSION_ID_PROPERTY = PROPERTY_PREFIX + "SESSION_ID";
    public static final String MULE_SESSION_PROPERTY = PROPERTY_PREFIX + "SESSION";
    public static final String MULE_MESSAGE_ID_PROPERTY = PROPERTY_PREFIX + "MESSAGE_ID";
    public static final String MULE_CORRELATION_ID_PROPERTY = PROPERTY_PREFIX + "CORRELATION_ID";
    public static final String MULE_CORRELATION_GROUP_SIZE_PROPERTY = PROPERTY_PREFIX + "CORRELATION_GROUP_SIZE";
    public static final String MULE_CORRELATION_SEQUENCE_PROPERTY = PROPERTY_PREFIX + "CORRELATION_SEQUENCE";
    public static final String MULE_REMOTE_SYNC_PROPERTY = PROPERTY_PREFIX + "REMOTE_SYNC";
    public static final String MULE_REMOTE_CLIENT_ADDRESS = PROPERTY_PREFIX + "REMOTE_CLIENT_ADDRESS";
    public static final String MULE_PROXY_ADDRESS = PROPERTY_PREFIX + "PROXY_ADDRESS";
    public static final String MULE_SOAP_METHOD = PROPERTY_PREFIX + "SOAP_METHOD";
    public static final String MULE_JMS_SESSION = PROPERTY_PREFIX + "JMS_SESSION";
    public static final String MULE_MANAGEMENT_CONTEXT_PROPERTY = PROPERTY_PREFIX + "MANAGEMENT_CONTEXT";
    public static final String MULE_CREDENTIALS_PROPERTY = PROPERTY_PREFIX + "CREDENTIALS";
    public static final String MULE_DISABLE_TRANSPORT_TRANSFORMER_PROPERTY = PROPERTY_PREFIX + "DISABLE_TRANSPORT_TRANSFORMER";
    public static final String MULE_FORCE_SYNC_PROPERTY = PROPERTY_PREFIX + "FORCE_SYNC";
    // End MuleEvent Level properties

    /**
     * *****************************************************************************
     * Logging properties
     * *****************************************************************************
     */

    public static final String LOG_CONTEXT_SELECTOR_PROPERTY = "Log4jContextSelector";
    public static final String DEFAULT_LOG_CONTEXT_SELECTOR = "org.mule.module.launcher.log4j.ArtifactAwareContextSelector";
    public static final String LOG_CONFIGURATION_FACTORY_PROPERTY = "log4j.configurationFactory";
    public static final String DEFAULT_LOG_CONFIGURATION_FACTORY = "org.mule.module.launcher.log4j.MuleLoggerConfigurationFactory";

    /**
     * *****************************************************************************
     * Generic Service descriptor properties
     * *****************************************************************************
     */
    public static final String SERVICE_FINDER = "service.finder";

    /**
     * *****************************************************************************
     * Model Service descriptor properties
     * *****************************************************************************
     */
    public static final String MODEL_CLASS = "model";

    /**
     * *****************************************************************************
     * Transport Service descriptor properties
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
    public static final String APP_HOME_DIRECTORY_PROPERTY = "app.home";
    public static final String APP_NAME_PROPERTY = "app.name";

    // Object Name Keys
    public static final String OBJECT_MULE_CONTEXT = "_muleContext";
    public static final String OBJECT_SYSTEM_MODEL = "_muleSystemModel";
    public static final String OBJECT_MULE_CONTEXT_PROCESSOR = "_muleContextProcessor";
    public static final String OBJECT_PROPERTY_PLACEHOLDER_PROCESSOR = "_mulePropertyPlaceholderProcessor";
    public static final String OBJECT_OBJECT_NAME_PROCESSOR = "_muleObjectNameProcessor";
    public static final String OBJECT_LIFECYCLE_MANAGER = "_muleLifecycleManager";
    public static final String OBJECT_SERIALIZER = "_muleDefaultObjectSerializer";
    public static final String OBJECT_SECURITY_MANAGER = "_muleSecurityManager";
    public static final String OBJECT_TRANSACTION_MANAGER = "_muleTransactionManager";
    public static final String OBJECT_QUEUE_MANAGER = "_muleQueueManager";
    public static final String OBJECT_STORE_DEFAULT_IN_MEMORY_NAME = "_defaultInMemoryObjectStore";
    public static final String OBJECT_STORE_DEFAULT_PERSISTENT_NAME = "_defaultPersistentObjectStore";
    public static final String QUEUE_STORE_DEFAULT_IN_MEMORY_NAME = "_defaultInMemoryQueueStore";
    public static final String QUEUE_STORE_DEFAULT_PERSISTENT_NAME = "_defaultPersistentQueueStore";
    public static final String DEFAULT_USER_OBJECT_STORE_NAME = "_defaultUserObjectStore";
    public static final String DEFAULT_USER_TRANSIENT_OBJECT_STORE_NAME = "_defaultTransientUserObjectStore";
    public static final String OBJECT_STORE_MANAGER = "_muleObjectStoreManager";
    public static final String OBJECT_STORE_SIMPLE_MEMORY_NAME = "_simpleMemoryQueueStore";
    public static final String OBJECT_STORE_FILE_NAME = "_fileQueueStore";
    public static final String OBJECT_MULE_APPLICATION_PROPERTIES = "_muleProperties";
    public static final String OBJECT_MULE_ENDPOINT_FACTORY = "_muleEndpointFactory";
    public static final String OBJECT_MULE_OUTBOUND_ENDPOINT_EXECUTOR_FACTORY = "_muleOutboundEndpointExecutorFactory";
    public static final String OBJECT_MULE_STREAM_CLOSER_SERVICE = "_muleStreamCloserService";
    public static final String OBJECT_MULE_SIMPLE_REGISTRY_BOOTSTRAP = "_muleSimpleRegistryBootstrap";
    public static final String OBJECT_DEFAULT_THREADING_PROFILE = "_defaultThreadingProfile";
    public static final String OBJECT_DEFAULT_MESSAGE_DISPATCHER_THREADING_PROFILE = "_defaultMessageDispatcherThreadingProfile";
    public static final String OBJECT_DEFAULT_MESSAGE_REQUESTER_THREADING_PROFILE = "_defaultMessageRequesterThreadingProfile";
    public static final String OBJECT_DEFAULT_MESSAGE_RECEIVER_THREADING_PROFILE = "_defaultMessageReceiverThreadingProfile";
    public static final String OBJECT_DEFAULT_SERVICE_THREADING_PROFILE = "_defaultServiceThreadingProfile";
    public static final String OBJECT_DEFAULT_GLOBAL_EXCEPTION_STRATEGY = "_defaultGlobalExceptionStrategy";
    public static final String OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE = "_defaultRetryPolicyTemplate";
    public static final String OBJECT_MULE_CONFIGURATION = "_muleConfiguration";
    public static final String OBJECT_MULE_NAMESPACE_MANAGER = "_muleNamespaceManager";
    public static final String OBJECT_CONVERTER_RESOLVER = "_converterResolver";
    public static final String OBJECT_EXPRESSION_LANGUAGE = "_muleExpressionLanguage";
    public static final String OBJECT_LOCK_FACTORY = "_muleLockFactory";
    public static final String OBJECT_LOCK_PROVIDER = "_muleLockProvider";
    public static final String OBJECT_DEFAULT_MESSAGE_PROCESSING_MANAGER = "_muleMessageProcessingManager";
    public static final String OBJECT_PROCESSING_TIME_WATCHER = "_muleProcessingTimeWatcher";
    public static final String OBJECT_POLLING_CONTROLLER = "_mulePollingController";
    public static final String OBJECT_CLUSTER_CONFIGURATION = "_muleClusterConfiguration";
    public static final String OBJECT_CONNECTOR_MESSAGE_PROCESSOR_LOCATOR = "_muleConnectorMessageProcessorLocator";
    public static final String OBJECT_EXTENSION_MANAGER = "_muleExtensionManager";

    // Not currently used as these need to be instance variables of the MuleContext.
    public static final String OBJECT_WORK_MANAGER = "_muleWorkManager";
    public static final String OBJECT_NOTIFICATION_MANAGER = "_muleNotificationManager";

    /**
     * Specifies whether mule should process messages synchronously, i.e. that a
     * mule-model can only process one message at a time, or asynchronously. The
     * default value is 'false'.
     */
    // TODO BL-76: remove me!
    public static final String SYNCHRONOUS_PROPERTY = "synchronous";
    public static final String EXCHANGE_PATTERN = "exchange-pattern";
    public static final String EXCHANGE_PATTERN_CAMEL_CASE = "exchangePattern";

    /**
     * The prefix for any Mule-specific properties set in the system properties
     */
    public static final String SYSTEM_PROPERTY_PREFIX = "mule.";
    public static final String MULE_CONTEXT_PROPERTY = SYSTEM_PROPERTY_PREFIX + "context";
    public static final String MULE_ENCODING_SYSTEM_PROPERTY = SYSTEM_PROPERTY_PREFIX + "encoding";
    public static final String MULE_SECURITY_SYSTEM_PROPERTY = SYSTEM_PROPERTY_PREFIX + "security.model";
    public static final String MULE_SECURITY_PROVIDER_PROPERTY = SYSTEM_PROPERTY_PREFIX + "security.provider";
    public static final String MULE_STREAMING_BUFFER_SIZE = SYSTEM_PROPERTY_PREFIX + "streaming.bufferSize";
    public static final String MULE_SIMPLE_LOG = SYSTEM_PROPERTY_PREFIX + "simpleLog";
    public static final String MULE_FORCE_CONSOLE_LOG = SYSTEM_PROPERTY_PREFIX + "forceConsoleLog";
    public static final String MULE_LOG_CONTEXT_DISPOSE_DELAY_MILLIS = SYSTEM_PROPERTY_PREFIX + "log.context.dispose.delay.millis";
    public static final String MULE_DEFAULT_PROCESSING_STRATEGY = SYSTEM_PROPERTY_PREFIX + "default.processing.strategy";
    public static final String MULE_HANDLE_COPY_OF_EVENT_IN_MESSAGE_PROCESSOR_NOTIFICATION = SYSTEM_PROPERTY_PREFIX + "handle.copy.event.for.notification";
    public static final String MULE_FLOW_TRACE = SYSTEM_PROPERTY_PREFIX + "flowTrace";
    public static final String MULE_FAIL_IF_DELETE_OPEN_FILE = SYSTEM_PROPERTY_PREFIX + "failIfDeleteOpenFile";
    public static final String CONTENT_TYPE_PROPERTY = "Content-Type";
    public static final String DISABLE_ERROR_COUNT_ON_ERROR_NOTIFICATION_DISABLED = SYSTEM_PROPERTY_PREFIX + "disable.error.count.on.error.notifications.disabled";
    public static final String MULE_EXPRESSION_FILTER_DEFAULT_BOOLEAN_VALUE = SYSTEM_PROPERTY_PREFIX + "expressionFilter.nonBooleanReturnsTrue";
}
