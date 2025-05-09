/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.extension;

import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.ExpressionSupport.REQUIRED;
import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.api.meta.model.error.ErrorModelBuilder.newError;
import static org.mule.runtime.api.meta.model.nested.ChainExecutionOccurrence.AT_LEAST_ONCE;
import static org.mule.runtime.api.meta.model.nested.ChainExecutionOccurrence.MULTIPLE_OR_NONE;
import static org.mule.runtime.api.meta.model.nested.ChainExecutionOccurrence.ONCE;
import static org.mule.runtime.api.meta.model.nested.ChainExecutionOccurrence.ONCE_OR_NONE;
import static org.mule.runtime.api.meta.model.operation.ExecutionType.CPU_LITE;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.OUTPUT;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.TRANSFORMATION;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.ANY_TYPE;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.BOOLEAN_TYPE;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.INTEGER_TYPE;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.MULE_NAME;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.MULE_VERSION;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.STRING_TYPE;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.VOID_TYPE;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.getExtensionModel;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.setConfigurerFactory;
import static org.mule.runtime.core.extension.ComponentConfigurerTestUtils.createMockedFactory;
import static org.mule.runtime.extension.api.ExtensionConstants.ERROR_MAPPINGS_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_VALUE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.error.ErrorConstants.ERROR;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.ERROR_HANDLER;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.FLOW;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.OBJECT_STORE;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.ON_ERROR;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.PROCESSOR;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.SOURCE;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.SUB_FLOW;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.CORE_PREFIX;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsIterableContaining.hasItem;

import org.hamcrest.CoreMatchers;
import org.junit.BeforeClass;
import org.mule.metadata.api.annotation.DefaultValueAnnotation;
import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.annotation.TypeIdAnnotation;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.BooleanType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.model.VoidType;
import org.mule.metadata.api.model.impl.DefaultAnyType;
import org.mule.metadata.api.model.impl.DefaultArrayType;
import org.mule.metadata.api.model.impl.DefaultBooleanType;
import org.mule.metadata.api.model.impl.DefaultNumberType;
import org.mule.metadata.api.model.impl.DefaultObjectType;
import org.mule.metadata.api.model.impl.DefaultStringType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.HasOutputModel;
import org.mule.runtime.api.meta.model.SubTypesModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.function.FunctionModel;
import org.mule.runtime.api.meta.model.nested.NestableElementModel;
import org.mule.runtime.api.meta.model.nested.NestedChainModel;
import org.mule.runtime.api.meta.model.nested.NestedComponentModel;
import org.mule.runtime.api.meta.model.nested.NestedRouteModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ExclusiveParametersModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.api.scheduler.SchedulingStrategy;
import org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider;
import org.mule.runtime.extension.api.property.SinceMuleVersionModelProperty;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;

import io.qameta.allure.Issue;

public class CoreExtensionModelTestCase {

  private static final ErrorModel errorMuleAny = newError("ANY", "MULE").build();

  private static ExtensionModel coreExtensionModel;

  @BeforeClass
  public static void setupClass() {
    setConfigurerFactory(createMockedFactory());
    coreExtensionModel = getExtensionModel();
  }

  @Test
  public void consistentWithManifest() {
    assertThat(coreExtensionModel.getName(), is(MULE_NAME));
    assertThat(coreExtensionModel.getDescription(), is("Mule Runtime and Integration Platform: Core components"));
    assertThat(coreExtensionModel.getVersion(), is(MuleExtensionModelProvider.MULE_VERSION));
    assertThat(coreExtensionModel.getVendor(), is(MuleExtensionModelProvider.MULESOFT_VENDOR));
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
               is(SchedulingStrategy.class.getName()));

    assertThat(subTypesModel.getSubTypes(), hasSize(2));
    Iterator<ObjectType> iterator = subTypesModel.getSubTypes().iterator();
    final DefaultObjectType ffSchedulerType = (DefaultObjectType) iterator.next();
    assertThat(ffSchedulerType.getFields(), hasSize(3));
    assertThat(ffSchedulerType.getFieldByName("frequency").get().isRequired(), is(false));
    assertThat(ffSchedulerType.getFieldByName("frequency").get().getValue(), instanceOf(DefaultNumberType.class));
    assertThat(ffSchedulerType.getFieldByName("frequency").get().getAnnotation(DefaultValueAnnotation.class).get().getValue(),
               is("60000"));
    assertThat(ffSchedulerType.getFieldByName("startDelay").get().isRequired(), is(false));
    assertThat(ffSchedulerType.getFieldByName("startDelay").get().getValue(), instanceOf(DefaultNumberType.class));
    assertThat(ffSchedulerType.getFieldByName("startDelay").get().getAnnotation(DefaultValueAnnotation.class).get().getValue(),
               is("0"));
    assertThat(ffSchedulerType.getFieldByName("timeUnit").get().isRequired(), is(false));
    assertThat(ffSchedulerType.getFieldByName("timeUnit").get().getValue(), instanceOf(DefaultStringType.class));
    assertThat(ffSchedulerType.getFieldByName("timeUnit").get().getAnnotation(DefaultValueAnnotation.class).get().getValue(),
               is("MILLISECONDS"));

