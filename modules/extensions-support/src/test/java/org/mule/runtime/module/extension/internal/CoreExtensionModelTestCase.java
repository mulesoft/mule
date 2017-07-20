/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
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
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.core.api.config.MuleManifest.getVendorName;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.module.extension.internal.resources.MuleExtensionModelProvider.MULE_VERSION;
import static org.mule.runtime.module.extension.internal.resources.MuleExtensionModelProvider.getExtensionModel;

import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.annotation.TypeIdAnnotation;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.UnionType;
import org.mule.metadata.api.model.VoidType;
import org.mule.metadata.api.model.impl.DefaultAnyType;
import org.mule.metadata.api.model.impl.DefaultArrayType;
import org.mule.metadata.api.model.impl.DefaultBooleanType;
import org.mule.metadata.api.model.impl.DefaultNumberType;
import org.mule.metadata.api.model.impl.DefaultObjectType;
import org.mule.metadata.api.model.impl.DefaultStringType;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.SubTypesModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.operation.RouteModel;
import org.mule.runtime.api.meta.model.operation.RouterModel;
import org.mule.runtime.api.meta.model.operation.ScopeModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.core.api.processor.LoggerMessageProcessor;
import org.mule.runtime.core.api.processor.LoggerMessageProcessor.LogLevel;
import org.mule.runtime.core.api.source.SchedulingStrategy;
import org.mule.runtime.core.api.routing.AggregationStrategy;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

public class CoreExtensionModelTestCase extends AbstractMuleContextTestCase {

  private static final String PRODUCT_NAME = "Mule Core";
  private static final ErrorModel errorMuleAny = newError("ANY", "MULE").build();

  private static ExtensionModel coreExtensionModel = getExtensionModel();

  @Test
  public void consistentWithManifest() {
    assertThat(coreExtensionModel.getName(), is(PRODUCT_NAME));
    assertThat(coreExtensionModel.getDescription(), is("Mule Runtime and Integration Platform: Core components"));
    assertThat(coreExtensionModel.getVersion(), is(getProductVersion()));
    assertThat(coreExtensionModel.getVendor(), is(getVendorName()));
    assertThat(coreExtensionModel.getCategory(), is(COMMUNITY));
    assertThat(coreExtensionModel.getMinMuleVersion(), is(new MuleVersion(MULE_VERSION)));
  }

  @Test
  public void consistentWithSchema() {
    assertThat(coreExtensionModel.getXmlDslModel().getPrefix(), is(CORE_PREFIX));
    assertThat(coreExtensionModel.getXmlDslModel().getNamespace(), is("http://www.mulesoft.org/schema/mule/core"));
    assertThat(coreExtensionModel.getXmlDslModel().getSchemaLocation(),
               is("http://www.mulesoft.org/schema/mule/core/current/mule.xsd"));
    assertThat(coreExtensionModel.getXmlDslModel().getSchemaVersion(), is(MULE_VERSION));
    assertThat(coreExtensionModel.getXmlDslModel().getXsdFileName(), is("mule.xsd"));
  }

  @Test
  public void otherModels() {
    assertThat(coreExtensionModel.getResources(), empty());
    assertThat(coreExtensionModel.getSubTypes(), hasSize(1));

    SubTypesModel subTypesModel = coreExtensionModel.getSubTypes().iterator().next();
    assertThat(subTypesModel.getBaseType().getAnnotation(TypeIdAnnotation.class).get().getValue(),
               is(SchedulingStrategy.class.getName()));

    assertThat(subTypesModel.getSubTypes(), hasSize(2));
    Iterator<ObjectType> iterator = subTypesModel.getSubTypes().iterator();
    final DefaultObjectType ffSchedulerType = (DefaultObjectType) iterator.next();
    assertThat(ffSchedulerType.getFields(), hasSize(3));
    assertThat(ffSchedulerType.getFieldByName("frequency").get().isRequired(), is(false));
    assertThat(ffSchedulerType.getFieldByName("frequency").get().getValue(), instanceOf(DefaultNumberType.class));
    assertThat(ffSchedulerType.getFieldByName("startDelay").get().isRequired(), is(false));
    assertThat(ffSchedulerType.getFieldByName("startDelay").get().getValue(), instanceOf(DefaultNumberType.class));
    assertThat(ffSchedulerType.getFieldByName("timeUnit").get().isRequired(), is(false));
    assertThat(ffSchedulerType.getFieldByName("timeUnit").get().getValue(), instanceOf(DefaultStringType.class));

    final DefaultObjectType cronSchedulerType = (DefaultObjectType) iterator.next();
    assertThat(cronSchedulerType.getFields(), hasSize(2));
    assertThat(cronSchedulerType.getFieldByName("expression").get().isRequired(), is(true));
    assertThat(cronSchedulerType.getFieldByName("expression").get().getValue(), instanceOf(DefaultStringType.class));
    assertThat(cronSchedulerType.getFieldByName("timeZone").get().isRequired(), is(false));
    assertThat(cronSchedulerType.getFieldByName("timeZone").get().getValue(), instanceOf(DefaultStringType.class));

    assertThat(coreExtensionModel.getExternalLibraryModels(), empty());
    assertThat(coreExtensionModel.getImportedTypes(), empty());
    assertThat(coreExtensionModel.getConfigurationModels(), empty());
    assertThat(coreExtensionModel.getOperationModels(), hasSize(14));
    assertThat(coreExtensionModel.getConnectionProviders(), empty());

    assertThat(coreExtensionModel.getErrorModels(),
               hasItem(newError("TRANSFORMATION", "CORE")
                   .withParent(newError("TRANSFORMATION", "MULE").withParent(errorMuleAny).build()).build()));
    assertThat(coreExtensionModel.getErrorModels(),
               hasItem(newError("CORRELATION_TIMEOUT", "CORE")
                   .withParent(newError("ANY", "CORE").withParent(errorMuleAny).build()).build()));

    assertThat(coreExtensionModel.getSourceModels(), hasSize(1));

    assertThat(coreExtensionModel.getModelProperties(), empty());
    assertThat(coreExtensionModel.getTypes(), hasSize(3));
  }

