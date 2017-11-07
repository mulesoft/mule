/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.extension;

import static java.util.Collections.singletonList;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.ExpressionSupport.REQUIRED;
import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.api.meta.model.display.PathModel.Type.FILE;
import static org.mule.runtime.api.meta.model.error.ErrorModelBuilder.newError;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.ANY;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.CLIENT_SECURITY;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.COMPOSITE_ROUTING;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.CONNECTIVITY;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.DUPLICATE_MESSAGE;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.EXPRESSION;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.NOT_PERMITTED;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.REDELIVERY_EXHAUSTED;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.RETRY_EXHAUSTED;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.ROUTING;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SECURITY;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SERVER_SECURITY;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SOURCE;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SOURCE_ERROR_RESPONSE_GENERATE;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SOURCE_ERROR_RESPONSE_SEND;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SOURCE_RESPONSE;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SOURCE_RESPONSE_GENERATE;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SOURCE_RESPONSE_SEND;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.STREAM_MAXIMUM_SIZE_EXCEEDED;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.TIMEOUT;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.TRANSFORMATION;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.UNKNOWN;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.VALIDATION;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Unhandleable.CRITICAL;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Unhandleable.FATAL;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Unhandleable.OVERLOAD;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULESOFT_VENDOR;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_NAME;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_VERSION;
import static org.mule.runtime.extension.api.ExtensionConstants.DYNAMIC_CONFIG_EXPIRATION_DESCRIPTION;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_DESCRIPTION;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_VALUE_PARAMETER_DESCRIPTION;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_VALUE_PARAMETER_DISPLAY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_VALUE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.annotation.param.Optional.PAYLOAD;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.APP_CONFIG;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.ERROR_HANDLER;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.FLOW;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.OBJECT_STORE;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.PROCESSOR;
import static org.mule.runtime.extension.internal.loader.util.InfrastructureParameterBuilder.addReconnectionStrategyParameter;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_NAMESPACE;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.FLOW_ELEMENT_IDENTIFIER;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConstructDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NestedRouteDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclarer;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.display.PathModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.parameter.ParameterRole;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.core.api.source.scheduler.CronScheduler;
import org.mule.runtime.core.api.source.scheduler.FixedFrequencyScheduler;
import org.mule.runtime.core.api.source.scheduler.Scheduler;
import org.mule.runtime.core.internal.extension.CustomBuildingDefinitionProviderModelProperty;
import org.mule.runtime.extension.api.declaration.type.DynamicConfigExpirationTypeBuilder;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.stereotype.MuleStereotypes;
import org.mule.runtime.extension.internal.property.TargetModelProperty;

import com.google.gson.reflect.TypeToken;

/**
 * An {@link ExtensionDeclarer} for Mule's Core Runtime
 *
 * @since 4.0
 */
class MuleExtensionModelDeclarer {

  final ErrorModel anyError = newError(ANY).build();
  final ErrorModel routingError = newError(ROUTING).withParent(anyError).build();
  final ErrorModel compositeRoutingError = newError(COMPOSITE_ROUTING).withParent(routingError).build();
  final ErrorModel validationError = newError(VALIDATION).withParent(anyError).build();
  final ErrorModel duplicateMessageError = newError(DUPLICATE_MESSAGE).withParent(validationError).build();

  ExtensionDeclarer createExtensionModel() {

    final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault()
        .createTypeLoader(MuleExtensionModelDeclarer.class.getClassLoader());

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
            .setXsdFileName("mule.xsd")
            .setSchemaLocation("http://www.mulesoft.org/schema/mule/core/current/mule.xsd")
            .build());