    final DefaultObjectType cronSchedulerType = (DefaultObjectType) iterator.next();
    assertThat(cronSchedulerType.getFields(), hasSize(2));
    assertThat(cronSchedulerType.getFieldByName("expression").get().isRequired(), is(true));
    assertThat(cronSchedulerType.getFieldByName("expression").get().getValue(), instanceOf(DefaultStringType.class));
    assertThat(cronSchedulerType.getFieldByName("timeZone").get().isRequired(), is(false));
    assertThat(cronSchedulerType.getFieldByName("timeZone").get().getValue(), instanceOf(DefaultStringType.class));

    assertThat(coreExtensionModel.getExternalLibraryModels(), empty());
    assertThat(coreExtensionModel.getImportedTypes(), empty());
    assertThat(coreExtensionModel.getConfigurationModels(), empty());
    assertThat(coreExtensionModel.getOperationModels(), hasSize(17));
    assertThat(coreExtensionModel.getConstructModels(), hasSize(12));
    assertThat(coreExtensionModel.getConnectionProviders(), empty());
    assertThat(coreExtensionModel.getSourceModels(), hasSize(1));

    assertThat(coreExtensionModel.getErrorModels(),
               hasItem(newError(TRANSFORMATION).withParent(errorMuleAny).build()));

    assertThat(coreExtensionModel.getTypes(), hasSize(6));
  }

  @Test
  public void flow() {
    final ConstructModel flow = coreExtensionModel.getConstructModel("flow").get();

    assertThat(flow.getStereotype().getType(), is(FLOW.getType()));
    assertThat(flow.allowsTopLevelDeclaration(), is(true));

    final List<ParameterModel> paramModels = flow.getAllParameterModels();
    assertThat(paramModels, hasSize(4));

    ParameterModel nameParam = paramModels.get(0);
    assertThat(nameParam.getName(), is("name"));
    assertThat(nameParam.getDefaultValue(), is(nullValue()));
    assertThat(nameParam.isComponentId(), is(true));

    ParameterModel initialState = paramModels.get(1);
    assertThat(initialState.getName(), is("initialState"));
    assertThat(initialState.getDefaultValue(), is("started"));

    ParameterModel maxConcurrency = paramModels.get(2);
    assertThat(maxConcurrency.getName(), is("maxConcurrency"));

    ParameterModel trackingEnableDefaultEvents = paramModels.get(3);
    assertThat(trackingEnableDefaultEvents.getName(), is("enableDefaultEvents"));

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
  public void subFlow() {
    final ConstructModel subFlow = coreExtensionModel.getConstructModel("subFlow").get();

    assertThat(subFlow.getStereotype().getType(), is(SUB_FLOW.getType()));
    assertThat(subFlow.allowsTopLevelDeclaration(), is(true));

    final List<ParameterModel> paramModels = subFlow.getAllParameterModels();
    assertThat(paramModels, hasSize(1));

    ParameterModel nameParam = paramModels.get(0);
    assertThat(nameParam.getName(), is("name"));
    assertThat(nameParam.getDefaultValue(), is(nullValue()));
    assertThat(nameParam.isComponentId(), is(true));

    List<? extends NestableElementModel> nestedComponents = subFlow.getNestedComponents();
    assertThat(nestedComponents, hasSize(1));

    NestableElementModel chain = nestedComponents.get(0);
    assertThat(chain.getName(), is("processors"));
    assertThat(chain.isRequired(), is(true));
    assertThat(chain, instanceOf(NestedChainModel.class));
    assertThat(((NestedChainModel) chain).getAllowedStereotypes().stream()
        .anyMatch(s -> s.getType().equals(PROCESSOR.getType())), is(true));
  }

  @Test
  public void scheduler() {
    final SourceModel schedulerModel = coreExtensionModel.getSourceModel("scheduler").get();
    assertSteretorype(schedulerModel.getStereotype(), "SCHEDULER", SOURCE);

    assertOutputTypes(schedulerModel, VOID_TYPE, VOID_TYPE);
    assertThat(schedulerModel.getErrorModels(), empty());
    assertThat(schedulerModel.hasResponse(), is(false));

    final List<ParameterModel> paramModels = schedulerModel.getAllParameterModels();
    assertThat(paramModels, hasSize(2));
    assertSchedulingStrategy(paramModels.get(0));
    assertSchedulingDisallowConcurrentExecution(paramModels.get(1));
  }

  private void assertSteretorype(StereotypeModel stereotypeModel, String type, StereotypeModel parent) {
    assertThat(stereotypeModel.getType(), equalTo(type));
    assertThat(stereotypeModel.getNamespace(), equalTo("MULE"));
    if (parent != null) {
      assertThat(stereotypeModel.getParent().get(), is(parent));
    } else {
      assertThat(stereotypeModel.getParent().isPresent(), is(false));
    }
  }

  @Test
  public void logger() {
    final OperationModel loggerModel = coreExtensionModel.getOperationModel("logger").get();
    assertSteretorype(loggerModel.getStereotype(), "LOGGER", PROCESSOR);

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
  public void object() {
    final ConstructModel object = coreExtensionModel.getConstructModel("object").get();

    assertThat(object.allowsTopLevelDeclaration(), is(true));

    final List<ParameterModel> paramModels = object.getAllParameterModels();
    assertThat(paramModels, hasSize(4));

    ParameterModel name = paramModels.get(0);
    assertThat(name.getName(), is("name"));
    assertThat(name.isRequired(), is(true));
    assertThat(name.isComponentId(), is(true));
    assertThat(name.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(name.getType(), instanceOf(DefaultStringType.class));

    ParameterModel ref = paramModels.get(1);
    assertThat(ref.getName(), is("ref"));
    assertThat(ref.isRequired(), is(false));
    assertThat(ref.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(ref.getType(), instanceOf(DefaultStringType.class));

    ParameterModel clazz = paramModels.get(2);
    assertThat(clazz.getName(), is("class"));
    assertThat(clazz.isRequired(), is(false));
    assertThat(clazz.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(clazz.getType(), instanceOf(DefaultStringType.class));

    List<ExclusiveParametersModel> exclusiveParametersModels =
        object.getParameterGroupModels().get(0).getExclusiveParametersModels();
    ExclusiveParametersModel exclusiveParameterModel = exclusiveParametersModels.get(0);
    assertThat(exclusiveParameterModel.getExclusiveParameterNames(), contains("ref", "class"));
    assertThat(exclusiveParameterModel.isOneRequired(), is(true));

    ParameterModel properties = paramModels.get(3);
    assertThat(properties.getName(), is("property"));
    assertThat(properties.isRequired(), is(false));
    assertThat(properties.getType(), instanceOf(DefaultObjectType.class));
    assertThat(properties.getType().getAnnotations(), hasSize(1));
    assertThat(properties.getType().getAnnotation(ClassInformationAnnotation.class).get().getClassname(),
               is(Map.class.getName()));
  }

  @Test
  public void raiseError() {
    final OperationModel raiseErrorModel = coreExtensionModel.getOperationModel("raiseError").get();
    assertSteretorype(raiseErrorModel.getStereotype(), "RAISE_ERROR", PROCESSOR);

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
    final OperationModel foreach = coreExtensionModel.getOperationModel("foreach").get();
    assertThat(foreach.isBlocking(), is(false));

    assertThat(foreach.getNestedComponents().size(), is(1));
    NestableElementModel processorsChain = foreach.getNestedComponents().get(0);
    assertThat(processorsChain, instanceOf(NestedChainModel.class));
    assertThat(processorsChain.isRequired(), is(true));

    assertThat(foreach.getAllParameterModels(), hasSize(5));

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

    assertErrorMappings(foreach.getAllParameterModels().get(4));

    assertThat(foreach.getNestedComponents(), hasSize(1));
    NestedChainModel chain = (NestedChainModel) foreach.getNestedComponents().get(0);
    assertThat(chain.getChainExecutionOccurrence(), is(MULTIPLE_OR_NONE));
  }

  @Test
  public void flowRef() {
    final OperationModel flowRefModel = coreExtensionModel.getOperationModel("flowRef").get();
    assertSteretorype(flowRefModel.getStereotype(), "FLOW_REF", PROCESSOR);

    assertAssociatedProcessorsChangeOutput(flowRefModel);

    assertThat(flowRefModel.getAllParameterModels(), hasSize(3));

    assertThat(flowRefModel.getAllParameterModels().get(0).getName(), is("name"));
    assertThat(flowRefModel.getAllParameterModels().get(0).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(flowRefModel.getAllParameterModels().get(0).getType(), instanceOf(DefaultStringType.class));
    assertThat(flowRefModel.getAllParameterModels().get(0).isRequired(), is(true));
    assertThat(flowRefModel.getAllParameterModels().get(0).getAllowedStereotypes(), is(asList(FLOW, SUB_FLOW)));

    assertTarget(flowRefModel.getAllParameterModels().get(1));
  }

  @Test
  public void idempotentMessageValidator() {
    final OperationModel filterModel = coreExtensionModel.getOperationModel("idempotentMessageValidator").get();
    assertSteretorype(filterModel.getStereotype(), "IDEMPOTENT_MESSAGE_VALIDATOR", PROCESSOR);

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

  private void assertOutputTypes(HasOutputModel model, MetadataType payload, MetadataType attributes) {
    assertThat(model.getOutput().getType(), equalTo(payload));
    assertThat(model.getOutputAttributes().getType(), equalTo(attributes));
  }

  @Test
  public void choice() {
    final OperationModel choiceModel = coreExtensionModel.getOperationModel("choice").get();
    assertThat(choiceModel.isBlocking(), is(false));

    final List<ParameterModel> parameterModels = choiceModel.getAllParameterModels();
    assertThat(parameterModels, hasSize(4));

    Set<String> parameterNames = parameterModels.stream().map(NamedObject::getName).collect(toSet());
    assertThat(parameterNames,
               containsInAnyOrder("enableDefaultEvents", TARGET_PARAMETER_NAME, TARGET_VALUE_PARAMETER_NAME,
                                  ERROR_MAPPINGS_PARAMETER_NAME));

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
    assertThat(whenRouteModel.getNestedComponents(), hasSize(1));
    final NestedChainModel whenChainModel = (NestedChainModel) whenRouteModel.getNestedComponents().get(0);
    assertThat(whenChainModel.getChainExecutionOccurrence(), is(ONCE_OR_NONE));

    final NestedRouteModel otherwiseRouteModel = (NestedRouteModel) choiceModel.getNestedComponents().get(1);
    assertThat(otherwiseRouteModel.getName(), is("otherwise"));
    assertThat(otherwiseRouteModel.getMinOccurs(), is(0));
    assertThat(otherwiseRouteModel.getMaxOccurs().get(), is(1));
    assertThat(otherwiseRouteModel.getAllParameterModels(), empty());
    final NestedChainModel otherwiseChainModel = (NestedChainModel) otherwiseRouteModel.getNestedComponents().get(0);
    assertThat(otherwiseChainModel.getChainExecutionOccurrence(), is(ONCE_OR_NONE));
  }

  @Test
  public void scatterGather() {
    final OperationModel scatterGatherModel = coreExtensionModel.getOperationModel("scatterGather").get();
    assertThat(scatterGatherModel.isBlocking(), is(false));

    assertThat(scatterGatherModel.getAllParameterModels(), hasSize(6));

    assertThat(scatterGatherModel.getAllParameterModels().get(0).getName(), is("timeout"));
    assertThat(scatterGatherModel.getAllParameterModels().get(0).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(scatterGatherModel.getAllParameterModels().get(0).getType(), instanceOf(DefaultNumberType.class));
    assertThat(scatterGatherModel.getAllParameterModels().get(0).isRequired(), is(false));

    assertThat(scatterGatherModel.getAllParameterModels().get(1).getName(), is("maxConcurrency"));
    assertThat(scatterGatherModel.getAllParameterModels().get(1).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(scatterGatherModel.getAllParameterModels().get(1).getType(), instanceOf(DefaultNumberType.class));
    assertThat(scatterGatherModel.getAllParameterModels().get(1).isRequired(), is(false));

    assertThat(scatterGatherModel.getAllParameterModels().get(2).getName(), is("collectList"));
    assertThat(scatterGatherModel.getAllParameterModels().get(2).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(scatterGatherModel.getAllParameterModels().get(2).getType(), instanceOf(DefaultObjectType.class));
    assertThat(scatterGatherModel.getAllParameterModels().get(2).isRequired(), is(false));

    assertTarget(scatterGatherModel.getAllParameterModels().get(3));
    assertTargetValue(scatterGatherModel.getAllParameterModels().get(4));
    assertErrorMappings(scatterGatherModel.getAllParameterModels().get(5));

    assertThat(scatterGatherModel.getNestedComponents(), hasSize(1));

    final NestedRouteModel routeModel = (NestedRouteModel) scatterGatherModel.getNestedComponents().get(0);
    assertThat(routeModel.getName(), is("route"));
    assertThat(routeModel.getMinOccurs(), is(2));
    assertThat(routeModel.getMaxOccurs(), is(Optional.empty()));
    assertThat(routeModel.getAllParameterModels(), empty());
    assertThat(routeModel.getNestedComponents(), hasSize(1));

    final NestedChainModel chainModel = (NestedChainModel) routeModel.getNestedComponents().get(0);
    assertThat(chainModel.getChainExecutionOccurrence(), is(ONCE));
  }

  @Test
  @Issue("MULE-19653")
  public void scatterGatherOutputParams() {
    final OperationModel scatterGatherModel = coreExtensionModel.getOperationModel("scatterGather").get();

    ParameterGroupModel outputGroup =
        scatterGatherModel.getParameterGroupModels().stream().filter(pg -> pg.getName().equals(OUTPUT)).findAny().get();

    assertThat(outputGroup.getParameter(TARGET_PARAMETER_NAME).isPresent(), is(true));
    assertThat(outputGroup.getParameter(TARGET_VALUE_PARAMETER_NAME).isPresent(), is(true));
  }

  @Test
  public void parallelForeach() {
    final OperationModel parallelForeach = coreExtensionModel.getOperationModel("parallelForeach").get();
    assertThat(parallelForeach.isBlocking(), is(false));

    assertThat(parallelForeach.getModelProperty(SinceMuleVersionModelProperty.class).map(mp -> mp.getVersion().toString())
        .orElse("NO MODEL PROPERTY"), equalTo("4.2.0"));

    NestedChainModel processorsChain = (NestedChainModel) parallelForeach.getNestedComponents().get(0);
    assertThat(processorsChain, instanceOf(NestedChainModel.class));
    assertThat(processorsChain.isRequired(), is(true));
    assertThat(processorsChain.getChainExecutionOccurrence(), is(MULTIPLE_OR_NONE));

    assertThat(parallelForeach.getAllParameterModels(), hasSize(6));

    final ParameterModel collection = parallelForeach.getAllParameterModels().get(0);
    assertThat(collection.getName(), is("collection"));
    assertThat(collection.getExpressionSupport(), is(REQUIRED));
    assertThat(collection.getType(), instanceOf(ArrayType.class));
    assertThat(collection.getType().getAnnotation(ClassInformationAnnotation.class)
        .map(ClassInformationAnnotation::getClassname)
        .orElse(""), is(Iterable.class.getName()));
    assertThat(collection.isRequired(), is(false));

    final ParameterModel timeout = parallelForeach.getAllParameterModels().get(1);
    assertThat(timeout.getName(), is("timeout"));
    assertThat(timeout.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(timeout.getType(), instanceOf(DefaultNumberType.class));
    assertThat(timeout.isRequired(), is(false));

    final ParameterModel maxConcurrency = parallelForeach.getAllParameterModels().get(2);
    assertThat(maxConcurrency.getName(), is("maxConcurrency"));
    assertThat(maxConcurrency.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(maxConcurrency.getType(), instanceOf(DefaultNumberType.class));
    assertThat(maxConcurrency.isRequired(), is(false));

    assertTarget(parallelForeach.getAllParameterModels().get(3));
    assertTargetValue(parallelForeach.getAllParameterModels().get(4));
    assertErrorMappings(parallelForeach.getAllParameterModels().get(5));
  }

  @Test
  @Issue("MULE-19653")
  public void parallelForeachOutputParams() {
    final OperationModel parallelForeach = coreExtensionModel.getOperationModel("parallelForeach").get();

    ParameterGroupModel outputGroup =
        parallelForeach.getParameterGroupModels().stream().filter(pg -> pg.getName().equals(OUTPUT)).findAny().get();

    assertThat(outputGroup.getParameter(TARGET_PARAMETER_NAME).isPresent(), is(true));
    assertThat(outputGroup.getParameter(TARGET_VALUE_PARAMETER_NAME).isPresent(), is(true));
  }

  @Test
  public void async() {
    final OperationModel asyncModel = coreExtensionModel.getOperationModel("async").get();
    assertThat(asyncModel.isBlocking(), is(false));

    assertThat(asyncModel.getNestedComponents(), hasSize(1));
    NestedChainModel processors = (NestedChainModel) asyncModel.getNestedComponents().get(0);
    assertThat(processors, instanceOf(NestedChainModel.class));
    assertThat(processors.isRequired(), is(true));
    assertThat(processors.getChainExecutionOccurrence(), is(ONCE));

    assertThat(asyncModel.getAllParameterModels(), hasSize(3));
    assertThat(asyncModel.getAllParameterModels().get(0).getName(), is("name"));
    assertThat(asyncModel.getAllParameterModels().get(0).getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(asyncModel.getAllParameterModels().get(0).getType(), instanceOf(DefaultStringType.class));
    assertThat(asyncModel.getAllParameterModels().get(0).isRequired(), is(false));

    assertThat(asyncModel.getAllParameterModels().get(1).getName(), is("maxConcurrency"));
    assertThat(asyncModel.getAllParameterModels().get(1).getType(), instanceOf(DefaultNumberType.class));
    assertThat(asyncModel.getAllParameterModels().get(1).isRequired(), is(false));
    assertErrorMappings(asyncModel.getAllParameterModels().get(2));
  }

  @Test
  public void tryScope() {
    final OperationModel tryModel = coreExtensionModel.getOperationModel("try").get();
    assertThat(tryModel.isBlocking(), is(false));

    List<ParameterModel> allParameterModels = tryModel.getAllParameterModels();
    assertThat(allParameterModels, hasSize(5));

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

    assertTarget(allParameterModels.get(2));
    assertTargetValue(allParameterModels.get(3));
    assertErrorMappings(allParameterModels.get(4));

    assertThat(tryModel.getNestedComponents(), hasSize(2));

    NestedChainModel chain = (NestedChainModel) tryModel.getNestedComponents().get(0);
    assertThat(chain.getName(), is("processors"));
    assertThat(chain.getChainExecutionOccurrence(), is(ONCE));

    assertThat(tryModel.getNestedComponents().get(1).getName(), is("errorHandler"));
  }

  @Test
  public void untilSuccessful() {
    final OperationModel untilSuccessful = coreExtensionModel.getOperationModel("untilSuccessful").get();
    assertThat(untilSuccessful.isBlocking(), is(false));

    List<ParameterModel> allParameterModels = untilSuccessful.getAllParameterModels();
    assertThat(allParameterModels, hasSize(5));

    ParameterModel action = allParameterModels.get(0);
    assertThat(action.getName(), is("maxRetries"));
    assertThat(action.getType(), is(instanceOf(DefaultNumberType.class)));
    assertThat(action.getExpressionSupport(), is(SUPPORTED));
    assertThat(action.getDefaultValue(), is(5));
    assertThat(action.isRequired(), is(false));

    ParameterModel type = allParameterModels.get(1);
    assertThat(type.getName(), is("millisBetweenRetries"));
    assertThat(type.getType(), is(instanceOf(DefaultNumberType.class)));
    assertThat(type.getExpressionSupport(), is(SUPPORTED));
    assertThat(type.getDefaultValue(), is(60000));
    assertThat(type.isRequired(), is(false));

    assertTarget(allParameterModels.get(2));
    assertTargetValue(allParameterModels.get(3));
    assertErrorMappings(allParameterModels.get(4));

    assertThat(untilSuccessful.getNestedComponents(), hasSize(1));
    NestedChainModel chain = (NestedChainModel) untilSuccessful.getNestedComponents().get(0);
    assertThat(chain.getChainExecutionOccurrence(), is(AT_LEAST_ONCE));
  }

  @Test
  public void firstSuccessful() {
    final OperationModel firstSuccessful = coreExtensionModel.getOperationModel("firstSuccessful").get();
    assertThat(firstSuccessful.isBlocking(), is(false));

    List<ParameterModel> allParameterModels = firstSuccessful.getAllParameterModels();
    assertThat(allParameterModels, hasSize(4));

    ParameterModel trackingEnableDefaultEvents = allParameterModels.get(0);
    assertThat(trackingEnableDefaultEvents.getName(), is("enableDefaultEvents"));
    assertTarget(allParameterModels.get(1));
    assertTargetValue(allParameterModels.get(2));
    assertErrorMappings(allParameterModels.get(3));

    assertThat(firstSuccessful.getNestedComponents(), hasSize(1));
    final NestedRouteModel routeModel = (NestedRouteModel) firstSuccessful.getNestedComponents().get(0);
    assertThat(routeModel.getName(), is("route"));
    assertThat(routeModel.getMinOccurs(), is(0));
    assertThat(routeModel.getMaxOccurs(), is(Optional.empty()));
    assertThat(routeModel.getAllParameterModels(), empty());
    assertThat(routeModel.getNestedComponents(), hasSize(1));

    final NestedChainModel chainModel = (NestedChainModel) routeModel.getNestedComponents().get(0);
    assertThat(chainModel.getChainExecutionOccurrence(), is(ONCE_OR_NONE));
  }

  @Test
  public void roundRobin() {
    final OperationModel roundRobin = coreExtensionModel.getOperationModel("roundRobin").get();
    assertThat(roundRobin.isBlocking(), is(false));

    List<ParameterModel> allParameterModels = roundRobin.getAllParameterModels();
    assertThat(allParameterModels, hasSize(4));

    ParameterModel trackingEnableDefaultEvents = allParameterModels.get(0);
    assertThat(trackingEnableDefaultEvents.getName(), is("enableDefaultEvents"));
    assertTarget(allParameterModels.get(1));
    assertTargetValue(allParameterModels.get(2));
    assertErrorMappings(allParameterModels.get(3));

    assertThat(roundRobin.getNestedComponents(), hasSize(1));
    final NestedRouteModel routeModel = (NestedRouteModel) roundRobin.getNestedComponents().get(0);
    assertThat(routeModel.getName(), is("route"));
    assertThat(routeModel.getMinOccurs(), is(1));
    assertThat(routeModel.getMaxOccurs(), is(Optional.empty()));
    assertThat(routeModel.getAllParameterModels(), empty());
    assertThat(routeModel.getNestedComponents(), hasSize(1));

    final NestedChainModel chainModel = (NestedChainModel) routeModel.getNestedComponents().get(0);
    assertThat(chainModel.getChainExecutionOccurrence(), is(ONCE_OR_NONE));
  }

  @Test
  public void globalErrorHandler() {
    final ConstructModel errorHandlerModel = coreExtensionModel.getConstructModel("errorHandler").get();

    assertThat(errorHandlerModel.allowsTopLevelDeclaration(), is(true));
    assertThat(errorHandlerModel.getStereotype().getType(), is(ERROR_HANDLER.getType()));

    assertThat(errorHandlerModel.getAllParameterModels(), hasSize(1));

    ParameterModel nameParam = errorHandlerModel.getAllParameterModels().get(0);
    assertThat(nameParam.getName(), is("name"));
    assertThat(nameParam.getDefaultValue(), is(nullValue()));
    assertThat(nameParam.isComponentId(), is(true));

    assertThat(errorHandlerModel.getNestedComponents(), hasSize(3));
    NestedRouteModel onErrorContinue = (NestedRouteModel) errorHandlerModel.getNestedComponents().get(0);
    verifyOnError(onErrorContinue);

    NestedRouteModel onErrorPropagate = (NestedRouteModel) errorHandlerModel.getNestedComponents().get(1);
    verifyOnError(onErrorPropagate);

    NestedComponentModel onErrorDelegate = (NestedComponentModel) errorHandlerModel.getNestedComponents().get(2);
    assertThat(onErrorDelegate.getName(), is("onError"));
    assertThat(onErrorDelegate.isRequired(), is(false));

    final ParameterModel onErrorRef =
        onErrorDelegate.getAllParameterModels().stream().filter(pm -> pm.getName().equals("ref")).findFirst().get();
    assertThat(onErrorRef.getName(), is("ref"));
    assertThat(onErrorRef.getType(), is(instanceOf(DefaultStringType.class)));
    assertThat(onErrorRef.isRequired(), is(true));
    assertThat(onErrorRef.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(onErrorRef.getAllowedStereotypes(), hasSize(1));
    assertThat(onErrorRef.getAllowedStereotypes().iterator().next().getType(), is(ON_ERROR.getType()));
  }

  @Test
  public void globalOnErrors() {
    ConstructModel onErrorContinue = coreExtensionModel.getConstructModel("onErrorContinue").get();
    assertThat(onErrorContinue.allowsTopLevelDeclaration(), is(true));
    assertThat(onErrorContinue.getStereotype().getType(), is(ON_ERROR.getType()));

    final List<ParameterModel> continueParams = onErrorContinue.getAllParameterModels();
    assertThat(continueParams, hasSize(5));

    assertGlobalOnErrorParams(continueParams);

    ConstructModel onErrorPropagate = coreExtensionModel.getConstructModel("onErrorPropagate").get();
    assertThat(onErrorPropagate.allowsTopLevelDeclaration(), is(true));
    assertThat(onErrorPropagate.getStereotype().getType(), is(ON_ERROR.getType()));

    final List<ParameterModel> propagateParams = onErrorPropagate.getAllParameterModels();
    assertThat(propagateParams, hasSize(5));

    assertGlobalOnErrorParams(propagateParams);
  }

  private void assertGlobalOnErrorParams(final List<ParameterModel> propagateParams) {
    ParameterModel nameParam;
    nameParam = propagateParams.get(0);
    assertThat(nameParam.getName(), is("name"));
    assertThat(nameParam.getDefaultValue(), is(nullValue()));
    assertThat(nameParam.isComponentId(), is(true));

    ParameterModel when = propagateParams.get(1);
    assertThat(when.getName(), is("when"));
    assertThat(when.getType(), is(instanceOf(DefaultStringType.class)));
    assertThat(when.getExpressionSupport(), is(SUPPORTED));
    assertThat(when.isRequired(), is(false));

    ParameterModel type = propagateParams.get(2);
    assertErrorType(type, "type");

    ParameterModel log = propagateParams.get(3);
    assertThat(log.getName(), is("logException"));
    assertThat(log.getType(), is(instanceOf(DefaultBooleanType.class)));
    assertThat(log.getExpressionSupport(), is(SUPPORTED));
    assertThat(log.isRequired(), is(false));

    ParameterModel notifications = propagateParams.get(4);
    assertThat(notifications.getName(), is("enableNotifications"));
    assertThat(notifications.getType(), is(instanceOf(DefaultBooleanType.class)));
    assertThat(notifications.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(notifications.isRequired(), is(false));
    assertThat(notifications.getDefaultValue(), is(true));
  }

  @Test
  public void configuration() {
    ConstructModel configuration = coreExtensionModel.getConstructModel("configuration").get();
    ParameterModel parameter = configuration.getAllParameterModels().stream()
        .filter(pm -> "inheritIterableRepeatability".equals(pm.getName())).findAny().get();
    SinceMuleVersionModelProperty sinceModelProperty = parameter.getModelProperty(SinceMuleVersionModelProperty.class).get();
    assertThat(sinceModelProperty.getVersion().toCompleteNumericVersion(), is("4.3.0"));
  }

  @Test
  public void setPayload() {
    OperationModel setPayload = coreExtensionModel.getOperationModel("setPayload").get();
    ParameterModel value = setPayload.getAllParameterModels().get(0);
    ParameterModel encoding = setPayload.getAllParameterModels().get(1);
    ParameterModel mimeType = setPayload.getAllParameterModels().get(2);

    assertThat(value.getName(), is("value"));
    assertThat(value.getExpressionSupport(), is(SUPPORTED));
    assertThat(value.getType(), is(instanceOf(StringType.class)));

    assertThat(encoding.getName(), is("encoding"));
    assertThat(encoding.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(encoding.getType(), is(instanceOf(StringType.class)));

    assertThat(mimeType.getName(), is("mimeType"));
    assertThat(mimeType.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(mimeType.getType(), is(instanceOf(StringType.class)));
  }

  @Test
  public void setVariable() {
    OperationModel setVariable = coreExtensionModel.getOperationModel("setVariable").get();

    ParameterModel variableName = setVariable.getAllParameterModels().get(0);
    ParameterModel value = setVariable.getAllParameterModels().get(1);
    ParameterModel encoding = setVariable.getAllParameterModels().get(2);
    ParameterModel mimeType = setVariable.getAllParameterModels().get(3);

    assertThat(variableName.getName(), is("variableName"));
    assertThat(variableName.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(variableName.getType(), is(instanceOf(StringType.class)));

    assertThat(value.getName(), is("value"));
    assertThat(value.getExpressionSupport(), is(SUPPORTED));
    assertThat(value.getType(), is(instanceOf(StringType.class)));

    assertThat(encoding.getName(), is("encoding"));
    assertThat(encoding.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(encoding.getType(), is(instanceOf(StringType.class)));

    assertThat(mimeType.getName(), is("mimeType"));
    assertThat(mimeType.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(mimeType.getType(), is(instanceOf(StringType.class)));
  }

  @Test
  public void parseTemplate() {
    OperationModel parseTemplateOperation = coreExtensionModel.getOperationModel("parseTemplate").get();
    ParameterModel targetValueParam = parseTemplateOperation.getParameterGroupModels().stream()
        .filter(op -> op.getName().equals(OUTPUT))
        .findFirst()
        .flatMap(pgm -> pgm.getParameter(TARGET_VALUE_PARAMETER_NAME))
        .get();

    assertThat(targetValueParam.getDefaultValue(), CoreMatchers.equalTo("#[payload]"));
  }

  @Test
  public void globalProperty() {
    ConstructModel setVariable = coreExtensionModel.getConstructModel("globalProperty").get();

    ParameterModel name = setVariable.getAllParameterModels().get(0);
    ParameterModel value = setVariable.getAllParameterModels().get(1);

    assertThat(name.getName(), is("name"));
    assertThat(name.isComponentId(), is(false));
    assertThat(name.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(name.getType(), is(instanceOf(StringType.class)));

    assertThat(value.getName(), is("value"));
    assertThat(value.isComponentId(), is(false));
    assertThat(value.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(value.getType(), is(instanceOf(StringType.class)));
  }

  @Test
  public void runtimeFunctions() {
    assertThat(coreExtensionModel.getFunctionModels(), hasSize(3));

    Optional<FunctionModel> p = coreExtensionModel.getFunctionModel("p");
    assertThat(p.isPresent(), is(true));
    assertThat(p.get().getOutput().getType(), is(STRING_TYPE));
    assertThat(p.get().getAllParameterModels(), hasSize(1));
    ParameterModel parameter = p.get().getAllParameterModels().get(0);
    assertThat(parameter.getName(), is("name"));
    assertThat(parameter.isRequired(), is(true));
    assertThat(parameter.getType(), is(STRING_TYPE));

    Optional<FunctionModel> lookup = coreExtensionModel.getFunctionModel("lookup");
    assertThat(lookup.isPresent(), is(true));
    assertThat(lookup.get().getOutput().getType(), is(ANY_TYPE));
    assertThat(lookup.get().getAllParameterModels(), hasSize(3));
    Optional<ParameterModel> param =
        lookup.get().getAllParameterModels().stream().filter(par -> par.getName().equals("flowName")).findFirst();
    assertThat(param.isPresent(), is(true));
    assertThat(param.get().isRequired(), is(true));
    assertThat(param.get().getType(), is(STRING_TYPE));
    param = lookup.get().getAllParameterModels().stream().filter(par -> par.getName().equals("payload")).findFirst();
    assertThat(param.isPresent(), is(true));
    assertThat(param.get().isRequired(), is(true));
    assertThat(param.get().getType(), is(ANY_TYPE));
    param = lookup.get().getAllParameterModels().stream().filter(par -> par.getName().equals("timeoutMillis")).findFirst();
    assertThat(param.isPresent(), is(true));
    assertThat(param.get().isRequired(), is(false));
    assertThat(param.get().getType(), is(INTEGER_TYPE));

    Optional<FunctionModel> causedBy = coreExtensionModel.getFunctionModel("causedBy");
    assertThat(causedBy.isPresent(), is(true));
    assertThat(causedBy.get().getOutput().getType(), is(BOOLEAN_TYPE));
    assertThat(causedBy.get().getAllParameterModels(), hasSize(2));
    param = causedBy.get().getAllParameterModels().stream().filter(par -> par.getName().equals("error")).findFirst();
    assertThat(param.isPresent(), is(true));
    assertThat(param.get().isRequired(), is(true));
    assertThat(param.get().getType(), is(ERROR));
    param = causedBy.get().getAllParameterModels().stream().filter(par -> par.getName().equals("type")).findFirst();
    assertThat(param.isPresent(), is(true));
    assertThat(param.get().isRequired(), is(true));
    assertThat(param.get().getType(), is(STRING_TYPE));
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
    assertThat(targetParameterModel.getName(), is(TARGET_PARAMETER_NAME));
    assertThat(targetParameterModel.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(targetParameterModel.getType(), is(instanceOf(StringType.class)));
    assertThat(targetParameterModel.isRequired(), is(false));
  }

  private void assertTargetValue(final ParameterModel targetValueParameterModel) {
    assertThat(targetValueParameterModel.getName(), is(TARGET_VALUE_PARAMETER_NAME));
    assertThat(targetValueParameterModel.getType(), is(instanceOf(DefaultStringType.class)));
    assertThat(targetValueParameterModel.getExpressionSupport(), is(REQUIRED));
    assertThat(targetValueParameterModel.isRequired(), is(false));
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
    assertThat(paramModel.getDslConfiguration().allowsReferences(), is(false));
    assertThat(paramModel.getType(), instanceOf(DefaultObjectType.class));
    assertThat(paramModel.isRequired(), is(true));
    assertThat(paramModel.getType().getAnnotation(TypeIdAnnotation.class).get().getValue(),
               is(SchedulingStrategy.class.getName()));
  }

  private void assertSchedulingDisallowConcurrentExecution(ParameterModel paramModel) {
    assertThat(paramModel.getName(), is("disallowConcurrentExecution"));
    assertThat(paramModel.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(paramModel.getType(), instanceOf(DefaultBooleanType.class));
    assertThat(paramModel.isRequired(), is(false));
    assertThat(paramModel.getDefaultValue(), is(false));
    assertThat(paramModel.getType(), is(instanceOf(BooleanType.class)));
  }

  private void assertErrorMappings(ParameterModel errorMappings) {
    assertThat(errorMappings.getName(), is(ERROR_MAPPINGS_PARAMETER_NAME));
    assertThat(errorMappings.getType(), is(instanceOf(DefaultArrayType.class)));
    assertThat(errorMappings.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(errorMappings.isRequired(), is(false));
  }

}
