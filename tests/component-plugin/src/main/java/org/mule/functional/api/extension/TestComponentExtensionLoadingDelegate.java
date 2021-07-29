/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.extension;

import static java.util.Collections.singletonList;
import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.ExpressionSupport.REQUIRED;
import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.api.meta.model.display.PathModel.Location.ANY;
import static org.mule.runtime.api.meta.model.display.PathModel.Type.FILE;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.CONTENT;
import static org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder.newStereotype;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.ANY_TYPE;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.BASE_TYPE_BUILDER;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.BOOLEAN_TYPE;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.INTEGER_TYPE;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_VERSION;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.NUMBER_TYPE;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.STRING_TYPE;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.VOID_TYPE;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.ERROR_HANDLER;
import static org.mule.runtime.extension.api.util.XmlModelUtils.buildSchemaLocation;

import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ConstructDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasConstructDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasOperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasSourceDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NestedComponentDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclarer;
import org.mule.runtime.api.meta.model.display.ClassValueModel;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.PathModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionLoadingDelegate;
import org.mule.runtime.extension.api.property.NoWrapperModelProperty;
import org.mule.runtime.extension.api.stereotype.MuleStereotypes;

/**
 * An {@link ExtensionDeclarer} for the Test Component Plugin
 *
 * @since 4.4
 */
public class TestComponentExtensionLoadingDelegate implements ExtensionLoadingDelegate {

  private static final StereotypeModel LOG_CHECKER = newStereotype("LOG_CHECKER", "TEST")
      .withParent(MuleStereotypes.PROCESSOR)
      .build();

  private static final StereotypeModel STACK_TRACE_ELEMENT = newStereotype("STACK_TRACE_ELEMENT", "TEST")
      .withParent(MuleStereotypes.PROCESSOR)
      .build();

  @Override
  public void accept(ExtensionDeclarer extensionDeclarer, ExtensionLoadingContext context) {
    extensionDeclarer
        .named("Test Component Plugin")
        .describedAs("Test component for performing assertions")
        .onVersion(MULE_VERSION)
        .fromVendor("MuleSoft, Inc.")
        .withCategory(COMMUNITY)
        .withXmlDsl(XmlDslModel.builder()
            .setPrefix("test")
            .setNamespace("http://www.mulesoft.org/schema/mule/test")
            .setSchemaVersion(MULE_VERSION)
            .setXsdFileName("mule-test.xsd")
            .setSchemaLocation(buildSchemaLocation("test", "http://www.mulesoft.org/schema/mule/test"))
            .build());

    declareProcessor(extensionDeclarer);
    declareInvocationCounter(extensionDeclarer);
    declareAssert(extensionDeclarer);
    declareLifecycleTracker(extensionDeclarer);
    declareLifecycleTrackerCheck(extensionDeclarer);
    declareLifecycleTrackerConfig(extensionDeclarer);
    declareLifecycleTrackerScope(extensionDeclarer);
    declareQueue(extensionDeclarer);
    declareLifecycleTrackerSource(extensionDeclarer);
    declareSkeletonSource(extensionDeclarer);
    declareThrow(extensionDeclarer);

    declareSharedConfig(extensionDeclarer);
    declareLifecycleObject(extensionDeclarer);
    declareDependencyInjectionObject(extensionDeclarer);
    declareOnErrorCheckLog(extensionDeclarer);
    declareCheckEquals(extensionDeclarer);
    declareCheckStackTrace(extensionDeclarer);
    declareMethodCall(extensionDeclarer);
    declareCause(extensionDeclarer);
    declareCheckSummary(extensionDeclarer);
  }

