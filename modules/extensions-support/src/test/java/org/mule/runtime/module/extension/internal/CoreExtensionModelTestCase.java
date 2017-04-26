/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.ExpressionSupport.REQUIRED;
import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.api.meta.model.ExecutionType.BLOCKING;
import static org.mule.runtime.api.meta.model.ExecutionType.CPU_INTENSIVE;
import static org.mule.runtime.api.meta.model.ExecutionType.CPU_LITE;
import static org.mule.runtime.api.meta.model.error.ErrorModelBuilder.newError;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.CONTENT;
import static org.mule.runtime.core.config.MuleManifest.getProductDescription;
import static org.mule.runtime.core.config.MuleManifest.getProductName;
import static org.mule.runtime.core.config.MuleManifest.getProductVersion;
import static org.mule.runtime.core.config.MuleManifest.getVendorName;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.module.extension.internal.resources.MuleExtensionModelProvider.getMuleExtensionModel;
import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.annotation.TypeIdAnnotation;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.model.UnionType;
import org.mule.metadata.api.model.impl.DefaultAnyType;
import org.mule.metadata.api.model.impl.DefaultArrayType;
import org.mule.metadata.api.model.impl.DefaultBooleanType;
import org.mule.metadata.api.model.impl.DefaultNumberType;
import org.mule.metadata.api.model.impl.DefaultObjectType;
import org.mule.metadata.api.model.impl.DefaultStringType;
import org.mule.metadata.api.model.impl.DefaultVoidType;
import org.mule.metadata.api.utils.MetadataTypeUtils;
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
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.core.api.processor.LoggerMessageProcessor;
import org.mule.runtime.core.api.processor.LoggerMessageProcessor.LogLevel;
import org.mule.runtime.core.api.store.ObjectStore;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.routing.AggregationStrategy;
import org.mule.runtime.core.source.scheduler.schedule.FixedFrequencyScheduler;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

public class CoreExtensionModelTestCase extends AbstractMuleContextTestCase {

  private static final ErrorModel errorMuleAny = newError("ANY", "MULE").build();

  private static ExtensionModel coreExtensionModel = getMuleExtensionModel();

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
    assertThat(coreExtensionModel.getXmlDslModel().getPrefix(), is(CORE_PREFIX));
    assertThat(coreExtensionModel.getXmlDslModel().getNamespace(), is("http://www.mulesoft.org/schema/mule/core"));
    assertThat(coreExtensionModel.getXmlDslModel().getSchemaLocation(),
               is("http://www.mulesoft.org/schema/mule/core/current/mule.xsd"));
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
    assertThat(coreExtensionModel.getOperationModels(), hasSize(15));
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

    // TODO MULE-11935 a way is needed to determine the return type of the trasformer in the extension model.
    assertComponentDeterminesOutput(transformModel);

    final List<ParameterGroupModel> paramGroupModels = transformModel.getParameterGroupModels();
    assertThat(paramGroupModels, hasSize(4));

