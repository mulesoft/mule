/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.extension;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.ExpressionSupport.REQUIRED;
import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.api.meta.model.display.PathModel.Location.EMBEDDED;
import static org.mule.runtime.api.meta.model.display.PathModel.Type.FILE;
import static org.mule.runtime.api.meta.model.error.ErrorModelBuilder.newError;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.OUTPUT;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.PRIMARY_CONTENT;
import static org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder.newStereotype;
import static org.mule.runtime.api.util.MuleSystemProperties.SUPPORT_EXPRESSIONS_IN_VARIABLE_NAME_IN_SET_VARIABLE_PROPERTY;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.ANY;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.CLIENT_SECURITY;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.COMPOSITE_ROUTING;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.CONNECTIVITY;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.DUPLICATE_MESSAGE;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.EXPRESSION;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.NOT_PERMITTED;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.REDELIVERY_EXHAUSTED;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.RETRY_EXHAUSTED;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.ROUTING;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.SECURITY;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.SERVER_SECURITY;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.SOURCE;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.SOURCE_ERROR_RESPONSE_GENERATE;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.SOURCE_ERROR_RESPONSE_SEND;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.SOURCE_RESPONSE;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.SOURCE_RESPONSE_GENERATE;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.SOURCE_RESPONSE_SEND;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.STREAM_MAXIMUM_SIZE_EXCEEDED;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.TIMEOUT;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.TRANSFORMATION;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.UNKNOWN;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.VALIDATION;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Unhandleable.CRITICAL;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Unhandleable.FATAL;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Unhandleable.FLOW_BACK_PRESSURE;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Unhandleable.OVERLOAD;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.ANY_TYPE;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.BASE_TYPE_BUILDER;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.BOOLEAN_TYPE;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.INTEGER_TYPE;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULESOFT_VENDOR;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_NAME;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_VERSION;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.OBJECT_STORE_TYPE;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.STRING_TYPE;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.TYPE_LOADER;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.VOID_TYPE;
import static org.mule.runtime.extension.api.ExtensionConstants.DYNAMIC_CONFIG_EXPIRATION_DESCRIPTION;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_DESCRIPTION;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_VALUE_PARAMETER_DESCRIPTION;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_VALUE_PARAMETER_DISPLAY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_VALUE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.annotation.param.Optional.PAYLOAD;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;
import static org.mule.runtime.extension.api.error.ErrorConstants.ERROR_TYPE_DEFINITION;
import static org.mule.runtime.extension.api.error.ErrorConstants.ERROR_TYPE_MATCHER;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.APP_CONFIG;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.ERROR_HANDLER;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.FLOW;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.OBJECT_STORE;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.ON_ERROR;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.PROCESSOR;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.SERIALIZER;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.SUB_FLOW;
import static org.mule.runtime.extension.internal.loader.util.InfrastructureParameterBuilder.addReconnectionStrategyParameter;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_NAMESPACE;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_SCHEMA_LOCATION;
import static org.mule.runtime.internal.dsl.DslConstants.FLOW_ELEMENT_IDENTIFIER;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import static com.google.common.collect.ImmutableSet.of;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.UnionType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConstructDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasParametersDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NestedComponentDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NestedRouteDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclarer;
import org.mule.runtime.api.meta.model.display.ClassValueModel;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.display.PathModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.notification.NotificationListener;
import org.mule.runtime.api.scheduler.SchedulingStrategy;
import org.mule.runtime.core.api.source.scheduler.CronScheduler;
import org.mule.runtime.core.api.source.scheduler.FixedFrequencyScheduler;
import org.mule.runtime.core.internal.extension.CustomBuildingDefinitionProviderModelProperty;
import org.mule.runtime.core.privileged.extension.SingletonModelProperty;
import org.mule.runtime.extension.api.declaration.type.DynamicConfigExpirationTypeBuilder;
import org.mule.runtime.extension.api.declaration.type.annotation.TypeDslAnnotation;
import org.mule.runtime.extension.api.model.deprecated.ImmutableDeprecationModel;
import org.mule.runtime.extension.api.property.NoRedeliveryPolicyModelProperty;
import org.mule.runtime.extension.api.property.NoWrapperModelProperty;
import org.mule.runtime.extension.api.property.SinceMuleVersionModelProperty;
import org.mule.runtime.extension.api.stereotype.MuleStereotypes;
import org.mule.runtime.extension.internal.property.NoErrorMappingModelProperty;
import org.mule.runtime.extension.internal.property.TargetModelProperty;

import java.util.Map;

import com.google.gson.reflect.TypeToken;

/**
 * An {@link ExtensionDeclarer} for Mule's Core Runtime
 *
 * @since 4.0
 */
class MuleExtensionModelDeclarer {

  static final String DEFAULT_LOG_LEVEL = "INFO";
  private static final Class<? extends ModelProperty> allowsExpressionWithoutMarkersModelPropertyClass;
  private static final ClassValueModel NOTIFICATION_CLASS_VALUE_MODEL =
      new ClassValueModel(singletonList(NotificationListener.class.getName()));

  static {
    Class<? extends ModelProperty> foundClass = null;
    try {
      foundClass = (Class<? extends ModelProperty>) Class
          .forName("org.mule.runtime.module.extension.api.loader.java.property.AllowsExpressionWithoutMarkersModelProperty");
    } catch (ClassNotFoundException | SecurityException e) {
      // No custom location processing
    }
    allowsExpressionWithoutMarkersModelPropertyClass = foundClass;
  }

  final ErrorModel anyError = newError(ANY).build();
  final ErrorModel routingError = newError(ROUTING).withParent(anyError).build();
  final ErrorModel compositeRoutingError = newError(COMPOSITE_ROUTING).withParent(routingError).build();
  final ErrorModel validationError = newError(VALIDATION).withParent(anyError).build();
  final ErrorModel duplicateMessageError = newError(DUPLICATE_MESSAGE).withParent(validationError).build();

  private static boolean SUPPORT_EXPRESSIONS_IN_VARIABLE_NAME_IN_SET_VARIABLE =
      parseBoolean(getProperty(SUPPORT_EXPRESSIONS_IN_VARIABLE_NAME_IN_SET_VARIABLE_PROPERTY, "true"));

  ExtensionDeclarer createExtensionModel() {

    ExtensionDeclarer extensionDeclarer = new ExtensionDeclarer()
        .named(MULE_NAME)
        .describedAs("Mule Runtime and Integration Platform: Core components")
        .onVersion(MULE_VERSION)
        .fromVendor(MULESOFT_VENDOR)
        .withCategory(COMMUNITY)
        .withModelProperty(new CustomBuildingDefinitionProviderModelProperty())
        .withXmlDsl(XmlDslModel.builder()
            .setPrefix(CORE_PREFIX)
            .setNamespace(CORE_NAMESPACE)
            .setSchemaVersion(MULE_VERSION)
            .setXsdFileName(CORE_PREFIX + ".xsd")
            .setSchemaLocation(CORE_SCHEMA_LOCATION)
            .build());

    declareExportedTypes(extensionDeclarer);

    // constructs
    declareObject(extensionDeclarer);
    declareFlow(extensionDeclarer);
    declareSubflow(extensionDeclarer);
    declareChoice(extensionDeclarer);
    declareErrorHandler(extensionDeclarer);
    declareTry(extensionDeclarer);
    declareScatterGather(extensionDeclarer, TYPE_LOADER);
    declareParallelForEach(extensionDeclarer, TYPE_LOADER);
    declareFirstSuccessful(extensionDeclarer);
    declareRoundRobin(extensionDeclarer);
    declareConfiguration(extensionDeclarer);
    declareConfigurationProperties(extensionDeclarer);
    declareAsync(extensionDeclarer);
    declareForEach(extensionDeclarer, TYPE_LOADER);
    declareUntilSuccessful(extensionDeclarer);
    declareSecurityFilter(extensionDeclarer);

    // operations
    declareFlowRef(extensionDeclarer);
    declareIdempotentValidator(extensionDeclarer);
    declareLogger(extensionDeclarer);
    declareSetPayload(extensionDeclarer);
    declareSetVariable(extensionDeclarer);
    declareRemoveVariable(extensionDeclarer);
    declareParseTemplate(extensionDeclarer);
    declareRaiseError(extensionDeclarer);

    // sources
    declareScheduler(extensionDeclarer);

    // errors
    declareErrors(extensionDeclarer);

    // misc
    declareNotifications(extensionDeclarer);
    declareGlobalProperties(extensionDeclarer);
    declareSecurityManager(extensionDeclarer, TYPE_LOADER);

    return extensionDeclarer;
  }

