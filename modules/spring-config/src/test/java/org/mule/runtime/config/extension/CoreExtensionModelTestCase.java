/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.extension;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.dsl.DslConstants.CORE_NAMESPACE;
import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.ExpressionSupport.REQUIRED;
import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.api.meta.model.ExecutionType.BLOCKING;
import static org.mule.runtime.api.meta.model.ExecutionType.CPU_INTENSIVE;
import static org.mule.runtime.api.meta.model.ExecutionType.CPU_LITE;
import static org.mule.runtime.api.meta.model.error.ErrorModelBuilder.newError;
import static org.mule.runtime.core.config.MuleManifest.getProductDescription;
import static org.mule.runtime.core.config.MuleManifest.getProductName;
import static org.mule.runtime.core.config.MuleManifest.getProductVersion;
import static org.mule.runtime.core.config.MuleManifest.getVendorName;

import org.mule.metadata.api.annotation.TypeIdAnnotation;
import org.mule.metadata.api.model.impl.DefaultAnyType;
import org.mule.metadata.api.model.impl.DefaultArrayType;
import org.mule.metadata.api.model.impl.DefaultBooleanType;
import org.mule.metadata.api.model.impl.DefaultNumberType;
import org.mule.metadata.api.model.impl.DefaultObjectType;
import org.mule.metadata.api.model.impl.DefaultStringType;
import org.mule.metadata.api.model.impl.DefaultVoidType;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.message.NullAttributes;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.operation.RouteModel;
import org.mule.runtime.api.meta.model.operation.RouterModel;
import org.mule.runtime.api.meta.model.operation.ScopeModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.core.api.processor.LoggerMessageProcessor;
import org.mule.runtime.core.api.processor.LoggerMessageProcessor.LogLevel;
import org.mule.runtime.core.api.store.ObjectStore;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.routing.AggregationStrategy;
import org.mule.runtime.core.source.scheduler.schedule.FixedFrequencyScheduler;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.extension.api.persistence.ExtensionModelJsonSerializer;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.List;
import java.util.Optional;

import org.junit.BeforeClass;
import org.junit.Test;

public class CoreExtensionModelTestCase extends AbstractMuleContextTestCase {

  private static final ErrorModel errorMuleAny = newError("ANY", "MULE").build();

  private static ExtensionModel coreExtensionModel;

  @BeforeClass
  public static void beforeClass() {
    final String loadedJson =
        IOUtils.toString(CoreExtensionModelTestCase.class.getResourceAsStream("/META-INF/extension-model.json"));
    coreExtensionModel = new ExtensionModelJsonSerializer().deserialize(loadedJson);
  }


  @Test
  public void consistentWithManifest() {
    assertThat(coreExtensionModel.getName(), is(getProductName()));
    assertThat(coreExtensionModel.getDescription(), is(getProductDescription() + ": Core components"));
    assertThat(coreExtensionModel.getVersion(), is(getProductVersion()));
    assertThat(coreExtensionModel.getVendor(), is(getVendorName()));
    assertThat(coreExtensionModel.getCategory(), is(COMMUNITY));
    assertThat(coreExtensionModel.getMinMuleVersion(), is(new MuleVersion(getProductVersion())));
  }

  @Test
  public void consistentWithSchema() {
    assertThat(coreExtensionModel.getXmlDslModel().getPrefix(), is(CORE_NAMESPACE));
    assertThat(coreExtensionModel.getXmlDslModel().getNamespace(), is("http://www.mulesoft.org/schema/mule/core"));
    assertThat(coreExtensionModel.getXmlDslModel().getSchemaLocation(),
               is("http://www.mulesoft.org/schema/mule/current/mule.xsd"));
    assertThat(coreExtensionModel.getXmlDslModel().getSchemaVersion(), is(getProductVersion()));
    assertThat(coreExtensionModel.getXmlDslModel().getXsdFileName(), is("mule.xsd"));
  }

