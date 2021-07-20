/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.allure;

public interface AllureConstants {

  interface DbFeature {

    String DB_EXTENSION = "DB Extension";

  }

  interface LeakPrevention {

    String LEAK_PREVENTION = "Leak Prevention";

    interface LeakPreventionMetaspace {

      String METASPACE_LEAK_PREVENTION_ON_REDEPLOY = "Class Loader leak prevention on redeploy";
    }

  }

  interface SpringModuleFeature {

    String SPRING_EXTENSION = "Spring Extension";

    interface SpringModuleStory {

      String SPRING_MODULE = "Spring Module";
    }

  }

  interface ConfigurationComponentLocatorFeature {

    String CONFIGURATION_COMPONENT_LOCATOR = "Configuration component locator";

    interface ConfigurationComponentLocatorStory {

      String SEARCH_CONFIGURATION = "Search configuration";
    }

    interface ConfigurationComponentTypeStore {

      String COMPONENT_CONFIGURATION_TYPE = "Component type";
    }

    interface ConfigurationComponentLocationStory {

      String COMPONENT_LOCATION = "Component location";

    }

    interface MUnitComponentLocatorStory {

      String MUNIT_COMPONENT_LOCATION = "MUnit component location";

    }

    interface ComponentLifeCycle {

      String COMPONENT_LIFE_CYCLE = "Component life cycle";
    }
  }

  interface ArtifactDeploymentFeature {

    String APP_DEPLOYMENT = "Application Deployment";
    String DOMAIN_DEPLOYMENT = "Domain Deployment";
    String POLICY_DEPLOYMENT = "Policy Deployment";
    String POLICY_REORDER = "Policy Reorder";

    interface DeploymentSuccessfulStory {

      String DEPLOYMENT_SUCCESS = "Deployment Success";

    }

    interface DeploymentFailureStory {

      String DEPLOYMENT_FAILURE = "Deployment Failure";

    }

    interface UndeploymentFailureStory {

      String UNDEPLOYMENT = "Undeployment";

    }
  }

  interface DeploymentTypeFeature {

    String DEPLOYMENT_TYPE = "Deployment type";

    interface DeploymentTypeStory {

      String HEAVYWEIGHT = "HeavyWeight";
      String LIGHTWEIGHT = "LightWeight";
      String EMBEDDED = "Embedded";

    }

    interface RedeploymentStory {

      String APPLICATION_REDEPLOYMENT = "Application Redeployment";
      String APPLICATION_PATCHING = "Application Patching";

    }
  }

  interface ClassloadingIsolationFeature {

    String CLASSLOADING_ISOLATION = "Classloading Isolation";

  }

  interface PackagerFeature {

    String PACKAGER = "Packager";

  }

  interface LazyInitializationFeature {

    String LAZY_INITIALIZATION = "Lazy Initialization";
  }

  interface DeploymentConfiguration {

    String DEPLOYMENT_CONFIGURATION = "Deployment Configuration";

    interface ApplicationConfiguration {

      String APPLICATION_CONFIGURATION = "Application Configuration";

    }

    interface LazyConnectionsStory {

      String LAZY_CONNECTIONS = "Lazy Connections";

    }

    interface ArtifactDeclarationStory {

      String ARTIFACT_DECLARATION = "Artifact Declaration";

    }

    interface FeatureFlaggingStory {

      String FEATURE_FLAGGING = "Feature Flagging";
    }

    interface FlowStatePersistenceStory {

      String FLOW_STATE_PERSISTENCE = "Flow State Persistence";
    }

  }

  interface EmailFeature {

    String EMAIL_EXTENSION = "Email Extension";

    interface EmailStory {

    }
  }

  interface EmbeddedApiFeature {

    String EMBEDDED_API = "Embedded API";

    interface EmbeddedApiStory {

      String CONFIGURATION = "configuration";
    }
  }

  interface ErrorHandlingFeature {

    String ERROR_HANDLING = "Error Handling";

    interface ErrorHandlingStory {

      String ERROR_TYPES = "Error Types";
      String ERROR_HANDLER = "Error Handler";
      String EXCEPTION_MAPPINGS = "Exception Mappings";
      String ERROR_MAPPINGS = "Error Mappings";
      String ON_ERROR_CONTINUE = "On Error Continue";
      String ON_ERROR_PROPAGATE = "On Error Propagate";
      String DEFAULT_ERROR_HANDLER = "Default Error Handler";
      String RAISE_ERROR = "Raise Errors";
    }
  }

  interface EventContextFeature {

    String EVENT_CONTEXT = "EventContext";

    interface EventContextStory {

      String RESPONSE_AND_COMPLETION_PUBLISHERS = "Response and completion publishers";
    }
  }