    assertThat(paramGroupModels.get(0).getName(), is("General"));
    List<ParameterModel> paramModels = paramGroupModels.get(0).getParameterModels();
    assertThat(paramModels.get(0).getName(), is("ref"));
    assertThat(paramModels.get(0).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(paramModels.get(0).getType(), instanceOf(DefaultObjectType.class));
    assertThat(paramModels.get(0).getType().getAnnotation(TypeIdAnnotation.class).get().getValue(),
               is(Transformer.class.getName()));
    assertThat(paramModels.get(0).isRequired(), is(true));

    assertPayload(transformModel.getAllParameterModels().get(1));

    assertThat(paramModels.get(2).getName(), is("setVariable"));
    assertThat(paramModels.get(2).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(paramModels.get(2).getType(), instanceOf(DefaultArrayType.class));
    assertThat(paramModels.get(2).getType().getAnnotation(TypeIdAnnotation.class).get().getValue(),
               is(List.class.getName()));
    assertThat(paramModels.get(2).isRequired(), is(false));

    assertThat(((ArrayType) paramModels.get(2).getType()).getType(), instanceOf(ObjectType.class));
    assertThat(((ObjectType) ((ArrayType) paramModels.get(2).getType()).getType()).getFields(), hasSize(3));
    assertThat(((ObjectType) ((ArrayType) paramModels.get(2).getType()).getType()).getFields().stream()
        .map(MetadataTypeUtils::getLocalPart).collect(toList()), containsInAnyOrder("variableName", "resource", "script"));


    assertThat(paramGroupModels.get(1).getName(), is("setPayload"));
    assertScriptAndResource(paramGroupModels.get(1).getParameterModels());

    assertThat(paramGroupModels.get(2).getName(), is("setAttributes"));
    assertScriptAndResource(paramGroupModels.get(2).getParameterModels());

    assertTarget(transformModel.getAllParameterModels().get(7));
  }

  private void assertScriptAndResource(List<ParameterModel> paramModels) {
    assertThat(paramModels.get(0).getName(), is("script"));
    assertThat(paramModels.get(0).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(paramModels.get(0).getType(), instanceOf(DefaultObjectType.class));
    assertThat(paramModels.get(0).isRequired(), is(false));
    assertThat(paramModels.get(0).getRole(), is(CONTENT));

    assertThat(paramModels.get(1).getName(), is("resource"));
    assertThat(paramModels.get(1).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(paramModels.get(1).getType(), instanceOf(StringType.class));
    assertThat(paramModels.get(1).isRequired(), is(false));
    assertThat(paramModels.get(1).getRole(), is(BEHAVIOUR));
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

    assertThat(asyncModel.getErrorModels(), empty());
    assertThat(asyncModel.getExecutionType(), is(CPU_LITE));

    assertOutputSameAsInput(asyncModel);

    assertThat(asyncModel.getAllParameterModels(), hasSize(0));
  }

  @Test
  public void tryScope() {
    final ScopeModel tryModel = (ScopeModel) coreExtensionModel.getOperationModel("try").get();

    assertThat(tryModel.getErrorModels(), empty());
    assertThat(tryModel.getExecutionType(), is(BLOCKING));

    assertAssociatedProcessorsChangeOutput(tryModel);

    List<ParameterModel> allParameterModels = tryModel.getAllParameterModels();
    assertThat(allParameterModels, hasSize(2));

    ParameterModel action = allParameterModels.get(0);
    assertThat(action.getName(), is("transactionalAction"));
    assertThat(action.getType(), is(instanceOf(DefaultStringType.class)));
    assertThat(action.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(action.isRequired(), is(false));

    ParameterModel type = allParameterModels.get(1);
    assertThat(type.getName(), is("transactionType"));
    assertThat(type.getType(), is(instanceOf(DefaultStringType.class)));
    assertThat(type.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(type.isRequired(), is(false));
  }

  //TODO: MULE-12224 - Provide support for scope as top level elements
  @Test
  public void errorHandler() {
    final ScopeModel errorHandlerModel = (ScopeModel) coreExtensionModel.getOperationModel("error-handler").get();

    assertThat(errorHandlerModel.getErrorModels(), empty());
    assertThat(errorHandlerModel.getExecutionType(), is(CPU_LITE));

    assertAssociatedProcessorsChangeOutput(errorHandlerModel);

    List<ParameterModel> allParameterModels = errorHandlerModel.getAllParameterModels();
    assertThat(allParameterModels, hasSize(0));
  }

  @Test
  public void onErrorContinue() {
    verifyOnError("on-error-continue");
  }

  @Test
  public void onErrorPropagate() {
    verifyOnError("on-error-propagate");
  }

  //TODO: MULE-12265 - Provide support for "exclusive scopes"
  void verifyOnError(String name) {
    final ScopeModel continueModel = (ScopeModel) coreExtensionModel.getOperationModel(name).get();

    assertThat(continueModel.getErrorModels(), empty());
    assertThat(continueModel.getExecutionType(), is(CPU_LITE));

    assertAssociatedProcessorsChangeOutput(continueModel);

    List<ParameterModel> allParameterModels = continueModel.getAllParameterModels();
    assertThat(allParameterModels, hasSize(4));

    ParameterModel type = allParameterModels.get(0);
    assertThat(type.getName(), is("type"));
    assertThat(type.getType(), is(instanceOf(UnionType.class)));
    List<MetadataType> types = ((UnionType) type.getType()).getTypes();
    assertThat(types, hasSize(2));
    assertThat(types.get(0), is(instanceOf(DefaultStringType.class)));
    assertThat(types.get(1), is(instanceOf(DefaultStringType.class)));
    assertThat(types.get(1).getAnnotation(EnumAnnotation.class).get().getValues(), arrayContainingInAnyOrder(
                                                                                                             "ANY",
                                                                                                             "REDELIVERY_EXHAUSTED",
                                                                                                             "TRANSFORMATION",
                                                                                                             "EXPRESSION",
                                                                                                             "SECURITY",
                                                                                                             "ROUTING",
                                                                                                             "CONNECTIVITY",
                                                                                                             "RETRY_EXHAUSTED"));
    assertThat(type.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(type.isRequired(), is(false));

    ParameterModel when = allParameterModels.get(1);
    assertThat(when.getName(), is("when"));
    assertThat(when.getType(), is(instanceOf(DefaultStringType.class)));
    assertThat(when.getExpressionSupport(), is(SUPPORTED));
    assertThat(when.isRequired(), is(false));

    ParameterModel log = allParameterModels.get(2);
    assertThat(log.getName(), is("logException"));
    assertThat(log.getType(), is(instanceOf(DefaultBooleanType.class)));
    assertThat(log.getExpressionSupport(), is(SUPPORTED));
    assertThat(log.isRequired(), is(false));

    ParameterModel notifications = allParameterModels.get(3);
    assertThat(notifications.getName(), is("enableNotifications"));
    assertThat(notifications.getType(), is(instanceOf(DefaultBooleanType.class)));
    assertThat(notifications.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(notifications.isRequired(), is(false));
    assertThat(notifications.getDefaultValue(), is("true"));
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
   * The operation returns the result of one of its routes, keeping the attributes of the result message.
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
