/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.allure;

import org.mule.test.allure.AllureConstants.PricingMetricsFeature.NetworkUsageMonitoringStory;

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
    String POLICY_SILENT_DEPLOY = "Policy Silent Deploy";

    interface DeploymentSuccessfulStory {

      String DEPLOYMENT_SUCCESS = "Deployment Success";
      String POLICY_ISOLATION = "Policy Isolation from App";

    }

    interface DeploymentServiceBuilderStory {

      String DEPLOYMENT_SERVICE_BUILDER = "Deployment Service Builder";

    }

    interface DeploymentFailureStory {

      String DEPLOYMENT_FAILURE = "Deployment Failure";

    }

    interface UndeploymentFailureStory {

      String UNDEPLOYMENT = "Undeployment";

    }

    interface SingleAppDeploymentStory {

      String SINGLE_APP_DEPLOYMENT = "Single App Deployment";

    }

    interface SupportedJavaVersions {

      String JAVA_VERSIONS_IN_DEPLOYABLE_ARTIFACT =
          "Supported Java Versions are reflected in the ApplicationModel or DomainModel";

      String ENFORCE_DEPLOYABLE_ARTIFACT_JAVA_VERSION =
          "Validate that a deployable artifact supports the Java version Mule is running on";
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

  interface JpmsFeature {

    String JPMS_FEATURE = "Java Platform Module System";
  }

  interface ClassloadingIsolationFeature {

    String CLASSLOADING_ISOLATION = "Classloading Isolation";

    interface ClassloadingIsolationStory {

      String ARTIFACT_DESCRIPTORS = "Artifact descriptors";
      String PATCHED_ARTIFACT_DESCRIPTORS = "Artifact descriptors with patched plugins";
      String ARTIFACT_DESCRIPTORS_WITH_CUSTOM_LOG_CONFIG = "Artifact descriptors with a custom logging configuration";
      @Deprecated
      String CLASSLOADER_MODEL = "ClassLoader model";
      String CLASSLOADER_CONFIGURATION = "ClassLoader configuration";
      String CLASSLOADER_CONFIGURATION_LOADER = "ClassLoader configuration loader";
      String CLASSLOADER_CONFIGURATION_BUILDER = "ClassLoader configuration builder";
      String CLASSLOADER_GENERATION = "ClassLoader generation";
      String ARTIFACT_CLASSLOADERS = "Artifact class loaders";
    }

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
      String GLOBAL_ERROR_HANDLER = "Global Error Handler";
      String RAISE_ERROR = "Raise Errors";
    }
  }

  interface EventContextFeature {

    String EVENT_CONTEXT = "EventContext";

    interface EventContextStory {

      String RESPONSE_AND_COMPLETION_PUBLISHERS = "Response and completion publishers";
      String DISTRIBUTED_TRACE_CONTEXT = "Distributed Trace Context";
    }
  }

  interface ExpressionLanguageFeature {

    String EXPRESSION_LANGUAGE = "Expression Language";

    interface ExpressionLanguageStory {

      String SUPPORT_DW = "Support DW";
      String SUPPORT_FUNCTIONS = "Support Functions";
      String SUPPORT_EXPRESSION_BINDINGS = "Support expression bindings";
    }
  }

  interface ExtensionModelDiscoveryFeature {

    String EXTENSION_MODEL_DISCOVERY = "Extension Model Discovery";

    interface ExtensionModelDiscoveryStory {

      String EXTENSION_MODEL_LOADER_REPOSITORY = "Extension model loader repository";
      String PARALLEL_EXTENSION_MODEL_LOADING = "Parallel Extension Model Loading";
    }
  }

  interface ExtensionsClientFeature {

    String EXTENSIONS_CLIENT = "Extensions Client";

    interface ExtensionsClientStory {

      String BLOCKING_CLIENT = "Blocking Extension Client";
      String NON_BLOCKING_CLIENT = "Non-Blocking Extension Client";
      String MESSAGE_SOURCE = "Extension Client with Message sources";
    }
  }

  interface FileFeature {

    String FILE_EXTENSION = "File Extension";

    interface FileStory {

    }
  }

  interface ComponentsFeature {

    String CORE_COMPONENTS = "Core Components";

    interface SetPayloadStory {

      String SET_PAYLOAD = "Set Payload";

    }

    interface AddVariableStory {

      String ADD_VARIABLE = "Add Variable";

    }

    interface RemoveVariableStory {

      String REMOVE_VARIABLE = "Remove Variable";

    }

    interface FlowReferenceStory {

      String FLOW_REFERENCE = "Flow Reference";

    }

    interface LoggerStory {

      String LOGGER = "Logger";

    }

    interface ParseTemplateStory {

      String PARSE_TEMPLATE = "Parse Template";

    }

    interface IdempotentMessageValidator {

      String IDEMPOTENT_MESSAGE_VALIDATOR = "Idempotent Message Validator";

    }

  }

  interface FtpFeature {

    String FTP_EXTENSION = "FTP Extension";

    interface FtpStory {

    }
  }

  interface ReuseFeature {

    String REUSE = "Reuse";

    interface ReuseStory {

      String APPLICATION_EXTENSION_MODEL = "Application Extension Model";
      String EXTENSION_EXTENSION_MODEL = "Extension Extension Model";
      String OPERATIONS = "Operations";
      String PARAMETERS = "Parameters";
      String TYPES_CATALOG = "Types Catalog";
      String ERROR_HANDLING = "Error Handling";
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
    String SDK_OAUTH_SUPPORT = "SDK OAuth Extension";
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

      String OBJECT_STORE_DATA_STORAGE = "ObjectStore data storage";

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

      String REMOTE_WSDL_LOADING = "Remote WSDL loading";

    }

  }

  interface VMFeature {

    String VM_EXTENSION = "VM Extension";

    interface VMStory {

      String VM_QUEUES = "VM Queues";
      String PERSISTENT_DATA_REDEPLOYMENT = "Persistent data redeployment";

    }

  }

  interface ToolingSupport {

    String TOOLING_SUPPORT = "Tooling support in the Runtime";

    interface ServiceBuilderStory {

      String SERVICE_BUILDER = "Service builder";
    }
  }

  interface SdkToolingSupport {

    String SDK_TOOLING_SUPPORT = "SDK Tooling Support";

    interface ConnectivityTestingStory {

      String CONNECTIVITY_TESTING_SERVICE = "Connectivity Testing Service";

    }

    interface MetadataTypeResolutionStory {

      String METADATA_SERVICE = "Metadata Service";
      String METADATA_CACHE = "Metadata Cache";

    }

    interface ValueProvidersStory {

      String VALUE_PROVIDERS_SERVICE = "Value Providers Service";

    }

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
      String REACTOR = "Reactor";
      String MAX_CONCURRENCY = "Max concurrency";
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
    }

  }

  interface SerializationFeature {

    String SERIALIZATION = "Serialization";

    interface SerializationStory {

      String MESSAGE_SERIALIZATION = "Message Serialization";
      String STATISTICS = "Payload statistics";
    }

  }

  interface RoutersFeature {

    String ROUTERS = "Routers";

    /**
     * @deprecated {@code foreach} is a scope.
     */
    @Deprecated
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

    /**
     * @deprecated {@code until-successful} is a scope.
     */
    @Deprecated
    interface UntilSuccessfulStory {

      String UNTIL_SUCCESSFUL = "Until Successful";
    }

    /**
     * @deprecated {@code async} is a scope.
     */
    @Deprecated
    interface AsyncStory {

      String ASYNC = "Async";
    }

    interface ProcessorChainRouterStory {

      String PROCESSOR_CHAIN_ROUTER = "Processor Chain Router";
    }

    /**
     * @deprecated {@code parallel-foreach} is a scope.
     */
    @Deprecated
    interface ParallelForEachStory {

      String PARALLEL_FOR_EACH = "Parallel For Each";
    }

    interface ChoiceStory {

      String CHOICE = "Choice";
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
      String REDELIVERY = "Redelivery";
      String WATERMARK = "Watermark";
    }

  }

  interface ScopeFeature {

    String SCOPE = "Scope";

    /**
     * @deprecated {@code choice} is a router.
     */
    @Deprecated
    interface ChoiceStory {

      String CHOICE = "Choice";
    }

    interface ForeachStory {

      String FOR_EACH = "Foreach";
    }

    interface UntilSuccessfulStory {

      String UNTIL_SUCCESSFUL = "Until Successful";
    }

    interface AsyncStory {

      String ASYNC = "Async";
    }

    interface ParallelForEachStory {

      String PARALLEL_FOR_EACH = "Parallel For Each";
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

    interface ApplicationStatus {

      String APPLICATION_STATUS_STORY = "Application status";
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

    interface TimeoutStory {

      String TRANSACTION_TIMEOUT = "Transaction timeout";

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

    interface DslAnnotationsStory {

      String DSL_ANNOTATIONS_STORY = "Mule DSL Annotations";

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
      String LOG_FORMAT = "Logging Format";
      String LOGGING_LIBS_SUPPORT = "Logging Libs Support";

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

  interface FipsFeature {

    String FIPS = "Fips";

    interface Fips1403Story {

      String FIPS_140_3_STORY = "Fips 140 3 Compliance";
    }
  }
  interface LicenseFeature {

    String LICENSE = "License";

    interface LicenseManagementStory {

      String LICENSE_MANAGEMENT = "License Management";
      String EVALUATION_LICENSE = "Evaluation/trial license (ftp)";
      String TESTING_MODE_LICENSE = "Testing mode license";

    }

  }

  interface RegistryFeature {

    String REGISTRY = "Registry";

    interface ObjectRegistrationStory {

      String OBJECT_REGISTRATION = "Object Registration";

    }

    interface TransfromersStory {

      String TRANSFORMERS = "Transformers";

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

    interface ArtifactAstSerialization {

      String AST_SERIALIZATION = "AST serialization";
      String AST_SERIALIZER_METADATA_SERIALIZATION = "AST metadata serialization";
      String AST_DTO = "AST serializable representation";
      String AST_JSON_SERIALIZER = "AST Json serialization";
      String AST_JSON_DESERIALIZER = "AST Json deserialization";
      String AST_SERIALIZATION_VERSIONING = "AST serialization versioning";
      String AST_SERIALIZATION_END_TO_END = "AST serialization end to end tests";
      String AST_SERIALIZATION_ENRICH = "AST serialization enrichment";

    }

  }

  interface Sdk {

    String SDK = "SDK";

    interface Parameters {

      String EXPRESSIONS_ON_CONFIG_REF = "Expressions on config-ref parameters";

    }

    interface SupportedJavaVersions {

      String JAVA_VERSIONS_IN_EXTENSION_MODEL = "Supported Java Versions are reflected in the ExtensionModel";

      String ENFORCE_EXTENSION_JAVA_VERSION =
          "Validate that all registered extensions support the Java version Mule is running on";
    }

    interface MinMuleVersion {

      String MIN_MULE_VERSION = "Min Mule Version calculation";

    }

  }

  interface JavaSdk {

    String JAVA_SDK = "Java SDK";

    interface Parameters {

      String PARAMETERS = "Parameters definitions in Java SDK";

    }

    interface ArtifactLifecycleListener {

      String ARTIFACT_LIFECYCLE_LISTENER = "Listeners for Artifact lifecycle events";

    }

    interface ConnectivityTestingStory {

      String CONNECTIVITY_TEST = "Connectivity test";
    }

    String JAVAX_INJECT_COMPATIBILITY = "javax.inject compatibility";
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

      String DEFAULT_CORE_EVENT_TRACER = "Default Core Event Tracer";

      String DEFAULT_METRICS_PROVIDER = "Default Metrics Provider";

      String TRACING_CUSTOMIZATION = "Tracing Customization";

      String OPEN_TELEMETRY_EXPORTER = "Open Telemetry Exporter";

      String TRACING_CONFIGURATION = "Tracing Configuration";

      String METRICS_EXPORTER = "Metrics Exporter";

      String METRICS_IMPLEMENTATION = "Metrics Implementation";
    }
  }

  interface MemoryManagement {

    String MEMORY_MANAGEMENT = "Memory Management";

    interface MemoryManagementServiceStory {

      String DEFAULT_MEMORY_MANAGEMENT_SERVICE = "Default Memory Management Service";

      String BYTE_BUFFER_PROVIDER = "Byte Buffer Provider";
    }
  }

  interface ObjectTag {

    String OBJECT_TAG_FEATURE = "Object";

    interface ObjectPropertiesStory {

      String OBJECT_PROPERTIES = "Object properties";
    }
  }

  interface ObjectSerializer {

    String CUSTOM_OBJECT_SERIALIZER = "Custom object serializer";
  }

  interface PoliciesEngineFeature {

    String POLICIES_ENGINE = "Policies Engine";

    interface OperationPolicyStory {

      String OPERATION_POLICIES = "Operation policies";
    }
  }

  interface PricingMetricsFeature {

    String PRICING_METRICS = "Pricing Metrics";

    interface NetworkUsageMonitoringStory {

      String MONITOR_NET_BYTES_USED = "Monitor net bytes used";
    }

    interface MessageMetricsStory {

      String LAPSED_MESSAGE_METRICS = "Lapsed message metrics";
    }

    interface FlowSummaryStory {

      String ACTIVE_FLOWS_SUMMARY = "Active flows summary";
      String DETECT_APIKIT_FLOWS_AS_TRIGGERS = "Detect APiKit flows as triggers";

    }
  }

  /**
   * @deprecated Use {@link PricingMetricsFeature} or {@link NetworkUsageMonitoringStory} instead.
   */
  @Deprecated
  interface MonitoringSideCar {

    String MONITOR_NET_BYTES_USED = "Monitor net bytes used";

    /**
     * @deprecated Use {@link PricingMetricsFeature} or {@link NetworkUsageMonitoringStory} instead.
     */
    @Deprecated
    interface MonitoringSideCarStory {

      String ANYPOINT_MONITORING_SIDECAR = "Anypoint monitoring sidecar";
    }
  }

  interface DeployableCreationFeature {

    String APP_CREATION = "Application creation";
    String DOMAIN_CREATION = "Domain creation";

  }

  interface SplashScreenFeature {

    String SPLASH_SCREEN = "Splash screen";
  }

  interface DescriptorLoaderFeature {

    String DESCRIPTOR_LOADER = "Descriptor loader";
  }

  interface ServicesFeature {

    String SERVICES = "Services";

    interface ServicesStory {

      String SERVICE_REGISTRY = "Service registry";
      String SERVICE_PROVIDER_DISCOVERER = "Service provider discoverer";
    }
  }

  interface SupportedEnvironmentsFeature {

    String SUPPORTED_ENVIRONMENTS = "Supported environments";

    interface JdkVersionStory {

      String JDK_VERSION = "JDK version";
      String JDK_ENVIRONMENT_CONFIGURATION = "JDK environment configuration";
    }
  }

  interface WrapperlessBootstrapFeature {

    String WRAPPERLESS_BOOTSTRAP = "Wrapper-less bootstrapping";

    interface WrapperlessBootstrapStory {

      String WRAPPERLESS_CONTAINER_MANAGEMENT = "Managing a container bootstrapped in wrapper-less mode";
      String WRAPPERLESS_PARAMETERS_RESOLUTION = "Resolution of parameters to use in a wrapper-less bootstrapping";
    }
  }

  interface CoreExtensionsFeature {

    String CORE_EXTENSIONS = "Core Extensions";

    interface CoreExtensionsStory {

      String CORE_EXTENSIONS_DEPENDENCY_INJECTION = "Dependency injection for core extensions";

    }
  }

  interface LockFactoryFeature {

    String LOCK_FACTORY = "Lock factory";

    interface LockFactoryStory {

      String SERVER_LOCK_FACTORY = "Container level lock factory";
    }
  }

  interface MuleContextFeature {

    String MULE_CONTEXT = "Mule Context";

    interface MuleContextCreationStory {

      String MULE_CONTEXT_CREATION = "Mule Context creation";
    }
  }

  interface CustomizationServiceFeature {

    String CUSTOMIZATION_SERVICE = "Customization Service";
  }

  interface MuleManifestFeature {

    String MULE_MANIFEST = "Mule Manifest";
  }
}