  interface ExpressionLanguageFeature {

    String EXPRESSION_LANGUAGE = "Expression Language";

    interface ExpressionLanguageStory {

      String SUPPORT_DW = "Support DW";
      String SUPPORT_FUNCTIONS = "Support Functions";
      String SUPPORT_MVEL_DW = "Support both MVEL and DW";
    }
  }

  interface ExtensionsClientFeature {

    String EXTENSIONS_CLIENT = "Extensions Client";

    interface ExtensionsClientStory {

      String BLOCKING_CLIENT = "Blocking Client";
      String NON_BLOCKING_CLIENT = "Non-Blocking Client";
    }
  }

  interface FileFeature {

    String FILE_EXTENSION = "File Extension";

    interface FileStory {

    }
  }

  interface ComponentsFeature {

    String CORE_COMPONENTS = "Core Components";

    interface FlowReferenceStory {

      String FLOW_REFERENCE = "Flow Reference";

    }

    interface LoggerStory {

      String LOGGER = "Logger";

    }

    interface ParseTemplateStory {

      String PARSE_TEMPLATE = "Parse Template";

    }

  }

  interface FtpFeature {

    String FTP_EXTENSION = "FTP Extension";

    interface FtpStory {

    }
  }

  interface HttpFeature {

    String HTTP_EXTENSION = "HTTP Extension";
    String HTTP_SERVICE = "HTTP Service";

    interface HttpStory {

      String ERRORS = "Errors";
      String ERROR_HANDLING = "Error Handling";
      String ERROR_MAPPINGS = "Error Mappings";
      String METADATA = "Metadata";
      String MULTI_MAP = "Multi Map";
      String PROXY_CONFIG_BUILDER = "Proxy Config Builder";
      String REQUEST_BUILDER = "Request Builder";
      String REQUEST_URL = "Request URL";
      String RESPONSE_BUILDER = "Response Builder";
      String STREAMING = "Streaming";
      String TCP_BUILDER = "TCP Builders";
      String NTLM = "NTLM";
      String URL_ENCODED = "URL encoded body";
      String MULTIPART = "Multipart body";
      String HTTPS = "HTTPS";
    }

  }

  interface JmsFeature {

    String JMS_EXTENSION = "JMS Extension";

    interface JmsStory {

    }

  }

  interface OauthFeature {

    String OAUTH_EXTENSION = "OAuth Extension";
    String OCS_SUPPORT = "OCS Support";

    interface OauthStory {

    }

    interface OcsStory {

      String OCS_CONNECTION_VALIDATION = "Validation of OCS data provided by the platform";

    }

  }

  interface ObjectStoreFeature {

    String OS_EXTENSION = "ObjectStore Extension";

    interface ObjectStoreStory {

      String PERSISTENT_DATA_REDEPLOYMENT = "Persistent data redeployment";

      String OBJECT_STORE_AS_OPERATION_PARAMETER = "ObjectStore is used as an operation parameter";
    }

  }

  interface ReconnectionPolicyFeature {

    String RECONNECTION_POLICIES = "Reconnection Policies";

    interface RetryTemplateStory {

      String RETRY_TEMPLATE = "Blocking";
    }
  }

  interface ProcessingStrategiesFeature {

    String PROCESSING_STRATEGIES = "Processing Strategies";

    interface ProcessingStrategiesStory {

      String BLOCKING = "Blocking";
      String DEFAULT = "Default (used when no processing strategy is configured)";
      String PROACTOR = "Proactor";
      String REACTOR = "Reactor";
      String DIRECT = "Direct";
      String WORK_QUEUE = "Work Queue";
      String ENRICHER = "Enricher";
    }

  }

  interface ForkJoinStrategiesFeature {

    String FORK_JOIN_STRATEGIES = "Fork/Join Strategies used by scatter-gather and parallel-foreach routers";

    interface ForkJoinStrategiesStory {

      String COLLECT_LIST = "Collect List";
      String COLLECT_MAP = "Collect Map";
      String JOIN_ONLY = "Join Only";
    }

  }

  interface SocketsFeature {

    String SOCKETS_EXTENSION = "Sockets Extension";

    interface SocketsStory {

    }

  }

  interface ValidationFeature {

    String VALIDATION_EXTENSION = "Validation Extension";

    interface ValidationStory {

    }

  }

  interface WscFeature {

    String WSC_EXTENSION = "WSC Extension";

    interface WscStory {

    }

  }

  interface VMFeature {

    String VM_EXTENSION = "VM Extension";

    interface VMStory {

      String PERSISTENT_DATA_REDEPLOYMENT = "Persistent data redeployment";

    }

  }

  interface SampleData {