    // constructs
    declareFlow(extensionDeclarer, typeLoader);
    declareSubflow(extensionDeclarer);
    declareChoice(extensionDeclarer, typeLoader);
    declareErrorHandler(extensionDeclarer, typeLoader);
    declareTry(extensionDeclarer, typeLoader);
    declareScatterGather(extensionDeclarer, typeLoader);
    declareFirstSuccessful(extensionDeclarer);
    declareRoundRobin(extensionDeclarer);
    declareConfiguration(extensionDeclarer, typeLoader);
    declareConfigurationProperties(extensionDeclarer, typeLoader);
    declareAsync(extensionDeclarer, typeLoader);
    declareForEach(extensionDeclarer, typeLoader);
    declareUntilSuccessful(extensionDeclarer, typeLoader);

    // operations
    declareFlowRef(extensionDeclarer, typeLoader);
    declareIdempotentValidator(extensionDeclarer, typeLoader);
    declareLogger(extensionDeclarer, typeLoader);
    declareSetPayload(extensionDeclarer, typeLoader);
    declareSetVariable(extensionDeclarer, typeLoader);
    declareRemoveVariable(extensionDeclarer, typeLoader);
    declareParseTemplate(extensionDeclarer, typeLoader);
    declareRaiseError(extensionDeclarer, typeLoader);

    // sources
    declareScheduler(extensionDeclarer, typeLoader);

    // errors
    declareErrors(extensionDeclarer);