  @Test
  public void scheduler() {
    final SourceModel schedulerModel = coreExtensionModel.getSourceModel("scheduler").get();

    assertThat(schedulerModel.getErrorModels(), empty());
    assertThat(schedulerModel.hasResponse(), is(false));
    assertThat(schedulerModel.getOutput().getType(), instanceOf(DefaultObjectType.class));
    assertThat(schedulerModel.getOutput().hasDynamicType(), is(true));
    assertThat(schedulerModel.getOutputAttributes().getType(), instanceOf(DefaultObjectType.class));
    assertThat(schedulerModel.getOutputAttributes().hasDynamicType(), is(false));

    final List<ParameterModel> paramModels = schedulerModel.getAllParameterModels();
    assertThat(paramModels, hasSize(1));
    assertSchedulingStrategy(paramModels.get(0));
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
  public void splitter() {
    final OperationModel splitterModel = coreExtensionModel.getOperationModel("splitter").get();

    assertThat(splitterModel.getErrorModels(), empty());
    assertThat(splitterModel.getExecutionType(), is(CPU_INTENSIVE));

    assertComponentDeterminesOutput(splitterModel);

    assertThat(splitterModel.getAllParameterModels(), hasSize(3));

    assertMultiPayload(splitterModel.getAllParameterModels().get(0), "payload", REQUIRED);
    assertErrorType(splitterModel.getAllParameterModels().get(1), "filterOnErrorType");
    assertTarget(splitterModel.getAllParameterModels().get(2));
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
    final OperationModel collectionAggregatorModel = coreExtensionModel.getOperationModel("collectionAggregator").get();

    assertThat(collectionAggregatorModel.getErrorModels(),
               hasItem(newError("CORRELATION_TIMEOUT", "CORE")
                   .withParent(newError("ANY", "CORE").withParent(errorMuleAny).build()).build()));
    assertThat(collectionAggregatorModel.getExecutionType(), is(BLOCKING));

    assertThat(collectionAggregatorModel.getOutput().getType(), instanceOf(DefaultArrayType.class));
    assertThat(collectionAggregatorModel.getOutput().hasDynamicType(), is(false));
    assertThat(collectionAggregatorModel.getOutputAttributes().getType(), instanceOf(VoidType.class));
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

    assertThat(paramModels.get(2).getName(), is("processedGroupsObjectStore"));
    assertThat(paramModels.get(2).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(paramModels.get(2).getType(), instanceOf(DefaultObjectType.class));
    assertThat(paramModels.get(2).getType().getAnnotation(TypeIdAnnotation.class).get().getValue(),
               is(ObjectStore.class.getName()));
    assertThat(paramModels.get(2).isRequired(), is(false));

    assertThat(paramModels.get(3).getName(), is("eventGroupsObjectStore"));
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

    assertThat(foreachModel.getAllParameterModels(), hasSize(6));

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

    assertErrorType(foreachModel.getAllParameterModels().get(4), "ignoreErrorType");
    assertTarget(foreachModel.getAllParameterModels().get(5));
  }

  @Test
  public void flowRef() {
    final OperationModel flowRefModel = coreExtensionModel.getOperationModel("flowRef").get();

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
  public void idempotentMessageValidator() {
    final OperationModel filterModel = coreExtensionModel.getOperationModel("idempotentMessageValidator").get();

    assertOutputSameAsInput(filterModel);

    assertThat(filterModel.getAllParameterModels(), hasSize(4));

    assertThat(filterModel.getAllParameterModels().get(0).getName(), is("idExpression"));
    assertThat(filterModel.getAllParameterModels().get(0).getExpressionSupport(), is(SUPPORTED));
    assertThat(filterModel.getAllParameterModels().get(0).getType(), instanceOf(DefaultObjectType.class));
    assertThat(filterModel.getAllParameterModels().get(0).isRequired(), is(false));

    assertThat(filterModel.getAllParameterModels().get(1).getName(), is("valueExpression"));
    assertThat(filterModel.getAllParameterModels().get(1).getExpressionSupport(), is(SUPPORTED));
    assertThat(filterModel.getAllParameterModels().get(1).getType(), instanceOf(DefaultStringType.class));
    assertThat(filterModel.getAllParameterModels().get(1).isRequired(), is(false));

    assertThat(filterModel.getAllParameterModels().get(2).getName(), is("objectStore"));
    assertThat(filterModel.getAllParameterModels().get(2).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(filterModel.getAllParameterModels().get(2).getType(), instanceOf(DefaultObjectType.class));
    assertThat(filterModel.getAllParameterModels().get(2).isRequired(), is(false));

    assertThat(filterModel.getAllParameterModels().get(3).getName(), is("storePrefix"));
    assertThat(filterModel.getAllParameterModels().get(3).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(filterModel.getAllParameterModels().get(3).getType(), instanceOf(DefaultStringType.class));
    assertThat(filterModel.getAllParameterModels().get(3).isRequired(), is(false));
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
    final RouterModel scatterGatherModel = (RouterModel) coreExtensionModel.getOperationModel("scatterGather").get();

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

  // TODO: MULE-12224 - Provide support for scope as top level elements
  @Test
  public void errorHandler() {
    final ScopeModel errorHandlerModel = (ScopeModel) coreExtensionModel.getOperationModel("errorHandler").get();

    assertThat(errorHandlerModel.getErrorModels(), empty());
    assertThat(errorHandlerModel.getExecutionType(), is(CPU_LITE));

    assertAssociatedProcessorsChangeOutput(errorHandlerModel);

    List<ParameterModel> allParameterModels = errorHandlerModel.getAllParameterModels();
    assertThat(allParameterModels, hasSize(0));
  }

  @Test
  public void onErrorContinue() {
    verifyOnError("onErrorContinue");
  }

  @Test
  public void onErrorPropagate() {
    verifyOnError("onErrorPropagate");
  }

  // TODO: MULE-12265 - Provide support for "exclusive scopes"
  void verifyOnError(String name) {
    final ScopeModel continueModel = (ScopeModel) coreExtensionModel.getOperationModel(name).get();

    assertThat(continueModel.getErrorModels(), empty());
    assertThat(continueModel.getExecutionType(), is(CPU_LITE));

    assertAssociatedProcessorsChangeOutput(continueModel);

    List<ParameterModel> allParameterModels = continueModel.getAllParameterModels();
    assertThat(allParameterModels, hasSize(4));

    ParameterModel type = allParameterModels.get(0);
    assertErrorType(type, "type");

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
    assertThat(model.getOutput().getType(), instanceOf(VoidType.class));
    assertThat(model.getOutput().hasDynamicType(), is(false));
    assertThat(model.getOutputAttributes().getType(), instanceOf(VoidType.class));
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
    assertThat(model.getOutputAttributes().getType(), instanceOf(VoidType.class));
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
               is(Object.class.getName()));
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

  private void assertErrorType(ParameterModel errorTypeParam, String paramName) {
    assertThat(errorTypeParam.getName(), is(paramName));
    assertThat(errorTypeParam.getType(), is(instanceOf(UnionType.class)));
    List<MetadataType> types = ((UnionType) errorTypeParam.getType()).getTypes();
    assertThat(types, hasSize(2));
    assertThat(types.get(0), is(instanceOf(DefaultStringType.class)));
    assertThat(types.get(1), is(instanceOf(DefaultStringType.class)));
    assertThat(types.get(1).getAnnotation(EnumAnnotation.class).get().getValues(), arrayContainingInAnyOrder(
                                                                                                             "ANY",
                                                                                                             "REDELIVERY_EXHAUSTED",
                                                                                                             "TRANSFORMATION",
                                                                                                             "EXPRESSION",
                                                                                                             "SECURITY",
                                                                                                             "CLIENT_SECURITY",
                                                                                                             "SERVER_SECURITY",
                                                                                                             "ROUTING",
                                                                                                             "CONNECTIVITY",
                                                                                                             "RETRY_EXHAUSTED"));
    assertThat(errorTypeParam.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(errorTypeParam.isRequired(), is(false));
  }

  private void assertSchedulingStrategy(ParameterModel paramModel) {
    assertThat(paramModel.getName(), is("schedulingStrategy"));
    assertThat(paramModel.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(paramModel.getType(), instanceOf(DefaultObjectType.class));
    assertThat(paramModel.isRequired(), is(true));
    assertThat(paramModel.getType().getAnnotation(TypeIdAnnotation.class).get().getValue(),
               is(SchedulingStrategy.class.getName()));
  }
}
