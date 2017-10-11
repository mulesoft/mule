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

    interface DbStory {

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
      String APPLICATION_PATCHING = "Application Redeployment";

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

    interface FlowReferenceStory {

      String FLOW_REFERENCE = "Flow Reference";

    }

    interface LoggerStory {

      String LOGGER = "Logger";

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

    interface OauthStory {
    }

  }

  interface ObjectStoreFeature {

    String OS_EXTENSION = "ObjectStore Extension";

    interface ObjectStoreStory {

      String PERSISTENT_DATA_REDEPLOYMENT = "Persistent data redeployment";

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
    }

  }

  interface ForkJoinStrategiesFeature {

    String FORK_JOIN_STRATEGIES = "Fork/Join Strategies used by scatter-gather and foreach routers";

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

  interface SchedulerFeature {

    String SCHEDULER = "Scheduler";

  }

  interface StreamingFeature {

    String STREAMING = "Streaming";

    interface StreamingStory {

      String BYTES_STREAMING = "Bytes Streaming";
      String OBJECT_STREAMING = "Object Streaming";
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

    interface ProcessorChainRouterStory {

      String PROCESSOR_CHAIN_ROUTER = "Processor Chain Router";
    }
  }

  interface TransformMessageFeature {

    String TRANSFORM_MESSAGE = "Transform Message";

    interface TransformMessageStory {
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

      String ARTIFACT_OBJECTS_DEPENDENCY_INJECTION_STORY = "Artifact Objects Dependency injection Store";
    }

  }

  interface TransactionFeature {

    String TRANSACTION = "Transaction";

    interface LocalStory {

      String LOCAL_TRANSACTION = "Local Transaction";
    }

  }

  interface MuleDsl {

    String MULE_DSL = "Mule DSL";

    interface DslParsingStory {

      String DSL_PARSING_STORY = "Mule DSL Parsing";

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

  interface InterceptonApi {

    String INTERCEPTION_API = "Interception API";

    interface ComponentInterceptionStory {

      String COMPONENT_INTERCEPTION_STORY = "Component Interception Story";

    }

  }

  interface RuntimeGlobalConfiguration {

    String RUNTIME_GLOBAL_CONFIGURATION = "Runtime Global Configuration";

    interface MavenGlobalConfiguration {

      String MAVEN_GLOBAL_CONFIGURATION_STORY = "Maven Global Configuration Story";

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


  interface XmlSdk {

    String XML_SDK = "Xml SDK";

    interface Streaming {

      String STREAMING = "Streaming consumption in operations";

    }

    interface Declaration {

      String DECLARATION_DATASENSE = "Declaration override of calculated metadata types";

    }

  }

}