  private void declareCheckSummary(HasOperationDeclarer declarer) {
    OperationDeclarer checkSummaryDeclarer = voidOperation(declarer, "checkSummary")
        .describedAs("Evaluates the log summary to be logged, checking that it contains the information expected.")
        .withStereotype(LOG_CHECKER);

    NestedComponentDeclarer summaryInfoDeclarer = checkSummaryDeclarer
        .withComponent("summaryInfo")
        .describedAs("An element expected log summary information");

    ParameterGroupDeclarer summaryInfoParams = summaryInfoDeclarer.onDefaultParameterGroup();
    summaryInfoParams.withRequiredParameter("key")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);
    summaryInfoParams.withOptionalParameter("value")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);
    summaryInfoParams.withOptionalParameter("valueStartsWith")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);

    checkSummaryDeclarer.onDefaultParameterGroup()
        .withOptionalParameter("exclusiveContent")
        .describedAs("Specifies if the content to check should be the only one present(true) or it allows another information(false)")
        .ofType(BOOLEAN_TYPE)
        .defaultingTo(false);
  }

  private void declareCause(HasOperationDeclarer declarer) {
    voidOperation(declarer, "cause")
        .describedAs("An element with information about stacktraces exception causes")
        .withStereotype(STACK_TRACE_ELEMENT)
        .onDefaultParameterGroup()
        .withRequiredParameter("exception")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .withDisplayModel(DisplayModel.builder().classValue(new ClassValueModel(singletonList("java.lang.Exception"))).build());
  }

  private void declareMethodCall(HasOperationDeclarer declarer) {
    ParameterGroupDeclarer params = voidOperation(declarer, "methodCall")
        .describedAs("An element with information about stacktraces method calls")
        .withStereotype(STACK_TRACE_ELEMENT)
        .onDefaultParameterGroup();

    params.withOptionalParameter("package")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);

    params.withOptionalParameter("class")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .withDisplayModel(DisplayModel.builder().classValue(new ClassValueModel(singletonList("java.lang.Object"))).build());

    params.withOptionalParameter("method")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);

    params.withOptionalParameter("lineNumber")
        .ofType(INTEGER_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);
  }

  private void declareCheckStackTrace(HasOperationDeclarer declarer) {
    OperationDeclarer operation = voidOperation(declarer, "checkStackTrace")
        .describedAs("Evaluates the stacktrace to be logged, checking that it goes through expected method calls")
        .withStereotype(LOG_CHECKER);

    operation.withChain()
        .withAllowedStereotypes(STACK_TRACE_ELEMENT)
        .withMinOccurs(1)
        .withMaxOccurs(null)
        .withModelProperty(NoWrapperModelProperty.INSTANCE);
  }

  private void declareCheckEquals(HasOperationDeclarer declarer) {
    ParameterGroupDeclarer params = voidOperation(declarer, "checkEquals")
        .describedAs("Evaluates the expected and actual logs line by line expecting them to be equal")
        .withStereotype(LOG_CHECKER)
        .onDefaultParameterGroup();

    params.withOptionalParameter("filterLog")
        .describedAs("Configures whether or not to filter the logs to compare before comparison, removing delimiter lines and special characters")
        .ofType(BOOLEAN_TYPE)
        .defaultingTo(true);
  }

  private void declareOnErrorCheckLog(HasOperationDeclarer declarer) {
    OperationDeclarer operation = declarer.withOperation("onErrorCheckLog")
        .describedAs("Error handler used to assert exceptions. It will check the exception and validate it and it's fields to an expected one.")
        .withStereotype(ERROR_HANDLER);

    operation.withOutput().ofType(ANY_TYPE);
    operation.withOutputAttributes().ofType(ANY_TYPE);

    operation.withChain()
        .withAllowedStereotypes(LOG_CHECKER)
        .withModelProperty(NoWrapperModelProperty.INSTANCE);

    ParameterGroupDeclarer params = operation.onDefaultParameterGroup();
    params.withOptionalParameter("propagate")
        .ofType(BOOLEAN_TYPE)
        .defaultingTo(false);
    params.withOptionalParameter("succeedIfNoLog")
        .ofType(BOOLEAN_TYPE)
        .defaultingTo(false);
  }

  private void declareDependencyInjectionObject(HasConstructDeclarer declarer) {
    declarer.withConstruct("dependencyInjectionObject")
        .describedAs("Global object to test lifecycle")
        .allowingTopLevelDefinition();
  }


  private void declareLifecycleObject(ExtensionDeclarer declarer) {
    ParameterGroupDeclarer params = declarer.withConstruct("lifecycleObject")
        .describedAs("Global object to test lifecycle")
        .allowingTopLevelDefinition()
        .onDefaultParameterGroup();

    params.withOptionalParameter("otherLifecycleObject")
        .describedAs("Dependency to another bean.")
        .ofType(ANY_TYPE)
        .withDsl(ParameterDslConfiguration.builder()
            .allowsInlineDefinition(false)
            .allowsReferences(true)
            .allowTopLevelDefinition(false)
            .build())
        .withExpressionSupport(NOT_SUPPORTED);

    params.withOptionalParameter("failurePhase")
        .describedAs("Phase in which this object will throw an exception.")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);
  }

  private void declareSharedConfig(ExtensionDeclarer declarer) {
    ConfigurationDeclarer config = declarer.withConfig("shared")
        .describedAs("Configuration element that can be put at a domain level");

    voidSource(config, "sharedSource").describedAs("Mock message source that links to a shared-config.");
  }

  private void declareThrow(HasOperationDeclarer declarer) {
    ParameterGroupDeclarer params = voidOperation(declarer, "throw")
        .describedAs("Mock message source that links to a shared-config.")
        .onDefaultParameterGroup();

    params.withRequiredParameter("exception")
        .describedAs("A fully qualified classname of the exception object to throw. Must be a TypedException unless an error is provided as well.")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .withDisplayModel(DisplayModel.builder()
            .classValue(new ClassValueModel(singletonList("org.mule.runtime.api.exception.TypedException"))).build());

    params.withOptionalParameter("errorType")
        .describedAs("The error to throw. If provided, the exception will be used as cause for a TypedException.")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);

    params.withOptionalParameter("count")
        .describedAs("The number of times error should be thrown.")
        .ofType(INTEGER_TYPE)
        .defaultingTo(-1)
        .withExpressionSupport(NOT_SUPPORTED);
  }

  private void declareSkeletonSource(HasSourceDeclarer declarer) {
    voidSource(declarer, "skeletonSource")
        .describedAs("Mock message source that provides access to the Processor set by the owner Flow.");
  }

  private void declareLifecycleTrackerSource(HasSourceDeclarer declarer) {
    voidSource(declarer, "lifecycleTrackerSource")
        .describedAs("Mock message source that records lifecycle method invocations");
  }

  private void declareQueue(HasOperationDeclarer declarer) {
    OperationDeclarer operationDeclarer = withNameParameter(declarer.withOperation("queue")
        .describedAs("A stores received events in a in-memory queue. Events can be consumed using mule client requests"));

    operationDeclarer.withOutput().ofType(ANY_TYPE);
    operationDeclarer.withOutputAttributes().ofType(ANY_TYPE);

    ParameterGroupDeclarer params = operationDeclarer.onDefaultParameterGroup();
    params.withOptionalParameter("content")
        .describedAs("Content to be sent to the queue. By default it will be the payload content")
        .ofType(ANY_TYPE)
        .withRole(CONTENT)
        .withExpressionSupport(SUPPORTED)
        .defaultingTo("#[payload]");

    params.withOptionalParameter("contentJavaType")
        .describedAs("Content type to use to transform the content parameter into a java type.")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .withDisplayModel(DisplayModel.builder().classValue(new ClassValueModel(singletonList("java.lang.Object"))).build());
  }

  private void declareLifecycleTrackerScope(HasOperationDeclarer declarer) {
    voidOperation(declarer, "lifecycleTrackerScope")
        .describedAs("Stores the lifecycle method calls received by this scope.")
        .withChain()
        .withModelProperty(NoWrapperModelProperty.INSTANCE);
  }

  private void declareLifecycleTrackerConfig(ExtensionDeclarer declarer) {
    StereotypeModel stereotype = newStereotype("LIFECYCLE_TRACKER_CONFIG", "TEST").build();
    ConstructDeclarer construct = declarer.withConstruct("lifecycleTrackerConfig")
        .describedAs("Stores the lifecycle method calls received by this configs.")
        .allowingTopLevelDefinition()
        .withStereotype(stereotype);

    construct.withOptionalComponent("child")
        .withAllowedStereotypes(stereotype)
        .withModelProperty(NoWrapperModelProperty.INSTANCE);
  }

  private void declareLifecycleTrackerCheck(HasOperationDeclarer declarer) {
    withNameParameter(voidOperation(declarer, "lifecycleTrackerCheck")
        .describedAs("Specialization of 'lifecycle-tracker' that validates the phase transition being done on this component."));
  }

  private void declareLifecycleTracker(HasOperationDeclarer declarer) {
    withNameParameter(voidOperation(declarer, "lifecycleTracker")
        .describedAs("Stores the lifecycle method calls received by this processor."));
  }

  private OperationDeclarer withNameParameter(OperationDeclarer declarer) {
    declarer.onDefaultParameterGroup()
        .withRequiredParameter("name")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .asComponentId();

    return declarer;
  }

  private void declareAssert(HasOperationDeclarer declarer) {
    ParameterGroupDeclarer params = voidOperation(declarer, "assert")
        .describedAs("Assertion processor used to assert an expression, invocation count and thread.")
        .onDefaultParameterGroup();

    params.withRequiredParameter("expression")
        .ofType(BOOLEAN_TYPE)
        .withExpressionSupport(REQUIRED);

    params.withOptionalParameter("message")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);

    params.withOptionalParameter("count")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);
  }

  private void declareInvocationCounter(HasOperationDeclarer declarer) {
    voidOperation(declarer, "invocationCounter")
        .describedAs("This component keeps track of the number of times it is executed.")
        .onDefaultParameterGroup()
        .withRequiredParameter("name")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .asComponentId();
  }

  private void declareProcessor(HasOperationDeclarer declarer) {
    OperationDeclarer processor = declarer.withOperation("processor")
        .describedAs("A processor that can be used for testing message flows. It is a configurable component. The return data for the component can be set so that users can simulate a call to a real service. This component can also track invocation history and fire notifications when messages are received.");

    processor.withOutput().ofType(ANY_TYPE);
    processor.withOutputAttributes().ofType(ANY_TYPE);

    NestedComponentDeclarer callbackDeclarer = processor.withOptionalComponent("callback");
    callbackDeclarer.onDefaultParameterGroup().withOptionalParameter("class")
        .describedAs("A user-defined callback that is invoked when the test component is invoked. This can be useful for capturing information such as message counts. Use the {{class}} attribute to specify the callback class name, which must be an object that implements {{org.mule.tck.functional.EventCallback}}.")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .withDisplayModel(DisplayModel.builder()
            .classValue(new ClassValueModel(singletonList("org.mule.tck.functional.EventCallback"))).build());

    ParameterGroupDeclarer params = processor.onParameterGroup("return-data").withDslInlineRepresentation(true);
    params.withOptionalParameter("file")
        .describedAs("The location of a file to load. The file can point to a resource on the classpath or on disk.")
        .ofType(STRING_TYPE)
        .withDisplayModel(DisplayModel.builder().path(new PathModel(FILE, false, ANY, new String[] {})).build())
        .withExpressionSupport(NOT_SUPPORTED);

    params = processor.onDefaultParameterGroup();
    params.withOptionalParameter("processingType")
        .describedAs("The kind of work this component will report to do, in order to affect the behavior of the Processing Strategy.")
        .ofType(BASE_TYPE_BUILDER.stringType().enumOf("CPU_INTENSIVE", "CPU_LITE", "BLOCKING", "IO_RW", "CPU_LITE_ASYNC").build())
        .defaultingTo("CPU_LITE")
        .withExpressionSupport(NOT_SUPPORTED);
    params.withOptionalParameter("class")
        .describedAs("The class name of a processor to be instantiated and executed")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .withDisplayModel(DisplayModel.builder()
            .classValue(new ClassValueModel(singletonList("org.mule.runtime.core.api.processor.Processor"))).build());

    params.withOptionalParameter("throwException")
        .describedAs("Whether the component should throw an exception before any processing takes place.")
        .ofType(BOOLEAN_TYPE)
        .defaultingTo(false)
        .withExpressionSupport(NOT_SUPPORTED);

    params.withOptionalParameter("logExceptionDetails")
        .describedAs("Whether to output all message details to the log. This includes all headers and the full payload. The information will be logged at INFO level.")
        .ofType(BOOLEAN_TYPE)
        .defaultingTo(false)
        .withExpressionSupport(NOT_SUPPORTED);

    params.withOptionalParameter("exceptionToThrow")
        .describedAs("A fully qualified classname of the exception object to throw. Used in conjunction with {{throwException}}. If this is not specified, a {{FunctionalTestException}} will be thrown by default.")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .withDisplayModel(DisplayModel.builder().classValue(new ClassValueModel(singletonList("java.lang.Exception"))).build());

    params.withOptionalParameter("exceptionText")
        .describedAs("The text of the exception that is thrown. Used in conjunction with {{throwException}}. If this is not specified, an empty message will be used.")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);

    params.withOptionalParameter("enableMessageHistory")
        .describedAs("Every message that is received by the test processor is stored and can be retrieved. If you do not want this information stored, such as if you are running millions of messages through the component, you can disable this feature to avoid a potential out of memory error.")
        .ofType(BOOLEAN_TYPE)
        .defaultingTo(false)
        .withExpressionSupport(NOT_SUPPORTED);

    params.withOptionalParameter("enableNotifications")
        .describedAs("Whether to fire a {{FunctionalTestNotification}} when a message is received by the processor. Test cases can register to receive these notifications and make assertions on the current message.")
        .ofType(BOOLEAN_TYPE)
        .defaultingTo(true)
        .withExpressionSupport(NOT_SUPPORTED);

    params.withOptionalParameter("appendString")
        .describedAs("A string value that will be appended to every message payload that passes through the processor. Note that by setting this property you implicitly select that the message payload will be converted to a string and that a string payload will be returned.")
        .ofType(STRING_TYPE)
        .withExpressionSupport(SUPPORTED);

    params.withOptionalParameter("waitTime")
        .describedAs("The time in milliseconds to wait before returning a result. All processing happens in the processor before the wait begins.")
        .ofType(NUMBER_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);

    params.withOptionalParameter("id")
        .describedAs("The name of this processor")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);
  }

  private SourceDeclarer voidSource(HasSourceDeclarer declarer, String name) {
    SourceDeclarer source = declarer.withMessageSource(name)
        .hasResponse(false);

    source.withOutput().ofType(VOID_TYPE);
    source.withOutputAttributes().ofType(VOID_TYPE);

    return source;
  }

  private OperationDeclarer voidOperation(HasOperationDeclarer declarer, String name) {
    OperationDeclarer operation = declarer.withOperation(name);
    operation.withOutput().ofType(VOID_TYPE);
    operation.withOutputAttributes().ofType(VOID_TYPE);

    return operation;
  }

}