  private void declareObject(ExtensionDeclarer extensionDeclarer) {
    ConstructDeclarer object = extensionDeclarer.withConstruct("object")
        .allowingTopLevelDefinition()
        .withStereotype(newStereotype("OBJECT", "MULE").withParent(APP_CONFIG).build())
        .describedAs("Element to declare a java object. Objects declared globally can be referenced from other parts of the " +
            "configuration or recovered programmatically through org.mule.runtime.api.artifact.Registry.")
        .withDeprecation(new ImmutableDeprecationModel("Only meant to be used for backwards compatibility.", "4.0", "5.0"));

    object.onDefaultParameterGroup()
        .withRequiredParameter("name")
        .asComponentId()
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("Name to use to reference this object.");

    object.onDefaultParameterGroup()
        .withOptionalParameter("ref")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("@Deprecated since 4.0. Only meant to be used for backwards compatibility. " +
            "Reference to another object defined in the mule configuration or any other provider of objects.");

    object.onDefaultParameterGroup()
        .withOptionalParameter("class")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("Creates an instance of the class provided as argument.");

    object.onDefaultParameterGroup()
        .withExclusiveOptionals(of("ref", "class"), true);

    object.onDefaultParameterGroup().withOptionalParameter("property")
        .ofType(BaseTypeBuilder.create(JAVA).objectType()
            .with(new ClassInformationAnnotation(Map.class, asList(String.class, String.class))).openWith(STRING_TYPE)
            .build())
        .withDsl(ParameterDslConfiguration.builder().allowsInlineDefinition(true)
            .allowsReferences(false).build())
        .withModelProperty(new NoWrapperModelProperty());
  }

  private void declareExportedTypes(ExtensionDeclarer extensionDeclarer) {
    extensionDeclarer.getDeclaration().addType((ObjectType) OBJECT_STORE_TYPE);
  }

  private void declareScheduler(ExtensionDeclarer extensionDeclarer) {
    SourceDeclarer scheduler = extensionDeclarer.withMessageSource("scheduler")
        .hasResponse(false)
        .describedAs("Source that schedules periodic execution of a flow.")
        .withModelProperty(NoRedeliveryPolicyModelProperty.INSTANCE);

    scheduler.withOutput().ofType(ANY_TYPE);
    scheduler.withOutputAttributes().ofType(ANY_TYPE);

    scheduler.onDefaultParameterGroup()
        .withRequiredParameter("schedulingStrategy")
        .ofType(buildSchedulingStrategyType(extensionDeclarer, TYPE_LOADER))
        .withExpressionSupport(NOT_SUPPORTED)
        .withDsl(ParameterDslConfiguration.builder()
            .allowsReferences(false)
            .allowsInlineDefinition(true)
            .allowTopLevelDefinition(false).build());

    scheduler.onDefaultParameterGroup()
        .withOptionalParameter("disallowConcurrentExecution")
        .ofType(BOOLEAN_TYPE)
        .defaultingTo(false)
        .withExpressionSupport(NOT_SUPPORTED);
  }

  /**
   * The {@code scheduling-strategy} type may be {@code cron} or {@code fixed-frequency}, and this hierarchy is implemented by
   * using subtypes. It makes the resulting XML look like this:
   *
   * <pre>
   * {
   *   &#64;code
   *   <scheduler>
   *     <scheduling-strategy>
   *       <fixed-frequency params... />
   *     </scheduling-strategy>
   *   </scheduler>
   * }
   * </pre>
   * <p>
   * If it were implemented as an {@link UnionType} (see MULE-19167) it would look like this, and it would break backwards:
   *
   * <pre>
   * {
   *   &#64;code
   *   <scheduler>
   *     <fixed-frequency params... />
   *   </scheduler>
   * }
   * </pre>
   * <p>
   * NOTE: This type is imported by the SDK in order to support polling sources at {@code PollingSourceDeclarationEnricher}.
   */
  private MetadataType buildSchedulingStrategyType(ExtensionDeclarer extensionDeclarer, ClassTypeLoader typeLoader) {
    MetadataType baseSchedulingStrategy = typeLoader.load(SchedulingStrategy.class);

    MetadataType fixedFrequencyScheduler = typeLoader.load(FixedFrequencyScheduler.class);
    MetadataType cronScheduler = typeLoader.load(CronScheduler.class);
    extensionDeclarer.withSubType(baseSchedulingStrategy, fixedFrequencyScheduler);
    extensionDeclarer.withSubType(baseSchedulingStrategy, cronScheduler);

    // workaround for an "org.mule.runtime" package and still export the type in the extension model
    extensionDeclarer.getDeclaration().addType((ObjectType) baseSchedulingStrategy);
    extensionDeclarer.getDeclaration().addType((ObjectType) fixedFrequencyScheduler);
    extensionDeclarer.getDeclaration().addType((ObjectType) cronScheduler);

    return baseSchedulingStrategy;
  }

  private void declareIdempotentValidator(ExtensionDeclarer extensionDeclarer) {
    OperationDeclarer validator = extensionDeclarer
        .withOperation("idempotentMessageValidator")
        .describedAs("Ensures that only unique messages are received by a service by checking the unique ID of the incoming message. "
            + "Note that the ID used can be generated from the message using an expression defined in the 'idExpression' "
            + "attribute. Otherwise, a 'DUPLICATE_MESSAGE' error is generated.")
        .withModelProperty(new NoErrorMappingModelProperty());

    validator.withOutput().ofType(VOID_TYPE);
    validator.withOutputAttributes().ofType(VOID_TYPE);

    validator.onDefaultParameterGroup()
        .withOptionalParameter("idExpression")
        .ofType(STRING_TYPE)
        .defaultingTo("#[correlationId]")
        .withDsl(ParameterDslConfiguration.builder().allowsReferences(false).build())
        .describedAs("The expression to use when extracting the ID from the message. "
            + "If this property is not set, '#[correlationId]' will be used by default.");

    validator.onDefaultParameterGroup()
        .withOptionalParameter("valueExpression")
        .ofType(STRING_TYPE)
        .defaultingTo("#[correlationId]")
        .describedAs("The expression to use when extracting the value from the message.");

    validator.onDefaultParameterGroup()
        .withOptionalParameter("storePrefix")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("Defines the prefix of the object store names. This will only be used for the internally built object store.");

    validator.onDefaultParameterGroup().withOptionalParameter("objectStore")
        .withDsl(ParameterDslConfiguration.builder()
            .allowsInlineDefinition(true)
            .allowsReferences(true).build())
        .ofType(OBJECT_STORE_TYPE).withExpressionSupport(NOT_SUPPORTED)
        .withAllowedStereotypes(singletonList(OBJECT_STORE))
        .describedAs("The object store where the IDs of the processed events are going to be stored. " +
            "If defined as an argument, it should reference a globally created object store. Otherwise, " +
            "it can be defined inline or not at all. In the last case, a default object store will be provided.");

    validator.withErrorModel(duplicateMessageError);
  }

