/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.extension;

import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.contains;
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
import static org.mule.runtime.api.meta.model.error.ErrorModelBuilder.newError;
import static org.mule.runtime.api.meta.model.operation.ExecutionType.CPU_LITE;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.core.api.config.MuleManifest.getVendorName;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.TRANSFORMATION;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_NAME;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_VERSION;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.getExtensionModel;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_VALUE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.ERROR_HANDLER;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.FLOW;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.OBJECT_STORE;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.PROCESSOR;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.SOURCE;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.annotation.TypeIdAnnotation;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.model.VoidType;
import org.mule.metadata.api.model.impl.DefaultAnyType;
import org.mule.metadata.api.model.impl.DefaultBooleanType;
import org.mule.metadata.api.model.impl.DefaultNumberType;
import org.mule.metadata.api.model.impl.DefaultObjectType;
import org.mule.metadata.api.model.impl.DefaultStringType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.SubTypesModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.nested.NestableElementModel;
import org.mule.runtime.api.meta.model.nested.NestedChainModel;
import org.mule.runtime.api.meta.model.nested.NestedComponentModel;
import org.mule.runtime.api.meta.model.nested.NestedRouteModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.core.api.source.scheduler.Scheduler;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

public class CoreExtensionModelTestCase extends AbstractMuleContextTestCase {

  private static final ErrorModel errorMuleAny = newError("ANY", "MULE").build();

  private static ExtensionModel coreExtensionModel = getExtensionModel();

  @Test
  public void consistentWithManifest() {
    assertThat(coreExtensionModel.getName(), is(MULE_NAME));
    assertThat(coreExtensionModel.getDescription(), is("Mule Runtime and Integration Platform: Core components"));
    assertThat(coreExtensionModel.getVersion(), is(getProductVersion()));
    assertThat(coreExtensionModel.getVendor(), is(getVendorName()));
    assertThat(coreExtensionModel.getCategory(), is(COMMUNITY));
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
               is(Scheduler.class.getName()));

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
    assertThat(cronSchedulerType.getFieldByName("timeZone").get().isRequired(), is(true));
    assertThat(cronSchedulerType.getFieldByName("timeZone").get().getValue(), instanceOf(DefaultStringType.class));

    assertThat(coreExtensionModel.getExternalLibraryModels(), empty());
    assertThat(coreExtensionModel.getImportedTypes(), empty());
    assertThat(coreExtensionModel.getConfigurationModels(), empty());
    assertThat(coreExtensionModel.getOperationModels(), hasSize(8));
    assertThat(coreExtensionModel.getConstructModels(), hasSize(13));
    assertThat(coreExtensionModel.getConnectionProviders(), empty());
    assertThat(coreExtensionModel.getSourceModels(), hasSize(1));

    assertThat(coreExtensionModel.getErrorModels(),
               hasItem(newError(TRANSFORMATION).withParent(errorMuleAny).build()));