    String SAMPLE_DATA = "Sample Data";

    interface SampleDataStory {

      String SAMPLE_DATA_SERVICE = "Sample Data Service";

      String RESOLVE_BY_LOCATION = "Resolve by location";

      String RESOLVE_THROUGH_TOOLING_API = "Resolve through Tooling API";
    }
  }

  interface IntegrationTestsFeature {

    String INTEGRATIONS_TESTS = "Integration Tests";

    interface IntegrationTestsStory {

    }

  }

  interface SchedulerServiceFeature {

    String SCHEDULER_SERVICE = "Scheduler Service";

    interface SchedulerServiceStory {

      String EXHAUSTION = "Exhaustion";
      String QUARTZ_TASK_SCHEDULING = "Quartz Task Scheduling";
      String SHUTDOWN = "Shutdown";
      String SOURCE_MANAGEMENT = "Source Management";
      String TASK_SCHEDULING = "Task Scheduling";
      String TERMINATION = "Termination";
      String THROTTLING = "Throttling";
    }

  }

  interface ExecutionEngineFeature {

    String EXECUTION_ENGINE = "Execution Engine";

    interface ExecutionEngineStory {

      String BACKPRESSURE = "Backpressure";
    }

  }

  interface SchedulerFeature {

    String SCHEDULER = "Scheduler";

    interface SchedulerStories {

      String SCHEDULED_FLOW_EXECUTION = "Scheduled flow execution";
    }
  }

  interface StreamingFeature {

    String STREAMING = "Streaming";

    interface StreamingStory {

      String BYTES_STREAMING = "Bytes Streaming";
      String OBJECT_STREAMING = "Object Streaming";
      String STREAM_MANAGEMENT = "Management of Streams";
      String TROUBLESHOOTING = "Streaming troubleshooting";
      String STATISTICS = "Payload statistics";
    }

  }

  interface SerializationFeature {

    String SERIALIZATION = "Serialization";

    interface SerializationStory {

      String STATISTICS = "Payload statistics";
    }

  }

  interface RoutersFeature {

    String ROUTERS = "Routers";

    interface ForeachStory {

      String FOR_EACH = "Foreach";
    }

    interface ScatterGatherStory {

      String SCATTER_GATHER = "Scatter Gather";
    }

    interface RoundRobinStory {

      String ROUND_ROBIN = "Round Robin";
    }

    interface FirstSuccessfulStory {

      String FIRST_SUCCESSFUL = "First Successful";
    }

    interface UntilSuccessfulStory {

      String UNTIL_SUCCESSFUL = "Until Successful";
    }

    interface AsyncStory {

      String ASYNC = "Async";
    }

    interface ProcessorChainRouterStory {

      String PROCESSOR_CHAIN_ROUTER = "Processor Chain Router";
    }

    interface ParallelForEachStory {

      String PARALLEL_FOR_EACH = "Parallel For Each";
    }

  }

  interface TransformMessageFeature {

    String TRANSFORM_MESSAGE = "Transform Message";

    interface TransformMessageStory {

    }

  }

  interface SourcesFeature {

    String SOURCES = "Sources";

    interface SourcesStories {

      String FLOW_DISPATCH = "Dispatch to flow";
      String POLLING = "Polling";
      String WATERMARK = "Watermark";
    }

  }

  interface ScopeFeature {

    String SCOPE = "Scope";

    interface ChoiceStory {

      String CHOICE = "Choice";
    }

  }

  interface LifecycleAndDependencyInjectionFeature {

    String LIFECYCLE_AND_DEPENDENCY_INJECTION = "Lifecycle and Dependency Injection";
    String NULL_OBJECTS_IN_SPRING5_REGISTRY = "Spring 5 handling of null objects";

    interface ObjectFactoryStory {

      String OBJECT_FACTORY_INECTION_AND_LIFECYCLE = "Object Factory Injection And Lifecycle";
    }

    interface LifecyclePhaseFailureStory {

      String LIFECYCLE_PHASE_FAILURE_STORY = "Lifecycle Phase Failure";
    }

    interface LifecyclePhaseStory {

      String LIFECYCLE_PHASE_STORY = "Lifecycle Phase";
    }

    interface MuleContextStartOrderStory {

      String MULE_CONTEXT_START_ORDER_STORY = "MuleContext start order";
    }

    interface ArtifactObjectsDependencyInjectionStory {

      String ARTIFACT_OBJECTS_DEPENDENCY_INJECTION_STORY = "Artifact Objects Dependency injection";
    }

    interface GracefulShutdownStory {

      String GRACEFUL_SHUTDOWN_STORY = "Graceful shutdown";
    }
  }

  interface TransactionFeature {