  private void declareAsync(ExtensionDeclarer extensionDeclarer) {
    ConstructDeclarer async = extensionDeclarer.withConstruct("async")
        .describedAs("Processes the nested list of message processors asynchronously.");

    async.withChain().withModelProperty(NoWrapperModelProperty.INSTANCE);
    async.onDefaultParameterGroup()
        .withOptionalParameter("name")
        .withExpressionSupport(NOT_SUPPORTED)
        .ofType(STRING_TYPE)
        .describedAs("Name that will be used to identify the async scheduling tasks.");

    async.onDefaultParameterGroup()
        .withOptionalParameter("maxConcurrency")
        .describedAs("The maximum concurrency. This value determines the maximum level of parallelism that this async router can use to optimize its performance when processing messages.")
        .ofType(INTEGER_TYPE);
  }

  private void declareFlowRef(ExtensionDeclarer extensionDeclarer) {
    OperationDeclarer flowRef = extensionDeclarer.withOperation("flowRef")
        .describedAs("Allows a \u0027flow\u0027 to be referenced so that message processing will continue in the referenced flow "
            + "before returning. Message processing in the referenced \u0027flow\u0027 will occur within the context of the "
            + "referenced flow and will therefore use its error handler etc.")
        .withErrorModel(routingError)
        .withModelProperty(new NoErrorMappingModelProperty());

    flowRef.withOutput().ofType(ANY_TYPE);
    flowRef.withOutputAttributes().ofType(ANY_TYPE);

    flowRef.onDefaultParameterGroup()
        .withRequiredParameter("name")
        .ofType(STRING_TYPE)
        .withAllowedStereotypes(asList(FLOW, SUB_FLOW))
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The name of the flow to call");
  }

  private void declareLogger(ExtensionDeclarer extensionDeclarer) {
    OperationDeclarer logger = extensionDeclarer.withOperation("logger")
        .describedAs("Performs logging using an expression to determine the message to be logged. By default, if a message is not specified, the current Mule Message is logged "
            + "using the " + DEFAULT_LOG_LEVEL
            + " level to the \u0027org.mule.runtime.core.api.processor.LoggerMessageProcessor\u0027 category but "
            + "the level and category can both be configured to suit your needs.")
        .withModelProperty(new NoErrorMappingModelProperty());

    logger.withOutput().ofType(VOID_TYPE);
    logger.withOutputAttributes().ofType(VOID_TYPE);

    logger.onDefaultParameterGroup()
        .withOptionalParameter("message")
        .ofType(STRING_TYPE)
        .describedAs("The message that will be logged. Embedded expressions can be used to extract values from the current message. "
            + "If no message is specified, then the current message is used.");

    logger.onDefaultParameterGroup()
        .withOptionalParameter("level")
        .defaultingTo(DEFAULT_LOG_LEVEL)
        .ofType(BASE_TYPE_BUILDER.stringType()
            .enumOf("ERROR", "WARN", "INFO", "DEBUG", "TRACE").build())
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("Sets the log level. Default is " + DEFAULT_LOG_LEVEL + ".");

    logger.onDefaultParameterGroup()
        .withOptionalParameter("category")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("Sets the log category.");
  }

  private void declareSetPayload(ExtensionDeclarer extensionDeclarer) {
    OperationDeclarer setPayload = extensionDeclarer.withOperation("setPayload")
        .describedAs("A processor that sets the payload with the provided value.")
        .withModelProperty(new NoErrorMappingModelProperty());

    setPayload.withOutput().ofType(VOID_TYPE);
    setPayload.withOutputAttributes().ofType(VOID_TYPE);

    setPayload.onDefaultParameterGroup()
        .withRequiredParameter("value")
        .ofType(STRING_TYPE)
        .withExpressionSupport(SUPPORTED)
        .describedAs("The value to be set on the payload. Supports expressions.");

    setPayload.onDefaultParameterGroup()
        .withOptionalParameter("encoding")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The encoding of the value assigned to the payload.");

    setPayload.onDefaultParameterGroup()
        .withOptionalParameter("mimeType")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The mime type, e.g. text/plain or application/json");
  }

  private void declareSetVariable(ExtensionDeclarer extensionDeclarer) {
    OperationDeclarer setVariable = extensionDeclarer.withOperation("setVariable")
        .describedAs("A processor that adds variables.")
        .withModelProperty(new NoErrorMappingModelProperty());

    setVariable.withOutput().ofType(VOID_TYPE);
    setVariable.withOutputAttributes().ofType(VOID_TYPE);

    if (SUPPORT_EXPRESSIONS_IN_VARIABLE_NAME_IN_SET_VARIABLE) {
      setVariable.onDefaultParameterGroup()
          .withOptionalParameter("variableName")
          .ofType(STRING_TYPE)
          .withExpressionSupport(SUPPORTED)
          .describedAs("The name of the variable.");
    } else {
      setVariable.onDefaultParameterGroup()
          .withOptionalParameter("variableName")
          .ofType(STRING_TYPE)
          .withExpressionSupport(NOT_SUPPORTED)
          .describedAs("The name of the variable.");
    }

    setVariable.onDefaultParameterGroup()
        .withRequiredParameter("value")
        .ofType(STRING_TYPE)
        .withExpressionSupport(SUPPORTED)
        .describedAs("The value assigned to the variable. Supports expressions.");

    setVariable.onDefaultParameterGroup()
        .withOptionalParameter("encoding")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The encoding of the value assigned to the variable.");

    setVariable.onDefaultParameterGroup()
        .withOptionalParameter("mimeType")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The mime type of the value assigned to the variable, e.g. text/plain or application/json");
  }

  private void declareParseTemplate(ExtensionDeclarer extensionDeclarer) {
    OperationDeclarer parseTemplate = extensionDeclarer.withOperation("parseTemplate")
        .describedAs("Parses a template defined inline.")
        .withModelProperty(new NoErrorMappingModelProperty());

    parseTemplate.withOutput().ofType(ANY_TYPE);
    parseTemplate.withOutputAttributes().ofType(VOID_TYPE);

    parseTemplate.onDefaultParameterGroup()
        .withOptionalParameter("content")
        .ofType(STRING_TYPE)
        .withRole(PRIMARY_CONTENT)
        .withExpressionSupport(SUPPORTED)
        .describedAs("Template to be processed.");

    parseTemplate.onDefaultParameterGroup()
        .withOptionalParameter("location")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The location of the template. The order in which the processor will attempt to load the file is: from the file system, from a URL, then from the classpath.");

    parseTemplate.onDefaultParameterGroup()
        .withOptionalParameter("outputEncoding")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The encoding to be assigned to the result generated when parsing the template.");

    parseTemplate.onDefaultParameterGroup()
        .withOptionalParameter("outputMimeType")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The mime type to be assigned to the result generated when parsing the template, e.g. text/plain or application/json");

    parseTemplate.onDefaultParameterGroup().withExclusiveOptionals(of("content", "location"), true);
  }

  private void declareRemoveVariable(ExtensionDeclarer extensionDeclarer) {
    OperationDeclarer removeVariable = extensionDeclarer.withOperation("removeVariable")
        .describedAs("A processor that removes variables by name or a wildcard expression.")
        .withModelProperty(new NoErrorMappingModelProperty());

    removeVariable.withOutput().ofType(VOID_TYPE);
    removeVariable.withOutputAttributes().ofType(VOID_TYPE);

    removeVariable.onDefaultParameterGroup()
        .withOptionalParameter("variableName")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The variable name.");
  }