    assertThat(coreExtensionModel.getTypes(), hasSize(4));
  }

  @Test
  public void flow() {
    final ConstructModel flow = coreExtensionModel.getConstructModel("flow").get();

    assertThat(flow.getStereotype().getType(), is(FLOW.getType()));
    assertThat(flow.allowsTopLevelDeclaration(), is(true));

    final List<ParameterModel> paramModels = flow.getAllParameterModels();
    assertThat(paramModels, hasSize(2));

    ParameterModel initialState = paramModels.get(0);
    assertThat(initialState.getName(), is("initialState"));
    assertThat(initialState.getDefaultValue(), is("started"));

    ParameterModel maxConcurrency = paramModels.get(1);
    assertThat(maxConcurrency.getName(), is("maxConcurrency"));

    List<? extends NestableElementModel> nestedComponents = flow.getNestedComponents();
    assertThat(nestedComponents, hasSize(3));

    NestableElementModel source = nestedComponents.get(0);
    assertThat(source.getName(), is("source"));
    assertThat(source.isRequired(), is(false));
    assertThat(source, instanceOf(NestedComponentModel.class));
    assertThat(((NestedComponentModel) source).getAllowedStereotypes(), contains(SOURCE));

    NestableElementModel chain = nestedComponents.get(1);
    assertThat(chain.getName(), is("processors"));
    assertThat(chain.isRequired(), is(true));
    assertThat(chain, instanceOf(NestedChainModel.class));
    assertThat(((NestedChainModel) chain).getAllowedStereotypes().stream()
        .anyMatch(s -> s.getType().equals(PROCESSOR.getType())), is(true));

    NestableElementModel errorHandler = nestedComponents.get(2);
    assertThat(errorHandler.getName(), is("errorHandler"));
    assertThat(errorHandler.isRequired(), is(false));
    assertThat(errorHandler, instanceOf(NestedComponentModel.class));
    assertThat(((NestedComponentModel) errorHandler).getAllowedStereotypes(), contains(ERROR_HANDLER));
  }

  @Test
  public void scheduler() {
    final SourceModel schedulerModel = coreExtensionModel.getSourceModel("scheduler").get();
    assertThat(schedulerModel.getStereotype(), is(SOURCE));

    assertThat(schedulerModel.getErrorModels(), empty());
    assertThat(schedulerModel.hasResponse(), is(false));
    assertThat(schedulerModel.getOutput().getType(), instanceOf(DefaultObjectType.class));
    assertThat(schedulerModel.getOutput().hasDynamicType(), is(false));
    assertThat(schedulerModel.getOutputAttributes().getType(), instanceOf(DefaultObjectType.class));
    assertThat(schedulerModel.getOutputAttributes().hasDynamicType(), is(false));

    final List<ParameterModel> paramModels = schedulerModel.getAllParameterModels();
    assertThat(paramModels, hasSize(1));
    assertSchedulingStrategy(paramModels.get(0));
  }

  @Test
  public void logger() {
    final OperationModel loggerModel = coreExtensionModel.getOperationModel("logger").get();
    assertThat(loggerModel.getStereotype(), is(PROCESSOR));

    assertThat(loggerModel.getErrorModels(), empty());
    assertThat(loggerModel.getExecutionType(), is(CPU_LITE));

    assertOutputSameAsInput(loggerModel);

    final List<ParameterModel> paramModels = loggerModel.getAllParameterModels();
    assertThat(paramModels, hasSize(3));

    assertThat(paramModels.get(0).getName(), is("message"));
    assertThat(paramModels.get(0).getExpressionSupport(), is(SUPPORTED));
    assertThat(paramModels.get(0).getType(), instanceOf(StringType.class));
    assertThat(paramModels.get(0).isRequired(), is(false));

    assertThat(paramModels.get(1).getName(), is("level"));
    assertThat(paramModels.get(1).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(paramModels.get(1).getType(), instanceOf(DefaultStringType.class));
    assertThat(paramModels.get(1).isRequired(), is(false));

    assertThat(paramModels.get(2).getName(), is("category"));
    assertThat(paramModels.get(2).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(paramModels.get(2).getType(), instanceOf(StringType.class));
    assertThat(paramModels.get(2).isRequired(), is(false));
  }

  @Test
  public void raiseError() {
    final OperationModel raiseErrorModel = coreExtensionModel.getOperationModel("raiseError").get();
    assertThat(raiseErrorModel.getStereotype(), is(PROCESSOR));

    assertThat(raiseErrorModel.getErrorModels(), empty());
    assertThat(raiseErrorModel.getExecutionType(), is(CPU_LITE));

    assertOutputSameAsInput(raiseErrorModel);

    final List<ParameterModel> paramModels = raiseErrorModel.getAllParameterModels();
    assertThat(paramModels, hasSize(2));

    assertThat(paramModels.get(0).getName(), is("type"));
    assertThat(paramModels.get(0).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(paramModels.get(0).getType(), instanceOf(DefaultStringType.class));
    assertThat(paramModels.get(0).isRequired(), is(true));

    assertThat(paramModels.get(1).getName(), is("description"));
    assertThat(paramModels.get(1).getExpressionSupport(), is(SUPPORTED));
    assertThat(paramModels.get(1).getType(), instanceOf(StringType.class));
    assertThat(paramModels.get(1).isRequired(), is(false));
  }

  @Test
  public void foreach() {
    final ConstructModel foreach = coreExtensionModel.getConstructModel("foreach").get();

    assertThat(foreach.getNestedComponents().size(), is(1));
    NestableElementModel processorsChain = foreach.getNestedComponents().get(0);
    assertThat(processorsChain, instanceOf(NestedChainModel.class));
    assertThat(processorsChain.isRequired(), is(true));

    assertThat(foreach.getAllParameterModels(), hasSize(4));

    ParameterModel collection = foreach.getAllParameterModels().get(0);
    assertThat(collection.getName(), is("collection"));
    assertThat(collection.getExpressionSupport(), is(REQUIRED));
    assertThat(collection.getType(), instanceOf(ArrayType.class));
    assertThat(collection.getType().getAnnotation(ClassInformationAnnotation.class)
        .map(ClassInformationAnnotation::getClassname)
        .orElse(""), is(Iterable.class.getName()));
    assertThat(collection.isRequired(), is(false));

    ParameterModel batchSize = foreach.getAllParameterModels().get(1);
    assertThat(batchSize.getName(), is("batchSize"));
    assertThat(batchSize.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(batchSize.getType(), instanceOf(DefaultNumberType.class));
    assertThat(batchSize.isRequired(), is(false));

    ParameterModel rootMessageName = foreach.getAllParameterModels().get(2);
    assertThat(rootMessageName.getName(), is("rootMessageVariableName"));
    assertThat(rootMessageName.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(rootMessageName.getType(), instanceOf(DefaultStringType.class));
    assertThat(rootMessageName.isRequired(), is(false));

    ParameterModel counter = foreach.getAllParameterModels().get(3);
    assertThat(counter.getName(), is("counterVariableName"));
    assertThat(counter.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(counter.getType(), instanceOf(DefaultStringType.class));
    assertThat(counter.isRequired(), is(false));
  }

  @Test
  public void flowRef() {
    final OperationModel flowRefModel = coreExtensionModel.getOperationModel("flowRef").get();
    assertThat(flowRefModel.getStereotype(), is(PROCESSOR));

    assertAssociatedProcessorsChangeOutput(flowRefModel);

    assertThat(flowRefModel.getAllParameterModels(), hasSize(3));

    assertThat(flowRefModel.getAllParameterModels().get(0).getName(), is("name"));
    assertThat(flowRefModel.getAllParameterModels().get(0).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(flowRefModel.getAllParameterModels().get(0).getType(), instanceOf(DefaultStringType.class));
    assertThat(flowRefModel.getAllParameterModels().get(0).isRequired(), is(true));

    assertTarget(flowRefModel.getAllParameterModels().get(1));
  }

  @Test
  public void idempotentMessageValidator() {
    final OperationModel filterModel = coreExtensionModel.getOperationModel("idempotentMessageValidator").get();
    assertThat(filterModel.getStereotype(), is(PROCESSOR));

    assertOutputSameAsInput(filterModel);

    assertThat(filterModel.getAllParameterModels(), hasSize(4));

    assertThat(filterModel.getAllParameterModels().get(0).getName(), is("idExpression"));
    assertThat(filterModel.getAllParameterModels().get(0).getExpressionSupport(), is(SUPPORTED));
    assertThat(filterModel.getAllParameterModels().get(0).getType(), instanceOf(DefaultStringType.class));
    assertThat(filterModel.getAllParameterModels().get(0).isRequired(), is(false));

    assertThat(filterModel.getAllParameterModels().get(1).getName(), is("valueExpression"));
    assertThat(filterModel.getAllParameterModels().get(1).getExpressionSupport(), is(SUPPORTED));
    assertThat(filterModel.getAllParameterModels().get(1).getType(), instanceOf(DefaultStringType.class));
    assertThat(filterModel.getAllParameterModels().get(1).isRequired(), is(false));

    assertThat(filterModel.getAllParameterModels().get(2).getName(), is("storePrefix"));
    assertThat(filterModel.getAllParameterModels().get(2).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(filterModel.getAllParameterModels().get(2).getType(), instanceOf(DefaultStringType.class));
    assertThat(filterModel.getAllParameterModels().get(2).isRequired(), is(false));

    assertThat(filterModel.getAllParameterModels().get(3).getName(), is("objectStore"));
    assertThat(filterModel.getAllParameterModels().get(3).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(filterModel.getAllParameterModels().get(3).getType(), instanceOf(DefaultObjectType.class));
    assertThat(filterModel.getAllParameterModels().get(3).isRequired(), is(false));
    assertThat(filterModel.getAllParameterModels().get(3).getAllowedStereotypes().size(), is(1));
    assertThat(filterModel.getAllParameterModels().get(3).getAllowedStereotypes().get(0), is(OBJECT_STORE));
  }

  @Test
  public void choice() {
    final ConstructModel choiceModel = coreExtensionModel.getConstructModel("choice").get();

    assertThat(choiceModel.allowsTopLevelDeclaration(), is(false));
    assertThat(choiceModel.getAllParameterModels(), empty());
    assertThat(choiceModel.getNestedComponents(), hasSize(2));

    final NestedRouteModel whenRouteModel = (NestedRouteModel) choiceModel.getNestedComponents().get(0);
    assertThat(whenRouteModel.getName(), is("when"));
    assertThat(whenRouteModel.getMinOccurs(), is(1));
    assertThat(whenRouteModel.getMaxOccurs(), is(Optional.empty()));
    assertThat(whenRouteModel.getAllParameterModels(), hasSize(1));

    assertThat(whenRouteModel.getAllParameterModels().get(0).getName(), is("expression"));
    assertThat(whenRouteModel.getAllParameterModels().get(0).getExpressionSupport(), is(SUPPORTED));
    assertThat(whenRouteModel.getAllParameterModels().get(0).getType(), instanceOf(DefaultBooleanType.class));
    assertThat(whenRouteModel.getAllParameterModels().get(0).isRequired(), is(true));

    final NestedRouteModel otherwiseRouteModel = (NestedRouteModel) choiceModel.getNestedComponents().get(1);
    assertThat(otherwiseRouteModel.getName(), is("otherwise"));
    assertThat(otherwiseRouteModel.getMinOccurs(), is(0));
    assertThat(otherwiseRouteModel.getMaxOccurs().get(), is(1));
    assertThat(otherwiseRouteModel.getAllParameterModels(), empty());
  }

  @Test
  public void scatterGather() {
    final ConstructModel scatterGatherModel = coreExtensionModel.getConstructModel("scatterGather").get();


    assertThat(scatterGatherModel.getAllParameterModels(), hasSize(4));

    assertThat(scatterGatherModel.getAllParameterModels().get(0).getName(), is("timeout"));
    assertThat(scatterGatherModel.getAllParameterModels().get(0).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(scatterGatherModel.getAllParameterModels().get(0).getType(), instanceOf(DefaultNumberType.class));
    assertThat(scatterGatherModel.getAllParameterModels().get(0).isRequired(), is(false));

    assertThat(scatterGatherModel.getAllParameterModels().get(1).getName(), is("maxConcurrency"));
    assertThat(scatterGatherModel.getAllParameterModels().get(1).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(scatterGatherModel.getAllParameterModels().get(1).getType(), instanceOf(DefaultNumberType.class));
    assertThat(scatterGatherModel.getAllParameterModels().get(1).isRequired(), is(false));

    assertThat(scatterGatherModel.getAllParameterModels().get(2).getName(), is(TARGET_PARAMETER_NAME));
    assertThat(scatterGatherModel.getAllParameterModels().get(2).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(scatterGatherModel.getAllParameterModels().get(2).getType(), instanceOf(DefaultStringType.class));
    assertThat(scatterGatherModel.getAllParameterModels().get(2).isRequired(), is(false));

    assertThat(scatterGatherModel.getAllParameterModels().get(3).getName(), is(TARGET_VALUE_PARAMETER_NAME));
    assertThat(scatterGatherModel.getAllParameterModels().get(3).getExpressionSupport(), is(REQUIRED));
    assertThat(scatterGatherModel.getAllParameterModels().get(3).getType(), instanceOf(StringType.class));
    assertThat(scatterGatherModel.getAllParameterModels().get(3).isRequired(), is(false));

    assertThat(scatterGatherModel.getNestedComponents(), hasSize(1));

    final NestedRouteModel routeModel = (NestedRouteModel) scatterGatherModel.getNestedComponents().get(0);
    assertThat(routeModel.getName(), is("route"));
    assertThat(routeModel.getMinOccurs(), is(2));
    assertThat(routeModel.getMaxOccurs(), is(Optional.empty()));
    assertThat(routeModel.getAllParameterModels(), empty());
  }

  @Test
  public void async() {
    final ConstructModel asyncModel = coreExtensionModel.getConstructModel("async").get();

    assertThat(asyncModel.getNestedComponents(), hasSize(1));
    NestableElementModel processors = asyncModel.getNestedComponents().get(0);
    assertThat(processors, instanceOf(NestedChainModel.class));
    assertThat(processors.isRequired(), is(true));

    assertThat(asyncModel.getAllParameterModels(), hasSize(1));
    assertThat(asyncModel.getAllParameterModels().get(0).getName(), is("name"));
    assertThat(asyncModel.getAllParameterModels().get(0).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(asyncModel.getAllParameterModels().get(0).getType(), instanceOf(DefaultStringType.class));
    assertThat(asyncModel.getAllParameterModels().get(0).isRequired(), is(false));
  }

  @Test
  public void tryScope() {
    final ConstructModel tryModel = coreExtensionModel.getConstructModel("try").get();

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

  @Test
  public void untilSuccessful() {
    final ConstructModel tryModel = coreExtensionModel.getConstructModel("untilSuccessful").get();

    List<ParameterModel> allParameterModels = tryModel.getAllParameterModels();
    assertThat(allParameterModels, hasSize(2));

    ParameterModel action = allParameterModels.get(0);
    assertThat(action.getName(), is("maxRetries"));
    assertThat(action.getType(), is(instanceOf(DefaultNumberType.class)));
    assertThat(action.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(action.getDefaultValue(), is(5));
    assertThat(action.isRequired(), is(false));

    ParameterModel type = allParameterModels.get(1);
    assertThat(type.getName(), is("millisBetweenRetries"));
    assertThat(type.getType(), is(instanceOf(DefaultNumberType.class)));
    assertThat(type.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(type.getDefaultValue(), is(60000));
    assertThat(type.isRequired(), is(false));
  }

  @Test
  public void firstSuccessful() {
    final ConstructModel tryModel = coreExtensionModel.getConstructModel("firstSuccessful").get();

    List<ParameterModel> allParameterModels = tryModel.getAllParameterModels();
    assertThat(allParameterModels, hasSize(0));
  }

  @Test
  public void errorHandler() {
    final ConstructModel errorHandlerModel = coreExtensionModel.getConstructModel("errorHandler").get();

    assertThat(errorHandlerModel.allowsTopLevelDeclaration(), is(true));
    assertThat(errorHandlerModel.getStereotype().getType(), is(ERROR_HANDLER.getType()));

    assertThat(errorHandlerModel.getAllParameterModels(), hasSize(0));

    assertThat(errorHandlerModel.getNestedComponents(), hasSize(2));
    NestedRouteModel onErrorContinue = (NestedRouteModel) errorHandlerModel.getNestedComponents().get(0);
    NestedRouteModel onErrorPropagate = (NestedRouteModel) errorHandlerModel.getNestedComponents().get(1);

    verifyOnError(onErrorContinue);
    verifyOnError(onErrorPropagate);
  }

  void verifyOnError(NestedRouteModel route) {
    List<ParameterModel> allParameterModels = route.getAllParameterModels();
    assertThat(allParameterModels, hasSize(4));

    ParameterModel when = allParameterModels.get(0);
    assertThat(when.getName(), is("when"));
    assertThat(when.getType(), is(instanceOf(DefaultStringType.class)));
    assertThat(when.getExpressionSupport(), is(SUPPORTED));
    assertThat(when.isRequired(), is(false));

    ParameterModel type = allParameterModels.get(1);
    assertErrorType(type, "type");

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
    assertThat(notifications.getDefaultValue(), is(true));
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
    assertThat(model.getOutputAttributes().getType(), instanceOf(DefaultAnyType.class));
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

  private void assertTarget(final ParameterModel targetParameterModel) {
    assertThat(targetParameterModel.getName(), is("target"));
    assertThat(targetParameterModel.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(targetParameterModel.getType(), instanceOf(DefaultStringType.class));
    assertThat(targetParameterModel.getType(), is(instanceOf(StringType.class)));
    assertThat(targetParameterModel.isRequired(), is(false));
  }

  private void assertErrorType(ParameterModel errorTypeParam, String paramName) {
    assertThat(errorTypeParam.getName(), is(paramName));
    assertThat(errorTypeParam.getType(), is(instanceOf(DefaultStringType.class)));
    assertThat(errorTypeParam.getType().getAnnotation(EnumAnnotation.class).get().getValues(), arrayContainingInAnyOrder(
                                                                                                                         "ANY",
                                                                                                                         "REDELIVERY_EXHAUSTED",
                                                                                                                         "TRANSFORMATION",
                                                                                                                         "EXPRESSION",
                                                                                                                         "SECURITY",
                                                                                                                         "CLIENT_SECURITY",
                                                                                                                         "SERVER_SECURITY",
                                                                                                                         "ROUTING",
                                                                                                                         "CONNECTIVITY",
                                                                                                                         "RETRY_EXHAUSTED",
                                                                                                                         "TIMEOUT"));
    assertThat(errorTypeParam.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(errorTypeParam.isRequired(), is(false));
  }

  private void assertSchedulingStrategy(ParameterModel paramModel) {
    assertThat(paramModel.getName(), is("schedulingStrategy"));
    assertThat(paramModel.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(paramModel.getType(), instanceOf(DefaultObjectType.class));
    assertThat(paramModel.isRequired(), is(true));
    assertThat(paramModel.getType().getAnnotation(TypeIdAnnotation.class).get().getValue(),
               is(Scheduler.class.getName()));
  }
}