  @Test
  public void otherModels() {
    assertThat(coreExtensionModel.getResources(), empty());
    assertThat(coreExtensionModel.getSubTypes(), empty());
    assertThat(coreExtensionModel.getExternalLibraryModels(), empty());
    assertThat(coreExtensionModel.getImportedTypes(), empty());
    assertThat(coreExtensionModel.getConfigurationModels(), empty());
    assertThat(coreExtensionModel.getOperationModels(), hasSize(11));
    assertThat(coreExtensionModel.getConnectionProviders(), empty());

    assertThat(coreExtensionModel.getErrorModels(),
               hasItem(newError("TRANSFORMATION", "CORE")
                   .withParent(newError("TRANSFORMATION", "MULE").withParent(errorMuleAny).build()).build()));
    assertThat(coreExtensionModel.getErrorModels(),
               hasItem(newError("CORRELATION_TIMEOUT", "CORE")
                   .withParent(newError("ANY", "CORE").withParent(errorMuleAny).build()).build()));

    assertThat(coreExtensionModel.getSourceModels(), hasSize(1));

    assertThat(coreExtensionModel.getModelProperties(), empty());
    assertThat(coreExtensionModel.getTypes(), empty());
  }

  // TODO MULE-9139 update this source model
  @Test
  public void poll() {
    final SourceModel pollModel = coreExtensionModel.getSourceModel("poll").get();

    assertThat(pollModel.getErrorModels(), empty());
    assertThat(pollModel.hasResponse(), is(false));
    assertThat(pollModel.getOutput().getType(), instanceOf(DefaultObjectType.class));
    assertThat(pollModel.getOutput().hasDynamicType(), is(true));
    assertThat(pollModel.getOutputAttributes().getType(), instanceOf(DefaultObjectType.class));
    assertThat(pollModel.getOutputAttributes().hasDynamicType(), is(false));

    final List<ParameterModel> paramModels = pollModel.getAllParameterModels();
    assertThat(paramModels, hasSize(1));

    assertThat(paramModels.get(0).getName(), is("fixedFrequencyScheduler"));
    assertThat(paramModels.get(0).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(paramModels.get(0).getType(), instanceOf(DefaultObjectType.class));
    assertThat(paramModels.get(0).isRequired(), is(false));
    assertThat(paramModels.get(0).getType().getAnnotation(TypeIdAnnotation.class).get().getValue(),
               is(FixedFrequencyScheduler.class.getName()));
    final DefaultObjectType ffSchedulerType = (DefaultObjectType) paramModels.get(0).getType();
    assertThat(ffSchedulerType.getFields(), hasSize(3));
    assertThat(ffSchedulerType.getFieldByName("frequency").get().isRequired(), is(false));
    assertThat(ffSchedulerType.getFieldByName("frequency").get().getValue(), instanceOf(DefaultNumberType.class));
    assertThat(ffSchedulerType.getFieldByName("startDelay").get().isRequired(), is(false));
    assertThat(ffSchedulerType.getFieldByName("startDelay").get().getValue(), instanceOf(DefaultNumberType.class));
    assertThat(ffSchedulerType.getFieldByName("timeUnit").get().isRequired(), is(false));
    assertThat(ffSchedulerType.getFieldByName("timeUnit").get().getValue(), instanceOf(DefaultStringType.class));

    // TODO add the route
  }

  @Test
  public void logger() {
    final OperationModel loggerModel = coreExtensionModel.getOperationModel("logger").get();

    assertThat(loggerModel.getErrorModels(), empty());
    assertThat(loggerModel.getExecutionType(), is(CPU_LITE));

    assertOutputSameAsInput(loggerModel);

    final List<ParameterModel> paramModels = loggerModel.getAllParameterModels();
    assertThat(paramModels, hasSize(3));

    assertThat(paramModels.get(0).getName(), is("message"));
    assertThat(paramModels.get(0).getExpressionSupport(), is(SUPPORTED));
    assertThat(paramModels.get(0).getType(), instanceOf(DefaultStringType.class));
    assertThat(paramModels.get(0).getType().getAnnotation(TypeIdAnnotation.class).get().getValue(),
               is(String.class.getName()));
    assertThat(paramModels.get(0).isRequired(), is(false));

    assertThat(paramModels.get(1).getName(), is("category"));
    assertThat(paramModels.get(1).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(paramModels.get(1).getType(), instanceOf(DefaultStringType.class));
    assertThat(paramModels.get(1).getType().getAnnotation(TypeIdAnnotation.class).get().getValue(),
               is(String.class.getName()));
    assertThat(paramModels.get(1).isRequired(), is(false));

    assertThat(paramModels.get(2).getName(), is("level"));
    assertThat(paramModels.get(2).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(paramModels.get(2).getType(), instanceOf(DefaultStringType.class));
    assertThat(paramModels.get(2).getType().getAnnotation(TypeIdAnnotation.class).get().getValue(),
               is(LoggerMessageProcessor.class.getName() + "." + LogLevel.class.getSimpleName()));
    assertThat(paramModels.get(2).isRequired(), is(false));
  }

  @Test
  public void transform() {
    final OperationModel transformModel = coreExtensionModel.getOperationModel("transform").get();

    assertThat(transformModel.getErrorModels(),
               hasItem(newError("TRANSFORMATION", "CORE")
                   .withParent(newError("TRANSFORMATION", "MULE").withParent(errorMuleAny).build()).build()));
    assertThat(transformModel.getExecutionType(), is(CPU_INTENSIVE));

    // TODO MULE-11935 a way is needed to detemrine the return type of the trasformer in the extension model.
    assertComponentDeterminesOutput(transformModel);

    final List<ParameterModel> paramModels = transformModel.getAllParameterModels();
    assertThat(paramModels, hasSize(3));

    assertThat(paramModels.get(0).getName(), is("ref"));
    assertThat(paramModels.get(0).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(paramModels.get(0).getType(), instanceOf(DefaultObjectType.class));
    assertThat(paramModels.get(0).getType().getAnnotation(TypeIdAnnotation.class).get().getValue(),
               is(Transformer.class.getName()));
    assertThat(paramModels.get(0).isRequired(), is(true));

    assertPayload(paramModels.get(1));
    assertTarget(paramModels.get(2));
  }

  @Test
  public void splitter() {
    final OperationModel splitterModel = coreExtensionModel.getOperationModel("splitter").get();

    assertThat(splitterModel.getErrorModels(), empty());
    assertThat(splitterModel.getExecutionType(), is(CPU_INTENSIVE));

    assertComponentDeterminesOutput(splitterModel);

    assertThat(splitterModel.getAllParameterModels(), hasSize(2));

    assertMultiPayload(splitterModel.getAllParameterModels().get(0), "payload", REQUIRED);
    assertTarget(splitterModel.getAllParameterModels().get(1));
  }


  @Test
  public void collectionSplitter() {
    final OperationModel collectionSplitterModel = coreExtensionModel.getOperationModel("collectionSplitter").get();

    assertThat(collectionSplitterModel.getErrorModels(), empty());
    assertThat(collectionSplitterModel.getExecutionType(), is(CPU_LITE));

    assertComponentDeterminesOutput(collectionSplitterModel);

    assertThat(collectionSplitterModel.getAllParameterModels(), hasSize(2));

    assertMultiPayload(collectionSplitterModel.getAllParameterModels().get(0), "payload", SUPPORTED);
    assertTarget(collectionSplitterModel.getAllParameterModels().get(1));
  }

  @Test
  public void collectionAggregator() {
    final OperationModel collectionAggregatorModel = coreExtensionModel.getOperationModel("collection-aggregator").get();

    assertThat(collectionAggregatorModel.getErrorModels(),
               hasItem(newError("CORRELATION_TIMEOUT", "CORE")
                   .withParent(newError("ANY", "CORE").withParent(errorMuleAny).build()).build()));
    assertThat(collectionAggregatorModel.getExecutionType(), is(BLOCKING));

    assertThat(collectionAggregatorModel.getOutput().getType(), instanceOf(DefaultArrayType.class));
    assertThat(collectionAggregatorModel.getOutput().hasDynamicType(), is(false));
    assertThat(collectionAggregatorModel.getOutputAttributes().getType(), instanceOf(DefaultObjectType.class));
    assertThat(collectionAggregatorModel.getOutputAttributes().getType().getAnnotation(TypeIdAnnotation.class).get().getValue(),
               is(NullAttributes.class.getName()));
    assertThat(collectionAggregatorModel.getOutputAttributes().hasDynamicType(), is(false));

    final List<ParameterModel> paramModels = collectionAggregatorModel.getAllParameterModels();
    assertThat(paramModels, hasSize(8));

    assertThat(paramModels.get(0).getName(), is("timeout"));
    assertThat(paramModels.get(0).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(paramModels.get(0).getType(), instanceOf(DefaultNumberType.class));
    assertThat(paramModels.get(0).getType().getAnnotation(TypeIdAnnotation.class).get().getValue(), is(long.class.getName()));
    assertThat(paramModels.get(0).isRequired(), is(true));

    assertThat(paramModels.get(1).getName(), is("failOnTimeout"));
    assertThat(paramModels.get(1).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(paramModels.get(1).getType(), instanceOf(DefaultBooleanType.class));
    assertThat(paramModels.get(1).isRequired(), is(false));

    assertThat(paramModels.get(2).getName(), is("processedGroupsObjectStoreRef"));
    assertThat(paramModels.get(2).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(paramModels.get(2).getType(), instanceOf(DefaultObjectType.class));
    assertThat(paramModels.get(2).getType().getAnnotation(TypeIdAnnotation.class).get().getValue(),
               is(ObjectStore.class.getName()));
    assertThat(paramModels.get(2).isRequired(), is(false));

    assertThat(paramModels.get(3).getName(), is("eventGroupsObjectStoreRef"));
    assertThat(paramModels.get(3).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(paramModels.get(3).getType(), instanceOf(DefaultObjectType.class));
    assertThat(paramModels.get(3).getType().getAnnotation(TypeIdAnnotation.class).get().getValue(),
               is(ObjectStore.class.getName()));
    assertThat(paramModels.get(3).isRequired(), is(false));

    assertThat(paramModels.get(4).getName(), is("persistentStores"));
    assertThat(paramModels.get(4).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(paramModels.get(4).getType(), instanceOf(DefaultBooleanType.class));
    assertThat(paramModels.get(4).isRequired(), is(false));

    assertThat(paramModels.get(5).getName(), is("storePrefix"));
    assertThat(paramModels.get(5).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(paramModels.get(5).getType(), instanceOf(DefaultStringType.class));
    assertThat(paramModels.get(5).isRequired(), is(true));

    assertPayload(paramModels.get(6));
    assertTarget(paramModels.get(7));
  }

  @Test
  public void foreach() {
    final ScopeModel foreachModel = (ScopeModel) coreExtensionModel.getOperationModel("foreach").get();

    assertThat(foreachModel.getRouteModel().getMinOccurs(), is(1));
    assertThat(foreachModel.getRouteModel().getMaxOccurs(), is(Optional.empty()));

    assertThat(foreachModel.getErrorModels(), empty());
    assertThat(foreachModel.getExecutionType(), is(CPU_LITE));

    assertComponentDeterminesOutput(foreachModel);

    assertThat(foreachModel.getAllParameterModels(), hasSize(5));

    assertMultiPayload(foreachModel.getAllParameterModels().get(0), "collection", SUPPORTED);

    assertThat(foreachModel.getAllParameterModels().get(1).getName(), is("batchSize"));
    assertThat(foreachModel.getAllParameterModels().get(1).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(foreachModel.getAllParameterModels().get(1).getType(), instanceOf(DefaultNumberType.class));
    assertThat(foreachModel.getAllParameterModels().get(1).isRequired(), is(false));

    assertThat(foreachModel.getAllParameterModels().get(2).getName(), is("rootMessageVariableName"));
    assertThat(foreachModel.getAllParameterModels().get(2).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(foreachModel.getAllParameterModels().get(2).getType(), instanceOf(DefaultStringType.class));
    assertThat(foreachModel.getAllParameterModels().get(2).isRequired(), is(false));

    assertThat(foreachModel.getAllParameterModels().get(3).getName(), is("counterVariableName"));
    assertThat(foreachModel.getAllParameterModels().get(3).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(foreachModel.getAllParameterModels().get(3).getType(), instanceOf(DefaultStringType.class));
    assertThat(foreachModel.getAllParameterModels().get(3).isRequired(), is(false));

    assertTarget(foreachModel.getAllParameterModels().get(4));
  }

  @Test
  public void flowRef() {
    final OperationModel flowRefModel = coreExtensionModel.getOperationModel("flow-ref").get();

    assertAssociatedProcessorsChangeOutput(flowRefModel);

    assertThat(flowRefModel.getAllParameterModels(), hasSize(3));

    assertThat(flowRefModel.getAllParameterModels().get(0).getName(), is("name"));
    assertThat(flowRefModel.getAllParameterModels().get(0).getExpressionSupport(), is(SUPPORTED));
    assertThat(flowRefModel.getAllParameterModels().get(0).getType(), instanceOf(DefaultStringType.class));
    assertThat(flowRefModel.getAllParameterModels().get(0).isRequired(), is(true));

    assertPayload(flowRefModel.getAllParameterModels().get(1));
    assertTarget(flowRefModel.getAllParameterModels().get(2));
  }

  @Test
  public void filter() {
    final OperationModel filterModel = coreExtensionModel.getOperationModel("filter").get();

    assertOutputSameAsInput(filterModel);

    assertThat(filterModel.getAllParameterModels(), hasSize(2));

    assertPayload(filterModel.getAllParameterModels().get(1));
  }

  @Test
  public void choice() {
    final RouterModel choiceModel = (RouterModel) coreExtensionModel.getOperationModel("choice").get();

    assertThat(choiceModel.getErrorModels(), empty());
    assertThat(choiceModel.getExecutionType(), is(CPU_LITE));

    assertAssociatedProcessorsChangeOutput(choiceModel);

    assertThat(choiceModel.getAllParameterModels(), empty());

    assertThat(choiceModel.getRouteModels(), hasSize(2));

    final RouteModel whenRouteModel = choiceModel.getRouteModels().get(0);
    assertThat(whenRouteModel.getName(), is("when"));
    assertThat(whenRouteModel.getMinOccurs(), is(1));
    assertThat(whenRouteModel.getMaxOccurs(), is(Optional.empty()));
    assertThat(whenRouteModel.getAllParameterModels(), hasSize(1));

    assertThat(whenRouteModel.getAllParameterModels().get(0).getName(), is("expression"));
    assertThat(whenRouteModel.getAllParameterModels().get(0).getExpressionSupport(), is(SUPPORTED));
    assertThat(whenRouteModel.getAllParameterModels().get(0).getType(), instanceOf(DefaultBooleanType.class));
    assertThat(whenRouteModel.getAllParameterModels().get(0).isRequired(), is(false));

    final RouteModel otherwiseRouteModel = choiceModel.getRouteModels().get(1);
    assertThat(otherwiseRouteModel.getName(), is("otherwise"));
    assertThat(otherwiseRouteModel.getMinOccurs(), is(0));
    assertThat(otherwiseRouteModel.getMaxOccurs().get(), is(1));
    assertThat(otherwiseRouteModel.getAllParameterModels(), empty());
  }

  @Test
  public void scatterGather() {
    final RouterModel scatterGatherModel = (RouterModel) coreExtensionModel.getOperationModel("scatter-gather").get();

    assertThat(scatterGatherModel.getErrorModels(), empty());
    assertThat(scatterGatherModel.getExecutionType(), is(CPU_LITE));

    assertComponentDeterminesOutput(scatterGatherModel);

    assertThat(scatterGatherModel.getAllParameterModels(), hasSize(3));

    assertThat(scatterGatherModel.getAllParameterModels().get(0).getName(), is("parallel"));
    assertThat(scatterGatherModel.getAllParameterModels().get(0).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(scatterGatherModel.getAllParameterModels().get(0).getType(), instanceOf(DefaultBooleanType.class));
    assertThat(scatterGatherModel.getAllParameterModels().get(0).isRequired(), is(false));

    assertThat(scatterGatherModel.getAllParameterModels().get(1).getName(), is("timeout"));
    assertThat(scatterGatherModel.getAllParameterModels().get(1).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(scatterGatherModel.getAllParameterModels().get(1).getType(), instanceOf(DefaultNumberType.class));
    assertThat(scatterGatherModel.getAllParameterModels().get(1).isRequired(), is(false));

    assertThat(scatterGatherModel.getAllParameterModels().get(2).getName(), is("custom-aggregation-strategy"));
    assertThat(scatterGatherModel.getAllParameterModels().get(2).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(scatterGatherModel.getAllParameterModels().get(2).getType(), instanceOf(DefaultObjectType.class));
    assertThat(scatterGatherModel.getAllParameterModels().get(2).getType().getAnnotation(TypeIdAnnotation.class).get().getValue(),
               is(AggregationStrategy.class.getName()));
    assertThat(scatterGatherModel.getAllParameterModels().get(2).isRequired(), is(false));

    assertThat(scatterGatherModel.getRouteModels(), hasSize(1));

    final RouteModel routeModel = scatterGatherModel.getRouteModels().get(0);
    assertThat(routeModel.getName(), is("route"));
    assertThat(routeModel.getMinOccurs(), is(2));
    assertThat(routeModel.getMaxOccurs(), is(Optional.empty()));
    assertThat(routeModel.getAllParameterModels(), empty());
  }

  @Test
  public void async() {
    final ScopeModel asyncModel = (ScopeModel) coreExtensionModel.getOperationModel("async").get();

    assertThat(asyncModel.getRouteModel().getMinOccurs(), is(1));
    assertThat(asyncModel.getRouteModel().getMaxOccurs(), is(Optional.empty()));

    assertThat(asyncModel.getErrorModels(), empty());
    assertThat(asyncModel.getExecutionType(), is(CPU_LITE));

    assertOutputSameAsInput(asyncModel);

    assertThat(asyncModel.getAllParameterModels(), hasSize(1));

    assertThat(asyncModel.getAllParameterModels().get(0).getName(), is("processingStrategy"));
    assertThat(asyncModel.getAllParameterModels().get(0).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(asyncModel.getAllParameterModels().get(0).getType(), instanceOf(DefaultStringType.class));
    assertThat(asyncModel.getAllParameterModels().get(0).isRequired(), is(false));
  }

  /**
   * The operation returns its input as the output.
   * 
   * @param model the model to assert on
   */
  private void assertOutputSameAsInput(final OperationModel model) {
    assertThat(model.getOutput().getType(), instanceOf(DefaultVoidType.class));
    assertThat(model.getOutput().hasDynamicType(), is(false));
    assertThat(model.getOutputAttributes().getType(), instanceOf(DefaultVoidType.class));
    assertThat(model.getOutputAttributes().hasDynamicType(), is(false));
  }

  /**
   * The operation buids its own output, leaving the resulting message without attrbiutes.
   * 
   * @param model the model to assert on
   */
  private void assertComponentDeterminesOutput(final OperationModel model) {
    assertThat(model.getOutput().getType(), instanceOf(DefaultAnyType.class));
    assertThat(model.getOutput().hasDynamicType(), is(false));
    assertThat(model.getOutputAttributes().getType(), instanceOf(DefaultObjectType.class));
    assertThat(model.getOutputAttributes().getType().getAnnotation(TypeIdAnnotation.class).get().getValue(),
               is(NullAttributes.class.getName()));
    assertThat(model.getOutputAttributes().hasDynamicType(), is(false));
  }

  /**
   * The operation breturns the result of one of its routes, keeping the attributes of the result message.
   * 
   * @param model the model to assert on
   */
  private void assertAssociatedProcessorsChangeOutput(final OperationModel model) {
    assertThat(model.getOutput().getType(), instanceOf(DefaultAnyType.class));
    assertThat(model.getOutput().hasDynamicType(), is(false));
    assertThat(model.getOutputAttributes().getType(), instanceOf(DefaultObjectType.class));
    assertThat(model.getOutputAttributes().getType().getAnnotation(TypeIdAnnotation.class).get().getValue(),
               is(Attributes.class.getName()));
    assertThat(model.getOutputAttributes().hasDynamicType(), is(false));
  }

  private void assertPayload(final ParameterModel payloadParameterModel) {
    assertThat(payloadParameterModel.getName(), is("payload"));
    assertThat(payloadParameterModel.getExpressionSupport(), is(SUPPORTED));
    assertThat(payloadParameterModel.getType(), instanceOf(DefaultObjectType.class));
    assertThat(payloadParameterModel.getType().getAnnotation(TypeIdAnnotation.class).get().getValue(),
               is(Object.class.getName()));
    assertThat(payloadParameterModel.isRequired(), is(false));
  }

  private void assertMultiPayload(final ParameterModel payloadParameterModel, String payloadVariableName,
                                  ExpressionSupport expressionSupport) {
    assertThat(payloadParameterModel.getName(), is(payloadVariableName));
    assertThat(payloadParameterModel.getExpressionSupport(), is(expressionSupport));
    assertThat(payloadParameterModel.getType(), instanceOf(DefaultArrayType.class));
    assertThat(payloadParameterModel.getType().getAnnotation(TypeIdAnnotation.class).get().getValue(),
               is(Iterable.class.getName()));
    assertThat(payloadParameterModel.isRequired(), is(expressionSupport == REQUIRED));
  }

  private void assertTarget(final ParameterModel targetParameterModel) {
    assertThat(targetParameterModel.getName(), is("target"));
    assertThat(targetParameterModel.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(targetParameterModel.getType(), instanceOf(DefaultStringType.class));
    assertThat(targetParameterModel.getType().getAnnotation(TypeIdAnnotation.class).get().getValue(),
               is(String.class.getName()));
    assertThat(targetParameterModel.isRequired(), is(false));
  }
}