  private void declareRaiseError(ExtensionDeclarer extensionDeclarer) {
    OperationDeclarer raiseError = extensionDeclarer.withOperation("raiseError")
        .describedAs("Throws an error with the specified type and description.")
        .withModelProperty(new NoErrorMappingModelProperty());

    raiseError.withOutput().ofType(VOID_TYPE);
    raiseError.withOutputAttributes().ofType(VOID_TYPE);

    raiseError.onDefaultParameterGroup()
        .withRequiredParameter("type")
        .ofType(ERROR_TYPE_DEFINITION)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The error type to raise.");

    raiseError.onDefaultParameterGroup()
        .withOptionalParameter("description")
        .ofType(STRING_TYPE)
        .describedAs("The description of this error.");
  }

  private void declareForEach(ExtensionDeclarer extensionDeclarer, ClassTypeLoader typeLoader) {
    ConstructDeclarer forEach = extensionDeclarer.withConstruct("foreach")
        .describedAs("The foreach Processor allows iterating over a collection payload, or any collection obtained by an expression,"
            + " generating a message for each element.");

    forEach.withChain()
        .withModelProperty(NoWrapperModelProperty.INSTANCE);

    ParameterDeclarer collectionParam = forEach.onDefaultParameterGroup()
        .withOptionalParameter("collection")
        .ofType(typeLoader.load(new TypeToken<Iterable<Object>>() {

        }.getType()))
        .defaultingTo("#[payload]")
        .withExpressionSupport(REQUIRED)
        .describedAs("Expression that defines the collection to iterate over.");
    if (allowsExpressionWithoutMarkersModelPropertyClass != null) {
      try {
        collectionParam.withModelProperty(allowsExpressionWithoutMarkersModelPropertyClass.newInstance());
      } catch (InstantiationException | IllegalAccessException e) {
        // ignore
      }
    }

    forEach.onDefaultParameterGroup()
        .withOptionalParameter("batchSize")
        .ofType(INTEGER_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("Partitions the collection in sub-collections of the specified size.");

    forEach.onDefaultParameterGroup()
        .withOptionalParameter("rootMessageVariableName")
        .ofType(STRING_TYPE)
        .defaultingTo("rootMessage")
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("Variable name for the original message.");

    forEach.onDefaultParameterGroup()
        .withOptionalParameter("counterVariableName")
        .ofType(STRING_TYPE)
        .defaultingTo("counter")
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("Variable name for the item number being processed.");

  }

  private void declareUntilSuccessful(ExtensionDeclarer extensionDeclarer) {
    ConstructDeclarer untilSuccessful = extensionDeclarer.withConstruct("untilSuccessful")
        .describedAs("Attempts to route a message to its inner chain in a synchronous manner. " +
            "Routing is considered successful if no error has been raised and, optionally, if the response matches an expression.");

    untilSuccessful.withChain().withModelProperty(NoWrapperModelProperty.INSTANCE);

    untilSuccessful.onDefaultParameterGroup()
        .withOptionalParameter("maxRetries")
        .ofType(INTEGER_TYPE)
        .defaultingTo(5)
        .withExpressionSupport(SUPPORTED)
        .describedAs("Specifies the maximum number of processing retries that will be performed.");

    untilSuccessful.onDefaultParameterGroup()
        .withOptionalParameter("millisBetweenRetries")
        .ofType(INTEGER_TYPE)
        .defaultingTo(60000)
        .withExpressionSupport(SUPPORTED)
        .describedAs("Specifies the minimum time interval between two process retries in milliseconds.\n" +
            " The actual time interval depends on the previous execution but should not exceed twice this number.\n" +
            " Default value is 60000 (one minute)");
  }

  private void declareChoice(ExtensionDeclarer extensionDeclarer) {
    ConstructDeclarer choice = extensionDeclarer.withConstruct("choice")
        .describedAs("Sends the message to the first message processor whose condition is satisfied. "
            + "If none of the conditions are satisfied, it sends the message to the configured default message processor "
            + "or fails if there is none.")
        .withErrorModel(routingError);

    NestedRouteDeclarer when = choice.withRoute("when").withMinOccurs(1);
    when.withChain().withModelProperty(NoWrapperModelProperty.INSTANCE);
    ParameterDeclarer expressionParam = when.onDefaultParameterGroup()
        .withRequiredParameter("expression")
        .ofType(BOOLEAN_TYPE);

    if (allowsExpressionWithoutMarkersModelPropertyClass != null) {
      try {
        expressionParam = expressionParam
            .withModelProperty(allowsExpressionWithoutMarkersModelPropertyClass.newInstance());
      } catch (InstantiationException | IllegalAccessException e) {
        // ignore
      }
    }

    expressionParam.describedAs("The expression to evaluate.");
    choice.withRoute("otherwise").withMaxOccurs(1).withChain().withModelProperty(NoWrapperModelProperty.INSTANCE);
  }

  private void declareFlow(ExtensionDeclarer extensionDeclarer) {
    ConstructDeclarer flow = extensionDeclarer.withConstruct(FLOW_ELEMENT_IDENTIFIER)
        .allowingTopLevelDefinition()
        .withStereotype(FLOW);

    flow.onDefaultParameterGroup()
        .withRequiredParameter("name")
        .asComponentId()
        .ofType(STRING_TYPE);
    flow.onDefaultParameterGroup().withOptionalParameter("initialState").defaultingTo("started")
        .ofType(BASE_TYPE_BUILDER.stringType().enumOf("started", "stopped").build());
    flow.onDefaultParameterGroup().withOptionalParameter("maxConcurrency")
        .describedAs("The maximum concurrency. This value determines the maximum level of parallelism that the Flow can use to optimize its performance when processing messages.")
        .ofType(INTEGER_TYPE);

    flow.withOptionalComponent("source")
        .withAllowedStereotypes(MuleStereotypes.SOURCE);
    flow.withChain()
        .setRequired(true)
        .withAllowedStereotypes(PROCESSOR)
        .withModelProperty(NoWrapperModelProperty.INSTANCE);
    flow.withComponent("errorHandler")
        .withAllowedStereotypes(ERROR_HANDLER);

  }

  private void declareSubflow(ExtensionDeclarer extensionDeclarer) {
    ConstructDeclarer subFlow = extensionDeclarer.withConstruct("subFlow")
        .allowingTopLevelDefinition()
        .withStereotype(SUB_FLOW);

    subFlow.onDefaultParameterGroup()
        .withRequiredParameter("name")
        .asComponentId()
        .ofType(STRING_TYPE);
    subFlow.withChain()
        .setRequired(true)
        .withAllowedStereotypes(PROCESSOR)
        .withModelProperty(NoWrapperModelProperty.INSTANCE);
  }

  private void declareFirstSuccessful(ExtensionDeclarer extensionDeclarer) {
    ConstructDeclarer firstSuccessful = extensionDeclarer.withConstruct("firstSuccessful")
        .describedAs("Sends a message to a list of message processors until one processes it successfully.");

    firstSuccessful.withRoute("route")
        .withChain()
        .withModelProperty(NoWrapperModelProperty.INSTANCE);
  }

  private void declareRoundRobin(ExtensionDeclarer extensionDeclarer) {
    ConstructDeclarer roundRobin = extensionDeclarer.withConstruct("roundRobin")
        .describedAs("Send each message received to the next message processor in a circular list of targets.");

    roundRobin.withRoute("route")
        // it doesn't make sense for it to have less than two routes, but the XSD allows for just one.
        .withMinOccurs(1)
        .withChain()
        .withModelProperty(NoWrapperModelProperty.INSTANCE);
  }

  private void declareScatterGather(ExtensionDeclarer extensionDeclarer, ClassTypeLoader typeLoader) {
    ConstructDeclarer scatterGather = extensionDeclarer.withConstruct("scatterGather")
        .describedAs("Sends the same message to multiple message processors in parallel.")
        .withErrorModel(compositeRoutingError);

    scatterGather.withRoute("route")
        .withMinOccurs(2)
        .withChain()
        .withModelProperty(NoWrapperModelProperty.INSTANCE);

    scatterGather.onDefaultParameterGroup()
        .withOptionalParameter("timeout")
        .ofType(typeLoader.load(Long.class))
        .defaultingTo(Long.MAX_VALUE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("Sets a timeout in milliseconds for each route. Values lower or equals than zero means no timeout.");
    scatterGather.onDefaultParameterGroup()
        .withOptionalParameter("maxConcurrency")
        .ofType(INTEGER_TYPE)
        .defaultingTo(Integer.MAX_VALUE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("This value determines the maximum level of parallelism that will be used by this router.");

    scatterGather.onParameterGroup("Aggregation")
        .withOptionalParameter("collectList")
        .withRole(BEHAVIOUR)
        .withExpressionSupport(NOT_SUPPORTED)
        .ofType(BaseTypeBuilder.create(JAVA).objectType()
            .id("CollectList")
            .with(new TypeDslAnnotation(true, false, null, null))
            .build())
        .withDsl(ParameterDslConfiguration.builder()
            .allowsInlineDefinition(true)
            .allowsReferences(false)
            .allowTopLevelDefinition(false)
            .build())
        .describedAs("Strategy that determines that the results are aggregated in a list rather than on a map.");

    scatterGather.onParameterGroup(OUTPUT)
        .withOptionalParameter(TARGET_PARAMETER_NAME)
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs(TARGET_PARAMETER_DESCRIPTION)
        .withLayout(LayoutModel.builder().tabName(ADVANCED_TAB).build());
    scatterGather.onParameterGroup(OUTPUT)
        .withOptionalParameter(TARGET_VALUE_PARAMETER_NAME)
        .ofType(STRING_TYPE)
        .defaultingTo(PAYLOAD)
        .withExpressionSupport(REQUIRED)
        .describedAs(TARGET_VALUE_PARAMETER_DESCRIPTION)
        .withRole(BEHAVIOUR)
        .withDisplayModel(DisplayModel.builder().displayName(TARGET_VALUE_PARAMETER_DISPLAY_NAME).build())
        .withLayout(LayoutModel.builder().tabName(ADVANCED_TAB).build())
        .withModelProperty(new TargetModelProperty());

    // TODO MULE-13316 Define error model (Routers should be able to define error type(s) thrown in ModelDeclarer but
    // ConstructModel doesn't support it.)
  }

  private void declareParallelForEach(ExtensionDeclarer extensionDeclarer, ClassTypeLoader typeLoader) {
    ConstructDeclarer parallelForeach = extensionDeclarer.withConstruct("parallelForeach")
        .describedAs("Splits the same message and processes each part in parallel.")
        .withErrorModel(compositeRoutingError).withModelProperty(new SinceMuleVersionModelProperty("4.2.0"));

    parallelForeach.withChain().withModelProperty(NoWrapperModelProperty.INSTANCE);

    ParameterDeclarer collectionParam = parallelForeach.onDefaultParameterGroup()
        .withOptionalParameter("collection")
        .ofType(typeLoader.load(new TypeToken<Iterable<Object>>() {

        }.getType()))
        .withRole(BEHAVIOUR)
        .withExpressionSupport(REQUIRED)
        .defaultingTo("#[payload]")
        .describedAs("Expression that defines the collection of parts to be processed in parallel.");
    if (allowsExpressionWithoutMarkersModelPropertyClass != null) {
      try {
        collectionParam.withModelProperty(allowsExpressionWithoutMarkersModelPropertyClass.newInstance());
      } catch (InstantiationException | IllegalAccessException e) {
        // ignore
      }
    }

    parallelForeach.onDefaultParameterGroup()
        .withOptionalParameter("timeout")
        .ofType(typeLoader.load(Long.class))
        .defaultingTo(Long.MAX_VALUE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("Sets a timeout in milliseconds for each route. Values lower or equals than zero means no timeout.");
    parallelForeach.onDefaultParameterGroup()
        .withOptionalParameter("maxConcurrency")
        .ofType(INTEGER_TYPE)
        .defaultingTo(Integer.MAX_VALUE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("This value determines the maximum level of parallelism that will be used by this router.");

    parallelForeach.onParameterGroup(OUTPUT)
        .withOptionalParameter(TARGET_PARAMETER_NAME)
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs(TARGET_PARAMETER_DESCRIPTION)
        .withLayout(LayoutModel.builder().tabName(ADVANCED_TAB).build());
    parallelForeach.onParameterGroup(OUTPUT)
        .withOptionalParameter(TARGET_VALUE_PARAMETER_NAME)
        .ofType(STRING_TYPE)
        .defaultingTo(PAYLOAD)
        .withExpressionSupport(REQUIRED)
        .describedAs(TARGET_VALUE_PARAMETER_DESCRIPTION)
        .withRole(BEHAVIOUR)
        .withDisplayModel(DisplayModel.builder().displayName(TARGET_VALUE_PARAMETER_DISPLAY_NAME).build())
        .withLayout(LayoutModel.builder().tabName(ADVANCED_TAB).build())
        .withModelProperty(new TargetModelProperty());
  }

  private void declareTry(ExtensionDeclarer extensionDeclarer) {
    ConstructDeclarer tryScope = extensionDeclarer.withConstruct("try")
        .describedAs("Processes the nested list of message processors, "
            + "within a transaction and with it's own error handler if required.");

    tryScope.onDefaultParameterGroup()
        .withOptionalParameter("transactionalAction")
        .ofType(BASE_TYPE_BUILDER.stringType().enumOf("INDIFFERENT", "ALWAYS_BEGIN", "BEGIN_OR_JOIN").build())
        .defaultingTo("INDIFFERENT")
        .withExpressionSupport(NOT_SUPPORTED)
        .withLayout(LayoutModel.builder().tabName("Transactions").build())
        .describedAs("The action to take regarding transactions. By default nothing will be done.");

    tryScope.onDefaultParameterGroup()
        .withOptionalParameter("transactionType")
        .ofType(BASE_TYPE_BUILDER.stringType().enumOf("LOCAL", "XA").build())
        .defaultingTo("LOCAL")
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("Transaction type supported. Availability will depend on the runtime version, "
            + "though LOCAL is always available.");

    tryScope.withChain().withModelProperty(NoWrapperModelProperty.INSTANCE);
    tryScope.withOptionalComponent("errorHandler")
        .withAllowedStereotypes(ERROR_HANDLER);
  }

  private void declareErrorHandler(ExtensionDeclarer extensionDeclarer) {
    ConstructDeclarer errorHandler = extensionDeclarer.withConstruct("errorHandler")
        .withStereotype(ERROR_HANDLER)
        .allowingTopLevelDefinition()
        .describedAs("Allows the definition of internal selective handlers. It will route the error to the first handler that matches it."
            + " If there's no match, then a default error handler will be executed.");

    errorHandler.onDefaultParameterGroup()
        .withRequiredParameter("name")
        .asComponentId()
        .ofType(STRING_TYPE);

    errorHandler.onDefaultParameterGroup()
        .withOptionalParameter("ref")
        .withAllowedStereotypes(singletonList(ERROR_HANDLER))
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The name of the error handler to reuse.");

    NestedRouteDeclarer onErrorContinue = errorHandler.withRoute("onErrorContinue")
        .describedAs("Error handler used to handle errors. It will commit any transaction as if the message was consumed successfully.");
    declareOnErrorRoute(onErrorContinue);

    NestedRouteDeclarer onErrorPropagate = errorHandler.withRoute("onErrorPropagate")
        .describedAs("Error handler used to propagate errors. It will rollback any transaction and not consume messages.");
    declareOnErrorRoute(onErrorPropagate);

    errorHandler.withOptionalComponent("onError")
        .withAllowedStereotypes(ON_ERROR)
        .describedAs("Error handler used to reference others.");

    ConstructDeclarer onError = extensionDeclarer.withConstruct("onError")
        .withStereotype(ON_ERROR)
        .describedAs("Error handler used to reference others.");

    onError.onDefaultParameterGroup()
        .withRequiredParameter("ref")
        .withAllowedStereotypes(asList(ON_ERROR))
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The name of the error handler to reuse.");

    // TODO MULE-13277 errorHandler.isOneRouteRequired(true);

    // global onError's that may be referenced from an error handler
    declareGlobalOnErrorRoute(extensionDeclarer.withConstruct("onErrorContinue")
        .describedAs("Error handler used to handle errors. It will commit any transaction as if the message was consumed successfully."));
    declareGlobalOnErrorRoute(extensionDeclarer.withConstruct("onErrorPropagate")
        .describedAs("Error handler used to propagate errors. It will rollback any transaction and not consume messages."));
  }

  private void declareOnErrorRoute(NestedRouteDeclarer onError) {
    onError.withChain().withModelProperty(NoWrapperModelProperty.INSTANCE);
    declareOnErrorRouteParams(onError);
  }

  private void declareGlobalOnErrorRoute(ConstructDeclarer onError) {
    onError.withStereotype(ON_ERROR)
        .allowingTopLevelDefinition()
        .withChain();

    onError.onDefaultParameterGroup()
        .withRequiredParameter("name")
        .asComponentId()
        .ofType(STRING_TYPE);

    declareOnErrorRouteParams(onError);
  }

  private void declareOnErrorRouteParams(HasParametersDeclarer onError) {
    onError.onDefaultParameterGroup()
        .withOptionalParameter("when")
        .ofType(STRING_TYPE)
        .describedAs("The expression that will be evaluated to determine if this exception strategy should be executed. "
            + "This should always be a boolean expression.");

    onError.onDefaultParameterGroup()
        .withOptionalParameter("type")
        .ofType(ERROR_TYPE_MATCHER)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The full name of the error type to match against or a comma separated list of full names, "
            + "to match against any of them.");

    onError.onDefaultParameterGroup()
        .withOptionalParameter("logException")
        .ofType(BOOLEAN_TYPE)
        .defaultingTo(true)
        .describedAs("Determines whether the handled exception will be logged to its standard logger in the ERROR "
            + "level before being handled. Default is true.");

    onError.onDefaultParameterGroup()
        .withOptionalParameter("enableNotifications")
        .ofType(BOOLEAN_TYPE)
        .defaultingTo(true)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("Determines whether ExceptionNotifications will be fired from this strategy when an exception occurs."
            + " Default is true.");
  }

  private void declareErrors(ExtensionDeclarer extensionDeclarer) {

    final ErrorModel criticalError = newError(CRITICAL).handleable(false).build();
    final ErrorModel overloadError = newError(OVERLOAD).withParent(criticalError).build();
    final ErrorModel securityError = newError(SECURITY).withParent(anyError).build();
    final ErrorModel sourceError = newError(SOURCE).withParent(anyError).build();
    final ErrorModel sourceResponseError = newError(SOURCE_RESPONSE).withParent(anyError).build();
    final ErrorModel serverSecurityError = newError(SERVER_SECURITY).withParent(securityError).build();

    extensionDeclarer.withErrorModel(anyError);

    extensionDeclarer.withErrorModel(newError(EXPRESSION).withParent(anyError).build());
    extensionDeclarer.withErrorModel(newError(TRANSFORMATION).withParent(anyError).build());
    extensionDeclarer.withErrorModel(newError(CONNECTIVITY).withParent(anyError).build());
    extensionDeclarer.withErrorModel(newError(RETRY_EXHAUSTED).withParent(anyError).build());
    extensionDeclarer.withErrorModel(newError(REDELIVERY_EXHAUSTED).withParent(anyError).build());
    extensionDeclarer.withErrorModel(newError(STREAM_MAXIMUM_SIZE_EXCEEDED).withParent(anyError).build());
    extensionDeclarer.withErrorModel(newError(TIMEOUT).withParent(anyError).build());
    extensionDeclarer.withErrorModel(newError(UNKNOWN).handleable(false).withParent(anyError).build());

    extensionDeclarer.withErrorModel(routingError);
    extensionDeclarer.withErrorModel(compositeRoutingError);

    extensionDeclarer.withErrorModel(validationError);
    extensionDeclarer.withErrorModel(duplicateMessageError);

    extensionDeclarer.withErrorModel(securityError);
    extensionDeclarer.withErrorModel(serverSecurityError);
    extensionDeclarer.withErrorModel(newError(CLIENT_SECURITY).withParent(securityError).build());
    extensionDeclarer.withErrorModel(newError(NOT_PERMITTED).withParent(securityError).build());

    extensionDeclarer.withErrorModel(sourceError);
    extensionDeclarer.withErrorModel(sourceResponseError);
    extensionDeclarer.withErrorModel(newError(SOURCE_ERROR_RESPONSE_GENERATE).handleable(false).withParent(sourceError).build());
    extensionDeclarer.withErrorModel(newError(SOURCE_ERROR_RESPONSE_SEND).handleable(false).withParent(sourceError).build());
    extensionDeclarer.withErrorModel(newError(SOURCE_RESPONSE_GENERATE).withParent(sourceResponseError).build());
    extensionDeclarer.withErrorModel(newError(SOURCE_RESPONSE_SEND).handleable(false).withParent(sourceResponseError).build());

    extensionDeclarer.withErrorModel(criticalError);
    extensionDeclarer.withErrorModel(overloadError);
    extensionDeclarer.withErrorModel(newError(FLOW_BACK_PRESSURE).handleable(false).withParent(overloadError).build());
    extensionDeclarer.withErrorModel(newError(FATAL).handleable(false).withParent(criticalError).build());
  }

  private void declareConfiguration(ExtensionDeclarer extensionDeclarer) {
    ConstructDeclarer configuration = extensionDeclarer.withConstruct("configuration")
        .allowingTopLevelDefinition()
        .withStereotype(APP_CONFIG)
        .withModelProperty(new SingletonModelProperty(false))
        .describedAs("Specifies defaults and general settings for the Mule instance.");

    addReconnectionStrategyParameter(configuration.getDeclaration());
    declareExpressionLanguage(configuration.withOptionalComponent("expression-language"));

    final ParameterGroupDeclarer params = configuration.onDefaultParameterGroup();
    params
        .withOptionalParameter("defaultResponseTimeout")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .defaultingTo("10000")
        .describedAs("The default period (ms) to wait for a synchronous response.");

    params
        .withOptionalParameter("defaultTransactionTimeout")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .defaultingTo("30000")
        .describedAs("The default timeout (ms) for transactions. This can also be configured on transactions, "
            + "in which case the transaction configuration is used instead of this default.");

    params
        .withOptionalParameter("defaultErrorHandler-ref")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .withAllowedStereotypes(singletonList(ERROR_HANDLER))
        .describedAs("The default error handler for every flow. This must be a reference to a global error handler.")
        .withDsl(ParameterDslConfiguration.builder()
            .allowsReferences(true)
            .allowsInlineDefinition(false)
            .allowTopLevelDefinition(false)
            .build());

    params
        .withOptionalParameter("inheritIterableRepeatability")
        .ofType(BOOLEAN_TYPE)
        .defaultingTo(false)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("Whether streamed iterable objects should follow the repeatability strategy of the iterable or use the default one.")
        .withModelProperty(new SinceMuleVersionModelProperty("4.3.0"));

    params
        .withOptionalParameter("shutdownTimeout")
        .ofType(INTEGER_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .defaultingTo("5000")
        .describedAs("The time in milliseconds to wait for any in-progress work to finish running before Mule shuts down. "
            + "After this threshold has been reached, Mule starts stopping schedulers and interrupting threads, "
            + "and messages can be lost. If you have a very large number of services in the same Mule instance, "
            + "if you have components that take more than a couple seconds to process, or if you are using large "
            + "payloads and/or slower transports, you should increase this value to allow more time for graceful shutdown."
            + " The value you specify is applied to services and separately to dispatchers, so the default value of "
            + "5000 milliseconds specifies that Mule has ten seconds to process and dispatch messages gracefully after "
            + "shutdown is initiated.");

    params
        .withOptionalParameter("maxQueueTransactionFilesSize")
        .ofType(INTEGER_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .defaultingTo("500")
        .describedAs("Sets the approximate maximum space in megabytes allowed for the transaction log files for transactional persistent queues."
            + " Take into account that this number applies both to the set of transaction log files for XA and for local transactions. "
            + "If both types of transactions are used then the approximate maximum space used, will be twice the configured value.");

    params
        .withOptionalParameter("defaultObjectSerializer-ref")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .withAllowedStereotypes(singletonList(SERIALIZER))
        .describedAs("An optional reference to an ObjectSerializer to be used as the application's default")
        .withDsl(ParameterDslConfiguration.builder()
            .allowsReferences(true)
            .allowsInlineDefinition(false)
            .allowTopLevelDefinition(false)
            .build());

    params
        .withOptionalParameter("dynamicConfigExpiration")
        .describedAs(DYNAMIC_CONFIG_EXPIRATION_DESCRIPTION)
        .ofType(new DynamicConfigExpirationTypeBuilder().buildDynamicConfigExpirationType())
        .withExpressionSupport(NOT_SUPPORTED)
        .withDsl(ParameterDslConfiguration.builder()
            .allowsReferences(false)
            .allowsInlineDefinition(true)
            .allowTopLevelDefinition(false)
            .build());

    params
        .withOptionalParameter("correlationIdGeneratorExpression")
        .ofType(STRING_TYPE)
        .withExpressionSupport(REQUIRED)
        .describedAs("The default correlation id generation expression for every source. This must be DataWeave expression.");
  }

  private void declareExpressionLanguage(NestedComponentDeclarer expressionLanguageDeclarer) {
    expressionLanguageDeclarer.describedAs("Configuration of Mule Expression Language")
        .withDeprecation(new ImmutableDeprecationModel("Only meant to be used for backwards compatibility.", "4.0", "5.0"));
    expressionLanguageDeclarer.onDefaultParameterGroup()
        .withOptionalParameter("autoResolveVariables")
        .ofType(BOOLEAN_TYPE)
        .defaultingTo(true)
        .withExpressionSupport(NOT_SUPPORTED);

    ParameterGroupDeclarer importParameters = expressionLanguageDeclarer.withOptionalComponent("import")
        .onDefaultParameterGroup();
    importParameters.withOptionalParameter("name")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);
    importParameters.withRequiredParameter("class")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);

    ParameterGroupDeclarer aliasParameters = expressionLanguageDeclarer.withOptionalComponent("alias")
        .onDefaultParameterGroup();
    aliasParameters.withRequiredParameter("name")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);
    aliasParameters.withRequiredParameter("expression")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);

    ParameterGroupDeclarer globalFunctionsParameters = expressionLanguageDeclarer.withOptionalComponent("global-functions")
        .onDefaultParameterGroup();
    globalFunctionsParameters.withOptionalParameter("file")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);
  }

  private void declareConfigurationProperties(ExtensionDeclarer extensionDeclarer) {
    ConstructDeclarer configuration = extensionDeclarer.withConstruct("configurationProperties")
        .allowingTopLevelDefinition()
        .withStereotype(APP_CONFIG)
        .describedAs("Configuration properties are key/value pairs that can be stored in configuration files or set as system environment variables. "
            + "\nEach property%2Cs value can be referenced inside the attributes of a mule configuration file by wrapping the property%2Cs key name in the syntax: \n${key_name}. "
            + "\n At runtime, each property placeholder expression is substituted with the property's value. "
            + "\nThis allows you to externalize configuration of properties outside"
            + " the Mule application's deployable archive, and to allow others to change these properties based on the environment the application is being deployed to. "
            + "Note that a system environment variable with a matching key name will override the same key%2Cs value from a properties file. Each property has a key and a value. \n"
            + "The key can be referenced from the mule configuration files using the following semantics: \n"
            + "${key_name}. This allows to externalize configuration and change it based\n"
            + "on the environment the application is being deployed to.");

    configuration.onDefaultParameterGroup()
        .withRequiredParameter("file")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .withDisplayModel(DisplayModel.builder().path(new PathModel(FILE, false, EMBEDDED, new String[] {"yaml", "properties"}))
            .build())
        .describedAs(" The location of the file with the configuration properties to use. "
            + "It may be a location in the classpath or an absolute location. \nThe file location"
            + " value may also contains references to properties that will only be resolved based on "
            + "system properties or properties set at deployment time.");
  }

  private void declareNotifications(ExtensionDeclarer extensionDeclarer) {
    ConstructDeclarer notificationsConstructDeclarer = extensionDeclarer.withConstruct("notifications")
        .allowingTopLevelDefinition()
        .withStereotype(newStereotype("NOTIFICATIONS", "MULE").withParent(APP_CONFIG).build())
        .describedAs("Registers listeners for notifications and associates interfaces with particular events.")
        .withDeprecation(new ImmutableDeprecationModel("Only meant to be used for backwards compatibility.", "4.0", "5.0"));

    notificationsConstructDeclarer.onDefaultParameterGroup()
        .withOptionalParameter("dynamic")
        .ofType(BOOLEAN_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .defaultingTo(false)
        .describedAs("If the notification manager is dynamic, listeners can be registered dynamically at runtime via the "
            + "MuleContext, and the configured notification can be changed. Otherwise, some parts of Mule will cache "
            + "notification configuration for efficiency and will not generate events for newly enabled notifications or "
            + "listeners. The default value is false.");

    declareEnableNotification(notificationsConstructDeclarer.withOptionalComponent("notification"));
    declareDisableNotification(notificationsConstructDeclarer.withOptionalComponent("disable-notification"));
    declareNotificationListener(notificationsConstructDeclarer.withOptionalComponent("notification-listener"));
  }

  private void declareEnableNotification(NestedComponentDeclarer enableNotificationDeclarer) {
    enableNotificationDeclarer
        .describedAs("Registers listeners for notifications and associates interfaces with particular events");

    enableNotificationDeclarer.onDefaultParameterGroup()
        .withOptionalParameter("event-class")
        .ofType(STRING_TYPE)
        .withDisplayModel(DisplayModel.builder()
            .classValue(NOTIFICATION_CLASS_VALUE_MODEL)
            .displayName("Event class").build())
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The class associated with a notification event that will be delivered to the interface.\n"
            + "This can be used instead of the 'event' attribute to specify a custom class.");

    enableNotificationDeclarer.onDefaultParameterGroup()
        .withOptionalParameter("event")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The notification event to deliver.");

    enableNotificationDeclarer.onDefaultParameterGroup()
        .withOptionalParameter("interface-class")
        .ofType(STRING_TYPE)
        .withDisplayModel(DisplayModel.builder()
            .classValue(NOTIFICATION_CLASS_VALUE_MODEL)
            .displayName("Interface class").build())
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The interface (class name) that will receive the notification event.");

    enableNotificationDeclarer.onDefaultParameterGroup()
        .withOptionalParameter("interface")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The interface that will receive the notification event.");
  }

  private void declareDisableNotification(NestedComponentDeclarer disableNotificationDeclarer) {
    disableNotificationDeclarer.describedAs("Blocks the association of an event with a particular interface. This "
        + "filters events after the association with a particular interface (and so takes precedence).");

    disableNotificationDeclarer.onDefaultParameterGroup()
        .withOptionalParameter("event-class")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .withDisplayModel(DisplayModel.builder()
            .classValue(NOTIFICATION_CLASS_VALUE_MODEL)
            .displayName("Event class").build())
        .describedAs("The class associated with an event that will no longer be delivered to any interface. This can be "
            + "used instead of the 'event' attribute to specify a custom class.");

    disableNotificationDeclarer.onDefaultParameterGroup()
        .withOptionalParameter("event")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The event you no longer want to deliver.");

    disableNotificationDeclarer.onDefaultParameterGroup()
        .withOptionalParameter("interface-class")
        .ofType(STRING_TYPE)
        .withDisplayModel(DisplayModel.builder()
            .classValue(NOTIFICATION_CLASS_VALUE_MODEL)
            .displayName("Interface class").build())
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The interface (class name) that will no longer receive the event.");

    disableNotificationDeclarer.onDefaultParameterGroup()
        .withOptionalParameter("interface")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The interface that will no longer receive the event.");
  }

  private void declareNotificationListener(NestedComponentDeclarer notificationListenerDeclarer) {
    notificationListenerDeclarer.describedAs("Registers a bean as a listener with the notification system. Events are "
        + "dispatched by reflection - the listener will receive all events associated with any interfaces it implements."
        + " The relationship between interfaces and events is configured by the notification and disable-notification "
        + "elements.");

    notificationListenerDeclarer.onDefaultParameterGroup()
        .withRequiredParameter("ref")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The bean that will receive notifications.");

    notificationListenerDeclarer.onDefaultParameterGroup()
        .withOptionalParameter("subscription")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .defaultingTo("*")
        .describedAs("An optional string that is compared with the event resource identifier. Only events with matching"
            + " identifiers will be sent. If no value is given, all events are sent.");
  }

  private void declareGlobalProperties(ExtensionDeclarer extensionDeclarer) {
    final ConstructDeclarer globalPropDeclarer = extensionDeclarer.withConstruct("globalProperty")
        .allowingTopLevelDefinition()
        .describedAs("A global property is a named string. It can be inserted in most attribute values using standard (${key}) property placeholders.");

    globalPropDeclarer
        .onDefaultParameterGroup()
        .withRequiredParameter("name")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The name of the property. This is used inside property placeholders.");

    globalPropDeclarer
        .onDefaultParameterGroup()
        .withRequiredParameter("value")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The value of the property. This replaces each occurence of a property placeholder.");
  }

  private void declareSecurityManager(ExtensionDeclarer extensionDeclarer, ClassTypeLoader typeLoader) {
    ConstructDeclarer securityManagerDeclarer = extensionDeclarer.withConstruct("securityManager")
        .allowingTopLevelDefinition()
        .describedAs("The default security manager provides basic support for security functions. Other modules (PGP, "
            + "Spring) provide more advanced functionality.");

    ParameterGroupDeclarer customSecurityProviderParameterGroup = securityManagerDeclarer
        .withOptionalComponent("customSecurityProvider")
        .describedAs("A custom implementation of SecurityProvider.")
        .onDefaultParameterGroup();
    customSecurityProviderParameterGroup.withRequiredParameter("name")
        .describedAs("A security provider is a source of specific security-related functionality.")
        .withExpressionSupport(NOT_SUPPORTED)
        .ofType(STRING_TYPE);
    customSecurityProviderParameterGroup.withRequiredParameter("provider-ref")
        .describedAs("The name of the security provider to use.")
        .withExpressionSupport(NOT_SUPPORTED)
        .ofType(STRING_TYPE);

    ParameterGroupDeclarer customEncryptionStrategyParameterGroup = securityManagerDeclarer
        .withOptionalComponent("customEncryptionStrategy")
        .describedAs("A custom implementation of EncryptionStrategy.")
        .onDefaultParameterGroup();
    customEncryptionStrategyParameterGroup.withRequiredParameter("name")
        .describedAs("An encryption strategy provides support for a specific encryption algorithm.")
        .withExpressionSupport(NOT_SUPPORTED)
        .ofType(STRING_TYPE);
    customEncryptionStrategyParameterGroup.withRequiredParameter("strategy-ref")
        .describedAs("A reference to the encryption strategy (which may be a Spring bean that implements the "
            + "EncryptionStrategy interface).")
        .withExpressionSupport(NOT_SUPPORTED)
        .ofType(STRING_TYPE);

    ParameterGroupDeclarer secretKeyEncryptionStrategyParameterGroup = securityManagerDeclarer
        .withOptionalComponent("secretKeyEncryptionStrategy")
        .describedAs("Provides secret key-based encryption using JCE.")
        .onDefaultParameterGroup();
    secretKeyEncryptionStrategyParameterGroup.withRequiredParameter("name")
        .describedAs("An encryption strategy provides support for a specific encryption algorithm.")
        .withExpressionSupport(NOT_SUPPORTED)
        .ofType(STRING_TYPE);
    secretKeyEncryptionStrategyParameterGroup.withOptionalParameter("key")
        .describedAs("The key to use. This and the 'keyFactory-ref' attribute are mutually exclusive.")
        .withExpressionSupport(NOT_SUPPORTED)
        .ofType(STRING_TYPE);
    secretKeyEncryptionStrategyParameterGroup.withOptionalParameter("keyFactory-ref")
        .describedAs("The name of the key factory to use. This should implement the ObjectFactory interface and "
            + "return a byte array. This and the 'key' attribute are mutually exclusive.")
        .withExpressionSupport(NOT_SUPPORTED)
        .ofType(STRING_TYPE);
    secretKeyEncryptionStrategyParameterGroup.withExclusiveOptionals(of("key", "keyFactory-ref"), true);

    ParameterGroupDeclarer passwordEncryptionStrategyParameterGroup = securityManagerDeclarer
        .withOptionalComponent("passwordEncryptionStrategy")
        .describedAs("Provides password-based encryption using JCE. Users must specify a password and"
            + "optionally a salt and iteration count as well. The default algorithm is"
            + "PBEWithMD5AndDES, but users can specify any valid algorithm supported by JCE.")
        .onDefaultParameterGroup();
    passwordEncryptionStrategyParameterGroup.withRequiredParameter("name")
        .describedAs("An encryption strategy provides support for a specific encryption algorithm.")
        .withExpressionSupport(NOT_SUPPORTED)
        .ofType(STRING_TYPE);
    passwordEncryptionStrategyParameterGroup.withRequiredParameter("password")
        .describedAs("The password to use.")
        .withExpressionSupport(NOT_SUPPORTED)
        .ofType(STRING_TYPE);
    passwordEncryptionStrategyParameterGroup.withOptionalParameter("salt")
        .describedAs("The salt to use (this helps prevent dictionary attacks).")
        .withExpressionSupport(NOT_SUPPORTED)
        .ofType(STRING_TYPE);
    passwordEncryptionStrategyParameterGroup.withOptionalParameter("iterationCount")
        .describedAs("The number of iterations to use.")
        .withExpressionSupport(NOT_SUPPORTED)
        .ofType(INTEGER_TYPE);
  }

  private void declareSecurityFilter(ExtensionDeclarer extensionDeclarer) {
    ConstructDeclarer encryptionSecurityFilterDeclarer = extensionDeclarer.withConstruct("encryptionSecurityFilter")
        .describedAs("A filter that provides password-based encyption.");
    encryptionSecurityFilterDeclarer.onDefaultParameterGroup()
        .withOptionalParameter("strategy-ref")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The name of the encryption strategy to use. This should be configured using the "
            + "'password-encryption-strategy' element, inside a 'security-manager' element at the top level.");
  }

  public static void refreshSystemProperties() {
    SUPPORT_EXPRESSIONS_IN_VARIABLE_NAME_IN_SET_VARIABLE =
        parseBoolean(getProperty(SUPPORT_EXPRESSIONS_IN_VARIABLE_NAME_IN_SET_VARIABLE_PROPERTY, "true"));
  }
}