    return extensionDeclarer;
  }

  private void declareScheduler(ExtensionDeclarer extensionDeclarer, ClassTypeLoader typeLoader) {
    SourceDeclarer scheduler = extensionDeclarer.withMessageSource("scheduler")
        .hasResponse(false)
        .describedAs("Source that schedules periodic execution of a flow.");

    scheduler.withOutput().ofType(typeLoader.load(Object.class));
    scheduler.withOutputAttributes().ofType(typeLoader.load(Object.class));

    MetadataType baseSchedulingStrategy = typeLoader.load(Scheduler.class);
    scheduler.onDefaultParameterGroup()
        .withRequiredParameter("schedulingStrategy")
        .ofType(baseSchedulingStrategy)
        .withExpressionSupport(NOT_SUPPORTED);

    MetadataType fixedFrequencyScheduler = typeLoader.load(FixedFrequencyScheduler.class);
    MetadataType cronScheduler = typeLoader.load(CronScheduler.class);
    extensionDeclarer.withSubType(baseSchedulingStrategy, fixedFrequencyScheduler);
    extensionDeclarer.withSubType(baseSchedulingStrategy, cronScheduler);

    // workaround for an "org.mule.runtime" package and still export the type in the extension model
    extensionDeclarer.getDeclaration().addType((ObjectType) baseSchedulingStrategy);
    extensionDeclarer.getDeclaration().addType((ObjectType) fixedFrequencyScheduler);
    extensionDeclarer.getDeclaration().addType((ObjectType) cronScheduler);

  }

  private void declareIdempotentValidator(ExtensionDeclarer extensionDeclarer, ClassTypeLoader typeLoader) {
    OperationDeclarer validator = extensionDeclarer
        .withOperation("idempotentMessageValidator")
        .describedAs("Ensures that only unique messages are received by a service by checking the unique ID of the incoming message. "
            + "Note that the ID used can be generated from the message using an expression defined in the 'idExpression' "
            + "attribute. Otherwise, a 'DUPLICATE_MESSAGE' error is generated.");

    validator.withOutput().ofType(typeLoader.load(void.class));
    validator.withOutputAttributes().ofType(typeLoader.load(void.class));

    validator.onDefaultParameterGroup()
        .withOptionalParameter("idExpression")
        .ofType(typeLoader.load(String.class))
        .defaultingTo("#[id]")
        .withDsl(ParameterDslConfiguration.builder().allowsReferences(false).build())
        .describedAs("Defines one or more expressions to use when extracting the ID from the message. "
            + "If this property is not set, '#[id]' will be used by default.");

    validator.onDefaultParameterGroup()
        .withOptionalParameter("valueExpression")
        .ofType(typeLoader.load(String.class))
        .defaultingTo("#[id]")
        .describedAs("Defines one or more expressions to use when extracting the value from the message.");

    validator.onDefaultParameterGroup()
        .withOptionalParameter("storePrefix")
        .ofType(typeLoader.load(String.class))
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("Defines the prefix of the object store names. This will only be used for the internally built object store.");

    validator.onDefaultParameterGroup().withOptionalParameter("objectStore").withDsl(
                                                                                     ParameterDslConfiguration.builder()
                                                                                         .allowsInlineDefinition(true)
                                                                                         .allowsReferences(true).build())
        .ofType(typeLoader.load(ObjectStore.class)).withExpressionSupport(NOT_SUPPORTED)
        .withAllowedStereotypes(singletonList(OBJECT_STORE))
        .describedAs("The object store where the IDs of the processed events are going to be stored. " +
            "If defined as argument it should reference a globally created object store. Otherwise, " +
            "it can be defined inline or not at all. In the last case, a default object store will be provided.");

    validator.withErrorModel(duplicateMessageError);
  }

  private void declareAsync(ExtensionDeclarer extensionDeclarer, ClassTypeLoader typeLoader) {
    ConstructDeclarer async = extensionDeclarer.withConstruct("async")
        .describedAs("Processes the nested list of message processors asynchronously using a thread pool");

    async.withChain();
    async.onDefaultParameterGroup()
        .withOptionalParameter("name")
        .withExpressionSupport(NOT_SUPPORTED)
        .ofType(typeLoader.load(String.class))
        .describedAs("Name that will be used to name the async scheduler");

  }

  private void declareFlowRef(ExtensionDeclarer extensionDeclarer, ClassTypeLoader typeLoader) {

    OperationDeclarer flowRef = extensionDeclarer.withOperation("flowRef")
        .describedAs("Allows a \u0027flow\u0027 to be referenced such that the message processing will continue in the referenced flow "
            + "before returning. Message processing in the referenced \u0027flow\u0027 will occur within the context of the "
            + "referenced flow and will therefore use its exception strategy etc.")
        .withErrorModel(routingError);

    flowRef.withOutput().ofType(BaseTypeBuilder.create(JAVA).anyType().build());
    flowRef.withOutputAttributes().ofType(BaseTypeBuilder.create(JAVA).anyType().build());

    flowRef.onDefaultParameterGroup()
        .withRequiredParameter("name")
        .ofType(typeLoader.load(String.class))
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The name of the flow to call");
  }

  private void declareLogger(ExtensionDeclarer extensionDeclarer, ClassTypeLoader typeLoader) {
    OperationDeclarer logger = extensionDeclarer.withOperation("logger")
        .describedAs("Performs logging using an expression that determines what should be logged. By default the current messages is logged "
            + "using the DEBUG level to the \u0027org.mule.runtime.core.api.processor.LoggerMessageProcessor\u0027 category but "
            + "the level and category can both be configured to suit your needs. message is specified then the current message is used.");

    logger.withOutput().ofType(typeLoader.load(void.class));
    logger.withOutputAttributes().ofType(typeLoader.load(void.class));

    logger.onDefaultParameterGroup()
        .withOptionalParameter("message")
        .ofType(typeLoader.load(String.class))
        .describedAs("Message that will be logged. Embedded expressions can be used to extract value from the current message. "
            + "If no message is specified then the current message is used.");

    logger.onDefaultParameterGroup()
        .withOptionalParameter("level")
        .defaultingTo("DEBUG")
        .ofType(BaseTypeBuilder.create(JAVA).stringType()
            .enumOf("ERROR", "WARN", "INFO", "DEBUG", "TRACE").build())
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The logging level to be used. Default is DEBUG.");

    logger.onDefaultParameterGroup()
        .withOptionalParameter("category")
        .ofType(typeLoader.load(String.class))
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The log category to be used");

  }

  private void declareSetPayload(ExtensionDeclarer extensionDeclarer, ClassTypeLoader typeLoader) {
    OperationDeclarer setPayload = extensionDeclarer.withOperation("setPayload")
        .describedAs("A transformer that sets the payload with the provided value.");

    setPayload.withOutput().ofType(typeLoader.load(void.class));
    setPayload.withOutputAttributes().ofType(typeLoader.load(void.class));

    setPayload.onDefaultParameterGroup()
        .withOptionalParameter("encoding")
        .ofType(typeLoader.load(String.class))
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The encoding of the value assigned to the payload.");

    setPayload.onDefaultParameterGroup()
        .withRequiredParameter("value")
        .ofType(typeLoader.load(String.class))
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The value to be set on the payload. Supports expressions.");

    setPayload.onDefaultParameterGroup()
        .withOptionalParameter("mimeType")
        .ofType(typeLoader.load(String.class))
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The mime type, e.g. text/plain or application/json");

  }

  private void declareSetVariable(ExtensionDeclarer extensionDeclarer, ClassTypeLoader typeLoader) {
    OperationDeclarer setVariable = extensionDeclarer.withOperation("setVariable")
        .describedAs("A processor that adds variables.");

    setVariable.withOutput().ofType(typeLoader.load(void.class));
    setVariable.withOutputAttributes().ofType(typeLoader.load(void.class));

    setVariable.onDefaultParameterGroup()
        .withOptionalParameter("variableName")
        .ofType(typeLoader.load(String.class))
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The variable name.");

    setVariable.onDefaultParameterGroup()
        .withRequiredParameter("value")
        .ofType(typeLoader.load(String.class))
        .withExpressionSupport(SUPPORTED)
        .describedAs("The variable value.");

    setVariable.onDefaultParameterGroup()
        .withOptionalParameter("encoding")
        .ofType(typeLoader.load(String.class))
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The encoding of the value assigned to the payload.");

    setVariable.onDefaultParameterGroup()
        .withOptionalParameter("mimeType")
        .ofType(typeLoader.load(String.class))
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The mime type, e.g. text/plain or application/json");
  }

  private void declareParseTemplate(ExtensionDeclarer extensionDeclarer, ClassTypeLoader typeLoader) {
    OperationDeclarer parseTemplate = extensionDeclarer.withOperation("parseTemplate")
        .describedAs("A transformer that parses a template defined inline.");

    parseTemplate.withOutput().ofType(typeLoader.load(String.class));
    parseTemplate.withOutputAttributes().ofType(typeLoader.load(void.class));

    parseTemplate.onDefaultParameterGroup()
        .withOptionalParameter("content")
        .ofType(typeLoader.load(String.class))
        .withRole(ParameterRole.PRIMARY_CONTENT)
        .withExpressionSupport(SUPPORTED)
        .describedAs("Template to be processed.");

    parseTemplate.onDefaultParameterGroup()
        .withOptionalParameter("location")
        .ofType(typeLoader.load(String.class))
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The location of the template. The order in which the transformer will attempt to load the file are: from the file system, from a URL or from the classpath.");

  }

  private void declareRemoveVariable(ExtensionDeclarer extensionDeclarer, ClassTypeLoader typeLoader) {
    OperationDeclarer removeVariable = extensionDeclarer.withOperation("removeVariable")
        .describedAs("A processor that remove variables by name or regular expression.");

    removeVariable.withOutput().ofType(typeLoader.load(void.class));
    removeVariable.withOutputAttributes().ofType(typeLoader.load(void.class));

    removeVariable.onDefaultParameterGroup()
        .withOptionalParameter("variableName")
        .ofType(typeLoader.load(String.class))
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The variable name.");

  }

  private void declareRaiseError(ExtensionDeclarer extensionDeclarer, ClassTypeLoader typeLoader) {
    OperationDeclarer raiseError = extensionDeclarer.withOperation("raiseError")
        .describedAs("Throws an error with the specified type and description.");

    raiseError.withOutput().ofType(typeLoader.load(void.class));
    raiseError.withOutputAttributes().ofType(typeLoader.load(void.class));

    raiseError.onDefaultParameterGroup()
        .withRequiredParameter("type")
        .ofType(typeLoader.load(String.class))
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The error type to raise.");

    raiseError.onDefaultParameterGroup()
        .withOptionalParameter("description")
        .ofType(typeLoader.load(String.class))
        .describedAs("The description of this error.");
  }

  private void declareForEach(ExtensionDeclarer extensionDeclarer, ClassTypeLoader typeLoader) {

    ConstructDeclarer forEach = extensionDeclarer.withConstruct("foreach")
        .describedAs("The foreach Processor allows iterating over a collection payload, or any collection obtained by an expression,"
            + " generating a message for each element.");

    forEach.withChain();

    forEach.onDefaultParameterGroup()
        .withOptionalParameter("collection")
        .ofType(typeLoader.load(new TypeToken<Iterable<Object>>() {

        }.getType()))
        .defaultingTo("#[payload]")
        .withExpressionSupport(REQUIRED)
        .describedAs("An expression to that returns a java collection, object array, map or DOM nodes.");

    forEach.onDefaultParameterGroup()
        .withOptionalParameter("batchSize")
        .ofType(typeLoader.load(Integer.class))
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("An expression to that returns a java collection, object array, map or DOM nodes.");

    forEach.onDefaultParameterGroup()
        .withOptionalParameter("rootMessageVariableName")
        .ofType(typeLoader.load(String.class))
        .defaultingTo("rootMessage")
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("Property name where the parent message is stored.");

    forEach.onDefaultParameterGroup()
        .withOptionalParameter("counterVariableName")
        .ofType(typeLoader.load(String.class))
        .defaultingTo("counter")
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("Property name used to store the number of message being iterated.");

  }

  private void declareUntilSuccessful(ExtensionDeclarer extensionDeclarer, ClassTypeLoader typeLoader) {
    ConstructDeclarer untilSuccessful = extensionDeclarer.withConstruct("untilSuccessful")
        .describedAs("Attempts to route a message to the message processor it contains in an asynchronous manner. " +
            "Routing is considered successful if no exception has been raised and, optionally, if the response matches an expression");

    untilSuccessful.withChain();

    untilSuccessful.onDefaultParameterGroup()
        .withOptionalParameter("maxRetries")
        .ofType(typeLoader.load(Integer.class))
        .defaultingTo(5)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("Specifies the maximum number of processing retries that will be performed.");

    untilSuccessful.onDefaultParameterGroup()
        .withOptionalParameter("millisBetweenRetries")
        .ofType(typeLoader.load(Integer.class))
        .defaultingTo(60000)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("Specifies the minimum time interval between two process retries in milliseconds.\n" +
            " The actual time interval depends on the previous execution but should not exceed twice this number.\n" +
            " Default value is 60000 (one minute)");
  }

  private void declareChoice(ExtensionDeclarer extensionDeclarer, ClassTypeLoader typeLoader) {
    ConstructDeclarer choice = extensionDeclarer.withConstruct("choice")
        .describedAs("Sends the message to the first message processor whose condition has been satisfied. "
            + "If no conditions were satisfied, sends to the configured default message processor if configured, "
            + "or throws an exception if not configured.")
        .withErrorModel(routingError);

    NestedRouteDeclarer when = choice.withRoute("when").withMinOccurs(1);
    when.withChain();
    when.onDefaultParameterGroup()
        .withRequiredParameter("expression")
        .ofType(typeLoader.load(boolean.class))
        .describedAs("The expression to evaluate.");

    choice.withRoute("otherwise").withMaxOccurs(1).withChain();
  }

  private void declareFlow(ExtensionDeclarer extensionDeclarer, ClassTypeLoader typeLoader) {
    ConstructDeclarer flow = extensionDeclarer.withConstruct(FLOW_ELEMENT_IDENTIFIER)
        .allowingTopLevelDefinition()
        .withStereotype(FLOW);

    flow.onDefaultParameterGroup().withOptionalParameter("initialState").defaultingTo("started")
        .ofType(BaseTypeBuilder.create(JAVA).stringType().enumOf("started", "stopped").build());
    flow.onDefaultParameterGroup().withOptionalParameter("maxConcurrency")
        .ofType(typeLoader.load(Integer.class));

    flow.withComponent("source")
        .withAllowedStereotypes(MuleStereotypes.SOURCE);
    flow.withChain().setRequired(true);
    flow.withComponent("errorHandler")
        .withAllowedStereotypes(ERROR_HANDLER);

  }

  private void declareSubflow(ExtensionDeclarer extensionDeclarer) {
    ConstructDeclarer flow = extensionDeclarer.withConstruct("subFlow")
        .allowingTopLevelDefinition();

    flow.withChain().setRequired(true).withAllowedStereotypes(PROCESSOR);
  }

  private void declareFirstSuccessful(ExtensionDeclarer extensionDeclarer) {
    ConstructDeclarer firstSuccessful = extensionDeclarer.withConstruct("firstSuccessful")
        .describedAs("Sends a message to a list of message processors until one processes it successfully.");

    firstSuccessful.withRoute("route").withChain();
  }

  private void declareRoundRobin(ExtensionDeclarer extensionDeclarer) {
    ConstructDeclarer roundRobin = extensionDeclarer.withConstruct("roundRobin")
        .describedAs("Send each message received to the next message processor in the circular list of targets.");

    roundRobin.withRoute("route").withChain();
  }

  private void declareScatterGather(ExtensionDeclarer extensionDeclarer, ClassTypeLoader typeLoader) {
    ConstructDeclarer scatterGather = extensionDeclarer.withConstruct("scatterGather")
        .describedAs("Sends the same message to multiple message processors in parallel.")
        .withErrorModel(compositeRoutingError);

    scatterGather.withRoute("route").withMinOccurs(2).withChain();

    scatterGather.onDefaultParameterGroup()
        .withOptionalParameter("timeout")
        .ofType(typeLoader.load(Long.class))
        .defaultingTo(Long.MAX_VALUE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("Sets a timeout in milliseconds for each route. Values lower or equals than zero means no timeout.");
    scatterGather.onDefaultParameterGroup()
        .withOptionalParameter("maxConcurrency")
        .ofType(typeLoader.load(Integer.class))
        .defaultingTo(Integer.MAX_VALUE)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("This value determines the maximum level of parallelism that will be used by this router. .");
    scatterGather.onDefaultParameterGroup()
        .withOptionalParameter(TARGET_PARAMETER_NAME)
        .ofType(typeLoader.load(String.class))
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs(TARGET_PARAMETER_DESCRIPTION)
        .withLayout(LayoutModel.builder().tabName(ADVANCED_TAB).build());

    scatterGather.onDefaultParameterGroup()
        .withOptionalParameter(TARGET_VALUE_PARAMETER_NAME)
        .ofType(typeLoader.load(String.class))
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

  private void declareTry(ExtensionDeclarer extensionDeclarer, ClassTypeLoader typeLoader) {
    ConstructDeclarer tryScope = extensionDeclarer.withConstruct("try")
        .describedAs("Processes the nested list of message processors, "
            + "within a transaction and with it's own error handler if required.");

    tryScope.onDefaultParameterGroup()
        .withOptionalParameter("transactionalAction")
        .ofType(BaseTypeBuilder.create(JAVA).stringType()
            .enumOf("INDIFFERENT", "ALWAYS_BEGIN", "BEGIN_OR_JOIN").build())
        .defaultingTo("INDIFFERENT")
        .withExpressionSupport(NOT_SUPPORTED)
        .withLayout(LayoutModel.builder().tabName("Transactions").build())
        .describedAs("The action to take regarding transactions. By default nothing will be done.");

    tryScope.onDefaultParameterGroup()
        .withOptionalParameter("transactionType")
        .ofType(BaseTypeBuilder.create(JAVA).stringType().enumOf("LOCAL", "XA").build())
        .defaultingTo("LOCAL")
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("Transaction type supported. Availability will depend on the runtime version, "
            + "though LOCAL is always available.");

    tryScope.withChain();
    tryScope.withOptionalComponent("errorHandler")
        .withAllowedStereotypes(ERROR_HANDLER);
  }

  private void declareErrorHandler(ExtensionDeclarer extensionDeclarer, ClassTypeLoader typeLoader) {
    ConstructDeclarer errorHandler = extensionDeclarer.withConstruct("errorHandler")
        .withStereotype(ERROR_HANDLER)
        .allowingTopLevelDefinition()
        .describedAs("Allows the definition of internal selective handlers. It will route the error to the first handler that matches it."
            + " If there's no match, then a default error handler will be executed.");

    NestedRouteDeclarer onErrorContinue = errorHandler.withRoute("onErrorContinue")
        .describedAs(
                     "Error handler used to handle exceptions. It will commit any transaction as if the message was consumed successfully.");
    declareOnErrorRoute(typeLoader, onErrorContinue);

    NestedRouteDeclarer onErrorPropagate = errorHandler.withRoute("onErrorPropagate")
        .describedAs("Error handler used to propagate errors. It will rollback any transaction and not consume messages.");
    declareOnErrorRoute(typeLoader, onErrorPropagate);

    // TODO MULE-13277 errorHandler.isOneRouteRequired(true);
  }

  private void declareOnErrorRoute(ClassTypeLoader typeLoader, NestedRouteDeclarer onError) {
    onError.withChain();

    onError.onDefaultParameterGroup()
        .withOptionalParameter("when")
        .ofType(typeLoader.load(String.class))
        .describedAs("The expression that will be evaluated to determine if this exception strategy should be executed. "
            + "This should always be a boolean expression.");

    onError.onDefaultParameterGroup()
        .withOptionalParameter("type")
        .ofType(BaseTypeBuilder.create(JAVA).stringType()
            .enumOf("ANY", "REDELIVERY_EXHAUSTED", "TRANSFORMATION", "EXPRESSION", "SECURITY", "CLIENT_SECURITY",
                    "SERVER_SECURITY", "ROUTING", "CONNECTIVITY", "RETRY_EXHAUSTED", "TIMEOUT")
            .build())
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The full name of the error type to match against or a comma separated list of full names, "
            + "to match against any of them.");

    onError.onDefaultParameterGroup()
        .withOptionalParameter("logException")
        .ofType(typeLoader.load(boolean.class))
        .defaultingTo(true)
        .describedAs("Determines whether the handled exception will be logged to its standard logger in the ERROR "
            + "level before being handled. Default is true.");

    onError.onDefaultParameterGroup()
        .withOptionalParameter("enableNotifications")
        .ofType(typeLoader.load(boolean.class))
        .defaultingTo(true)
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("Determines whether ExceptionNotifications will be fired from this strategy when an exception occurs."
            + " Default is true.");
  }

  private void declareErrors(ExtensionDeclarer extensionDeclarer) {

    final ErrorModel criticalError = newError(CRITICAL).build();
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
    extensionDeclarer.withErrorModel(newError(UNKNOWN).withParent(anyError).build());

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
    extensionDeclarer.withErrorModel(newError(SOURCE_ERROR_RESPONSE_GENERATE).withParent(sourceError).build());
    extensionDeclarer.withErrorModel(newError(SOURCE_ERROR_RESPONSE_SEND).withParent(sourceError).build());
    extensionDeclarer.withErrorModel(newError(SOURCE_RESPONSE_GENERATE).withParent(sourceResponseError).build());
    extensionDeclarer.withErrorModel(newError(SOURCE_RESPONSE_SEND).withParent(sourceResponseError).build());

    extensionDeclarer.withErrorModel(criticalError);
    extensionDeclarer.withErrorModel(newError(OVERLOAD).withParent(criticalError).build());
    extensionDeclarer.withErrorModel(newError(FATAL).withParent(criticalError).build());
  }

  private void declareConfiguration(ExtensionDeclarer extensionDeclarer, ClassTypeLoader typeLoader) {
    ConstructDeclarer configuration = extensionDeclarer.withConstruct("configuration")
        .allowingTopLevelDefinition()
        .withStereotype(APP_CONFIG)
        .describedAs("Specifies defaults and general settings for the Mule instance.");

    addReconnectionStrategyParameter(configuration.getDeclaration());

    configuration.onDefaultParameterGroup()
        .withOptionalParameter("defaultResponseTimeout")
        .ofType(typeLoader.load(String.class))
        .withExpressionSupport(NOT_SUPPORTED)
        .defaultingTo("10000")
        .describedAs("The default period (ms) to wait for a synchronous response.");

    configuration.onDefaultParameterGroup()
        .withOptionalParameter("defaultTransactionTimeout")
        .ofType(typeLoader.load(String.class))
        .withExpressionSupport(NOT_SUPPORTED)
        .defaultingTo("30000")
        .describedAs("The default timeout (ms) for transactions. This can also be configured on transactions, "
            + "in which case the transaction configuration is used instead of this default.");

    configuration.onDefaultParameterGroup()
        .withOptionalParameter("defaultErrorHandler-ref")
        .ofType(typeLoader.load(String.class))
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("The default error handler for every flow. This must be a reference to a global error handler.");

    configuration.onDefaultParameterGroup()
        .withOptionalParameter("shutdownTimeout")
        .ofType(typeLoader.load(Integer.class))
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

    configuration.onDefaultParameterGroup()
        .withOptionalParameter("maxQueueTransactionFilesSize")
        .ofType(typeLoader.load(Integer.class))
        .withExpressionSupport(NOT_SUPPORTED)
        .defaultingTo("500")
        .describedAs("The approximated maximum space in megabytes used by the transaction log files for transactional persistent queues."
            + " Take into account that this number applies both to the set of transaction log files for XA and for local transactions. "
            + "If both type of transactions are used then the approximated maximum space used will be twice the configured value.");

    configuration.onDefaultParameterGroup()
        .withOptionalParameter("defaultObjectSerializer-ref")
        .ofType(typeLoader.load(String.class))
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("An optional reference to an ObjectSerializer to be used as the application's default");

    configuration.onDefaultParameterGroup()
        .withOptionalParameter("dynamicConfigExpiration")
        .describedAs(DYNAMIC_CONFIG_EXPIRATION_DESCRIPTION)
        .ofType(new DynamicConfigExpirationTypeBuilder().buildDynamicConfigExpirationType())
        .withExpressionSupport(NOT_SUPPORTED)
        .withDsl(ParameterDslConfiguration.builder()
            .allowsReferences(false)
            .allowsInlineDefinition(true)
            .allowTopLevelDefinition(false)
            .build());
  }

  private void declareConfigurationProperties(ExtensionDeclarer extensionDeclarer, ClassTypeLoader typeLoader) {
    ConstructDeclarer configuration = extensionDeclarer.withConstruct("configurationProperties")
        .allowingTopLevelDefinition()
        .withStereotype(APP_CONFIG)
        .describedAs("References a file with configuration properties. Each property has a key and a value. \n"
            + "The key can be referenced from the mule configuration files using the following semantics: \n"
            + "${key_name}. This allows to externalize configuration and change it based\n"
            + "on the environment the application is being deployed to.");

    configuration.onDefaultParameterGroup()
        .withRequiredParameter("file")
        .ofType(typeLoader.load(String.class))
        .withExpressionSupport(NOT_SUPPORTED)
        .withDisplayModel(DisplayModel.builder().path(new PathModel(FILE, false, new String[] {"properties"})).build())
        .describedAs(" The location of the file with the configuration properties to use. "
            + "It may be a location in the classpath or an absolute location. The file location\n"
            + " value may also contains references to properties that will only be resolved based on "
            + "system properties or properties set at deployment time.");
  }

}