    String TRANSACTION = "Transaction";

    interface LocalStory {

      String LOCAL_TRANSACTION = "Local Transaction";
    }

    interface XaStory {

      String XA_TRANSACTION = "XA Transaction";
    }

  }

  interface MuleDsl {

    String MULE_DSL = "Mule DSL";

    interface DslParsingStory {

      String DSL_PARSING_STORY = "Mule DSL Parsing";

    }

    interface DslValidationStory {

      String DSL_VALIDATION_STORY = "Mule DSL Validations";

    }

  }

  interface ConfigurationProperties {

    String CONFIGURATION_PROPERTIES = "Configuration properties";

    interface ComponentConfigurationAttributesStory {

      String CONFIGURATION_PROPERTIES_RESOLVER_STORY = "Component configuration properties resolver story";

      String COMPONENT_CONFIGURATION_PROPERTIES_STORY = "Component configuration properties story";

      String COMPONENT_CONFIGURATION_YAML_STORY = "Component configuration properties with YAML story";

      String COMPONENT_CONFIGURATION_ERROR_SCEANRIOS = "Component configuration properties error scenarios";

    }

  }

  interface Logging {

    String LOGGING = "Logging";

    interface LoggingStory {

      String ERROR_REPORTING = "Error Reporting";
      String FLOW_STACK = "Flow Stack";
      String CONTEXT_FACTORY = "Log Context Factory";
      String PROCESSING_TYPE = "Processing Type";

    }

  }

  interface InterceptonApi {

    String INTERCEPTION_API = "Interception API";

    interface ComponentInterceptionStory {

      String COMPONENT_INTERCEPTION_STORY = "Component Interception Story";

      String FLOW_INTERCEPTION_STORY = "Flow Interception Story";

    }

  }

  interface RuntimeGlobalConfiguration {

    String RUNTIME_GLOBAL_CONFIGURATION = "Runtime Global Configuration";

    interface MavenGlobalConfiguration {

      String MAVEN_GLOBAL_CONFIGURATION_STORY = "Maven Global Configuration Story";

    }

    interface ClusterGlobalConfiguration {

      String CLUSTER_GLOBAL_CONFIGURATION_STORY = "Cluster Global Configuration Story";

    }

  }

  interface LicenseFeature {

    String LICENSE = "License";

    interface LicenseManagementStory {

      String LICENSE_MANAGEMENT = "License Management";

    }

  }

  interface RegistryFeature {

    String REGISTRY = "Registry";

    interface ObjectRegistrationStory {

      String OBJECT_REGISTRATION = "Object Registration";

    }

    interface DomainObjectRegistrationStory {

      String OBJECT_REGISTRATION = "Object Registration";

    }

  }

  interface DomainSupport {

    String DOMAIN_SUPPORT = "Domain Support";

  }

  interface MuleEvent {

    String MULE_EVENT = "Mule Event";

  }

  interface ArtifactAst {

    String ARTIFACT_AST = "Mule Artifact AST";

    interface ParameterAst {

      String PARAMETER_AST = "Parameter AST resolution";
    }

  }

  interface JavaSdk {

    String JAVA_SDK = "Java SDK";

    interface Parameters {

      String PARAMETERS = "Parameters definitions in Java SDK";

    }

  }

  interface XmlSdk {

    String XML_SDK = "Xml SDK";

    interface Streaming {

      String STREAMING = "Streaming consumption in operations";

    }

    interface Declaration {

      String DECLARATION_DATASENSE = "Declaration override of calculated metadata types";

    }

  }

  interface AggregatorsFeature {

    String AGGREGATORS_EXTENSION = "Aggregators Extension";

    interface AggregatorsStory {

    }
  }

  interface CorrelationIdFeature {

    String CORRELATION_ID = "Correlation ID";

    interface CorrelationIdOnSourcesStory {

      String CORRELATION_ID_ON_SOURCES = "Sources' correlation id generation";

      String CORRELATION_ID_MODIFICATION = "Correlation id modification in child context in chains";

    }
  }

  interface NotificationsFeature {

    String NOTIFICATIONS = "Notifications";
  }

  interface ClusteringFeature {

    String CLUSTERING = "Clustering";

  }

  interface ArtifactPatchingFeature {

    String ARTIFACT_PATCHING = "Artifact Patching";

    interface ArtifactPatchingStory {

    }
  }
  interface VariablesValues {

    String VARIABLES_VALUES = "Variables";

    interface NullValue {

      String NULL_VALUE = "Null value";
    }

  }

  interface Profiling {

    String PROFILING = "Profiling";


    interface ProfilingServiceStory {

      String DEFAULT_PROFILING_SERVICE = "Default Profiling Service";

    }
  }
}
