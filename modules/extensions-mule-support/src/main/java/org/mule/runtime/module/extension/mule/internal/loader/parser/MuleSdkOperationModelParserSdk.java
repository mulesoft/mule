/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.UNKNOWN;
import static org.mule.runtime.api.meta.model.ComponentVisibility.PUBLIC;
import static org.mule.runtime.api.meta.model.operation.ExecutionType.CPU_LITE;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.utils.Characteristic.AnyMatchCharacteristic;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.utils.Characteristic.AggregatedNotificationsCharacteristic;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.utils.MuleSdkOperationodelParserUtils.areAllCharacteristicsWithDefinitiveValue;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.utils.MuleSdkOperationodelParserUtils.setToDefaultIfNeeded;

import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.model.BinaryType;
import org.mule.runtime.api.meta.model.ComponentVisibility;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.notification.NotificationModel;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.extension.internal.property.NoStreamingConfigurationModelProperty;
import org.mule.runtime.extension.internal.property.NoTransactionalActionModelProperty;
import org.mule.runtime.module.extension.api.loader.java.property.CompletableComponentExecutorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ExceptionHandlerModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.DefaultOutputModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ErrorModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.NestedChainModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.NestedRouteModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.OperationModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.OutputModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.StereotypeModelFactory;
import org.mule.runtime.module.extension.mule.internal.execution.MuleOperationExecutor;
import org.mule.runtime.module.extension.mule.internal.loader.parser.utils.Characteristic;
import org.mule.runtime.module.extension.mule.internal.loader.parser.utils.MuleSdkOperationodelParserUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * {@link OperationModelParser} implementation for Mule SDK
 *
 * @since 4.5.0
 */
class MuleSdkOperationModelParserSdk extends BaseMuleSdkExtensionModelParser implements OperationModelParser {

  private static final String BODY_CHILD = "body";
  private static final String DESCRIPTION_PARAMETER = "description";
  private static final String DISPLAY_PARAMETER = "displayName";
  private static final String NAME_PARAMETER = "name";
  private static final String SUMMARY_PARAMETER = "summary";
  private static final String TYPE_PARAMETER = "type";
  private static final String VISIBILITY_PARAMETER = "visibility";
  private static final String DEPRECATED_CONSTRUCT_NAME = "deprecated";

  private final ComponentAst operation;
  private final TypeLoader typeLoader;

  private final Characteristic<Boolean> isBlocking = new AnyMatchCharacteristic(OperationModel::isBlocking);
  private final Characteristic<Boolean> isTransactional =
      new AnyMatchCharacteristic(OperationModel::isTransactional, MuleSdkOperationodelParserUtils::isSkippedScopeForTx);
  private final Characteristic<List<NotificationModel>> notificationModels = new AggregatedNotificationsCharacteristic();

  private final List<ModelProperty> additionalModelProperties = asList(new NoStreamingConfigurationModelProperty(), new NoTransactionalActionModelProperty());

  private String name;

  public MuleSdkOperationModelParserSdk(ComponentAst operation, TypeLoader typeLoader) {
    this.operation = operation;
    this.typeLoader = typeLoader;

    parseStructure();
  }

  private void parseStructure() {
    name = getParameter(operation, NAME_PARAMETER);
  }

  private OutputModelParser asOutputModelParser(ComponentAst outputTypeElement) {
    String type = getParameter(outputTypeElement, TYPE_PARAMETER);

    return typeLoader.load(type)
        .map(mt -> new DefaultOutputModelParser(mt, false))
        .orElseThrow(() -> new IllegalModelDefinitionException(format(
                                                                      "Component <%s:%s> defines %s as '%s' but such type is not defined in the application",
                                                                      outputTypeElement.getIdentifier().getNamespace(),
                                                                      outputTypeElement.getIdentifier().getName(),
                                                                      outputTypeElement.getIdentifier().getName())));
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return this.<String>getOptionalParameter(operation, DESCRIPTION_PARAMETER).orElse("");
  }

  @Override
  public List<ModelProperty> getAdditionalModelProperties() {
    return additionalModelProperties;
  }

  @Override
  public Stream<NotificationModel> getEmittedNotificationsStream(Function<String, Optional<NotificationModel>> notificationMapper) {
    // The mapper isn't needed at this implementation since we already have the operation models.
    return notificationModels.getValue().stream();
  }

  @Override
  public OutputModelParser getOutputType() {
    return asOutputModelParser(getOutputPayloadTypeElement());
  }

  @Override
  public OutputModelParser getAttributesOutputType() {
    return getOutputAttributesTypeElement()
        .map(this::asOutputModelParser)
        .orElse(VoidOutputModelParser.INSTANCE);
  }

  @Override
  public List<ParameterGroupModelParser> getParameterGroupModelParsers() {
    return getSingleChild(operation, "parameters")
        .map(parameters -> Collections
            .<ParameterGroupModelParser>singletonList(new MuleSdkParameterGroupModelParser(parameters, typeLoader)))
        .orElse(emptyList());
  }

  @Override
  public ComponentVisibility getComponentVisibility() {
    return this.<String>getOptionalParameter(operation, VISIBILITY_PARAMETER)
        .map(visibility -> ComponentVisibility.valueOf(visibility)).orElse(PUBLIC);
  }

  @Override
  public List<NestedRouteModelParser> getNestedRouteParsers() {
    return emptyList();
  }

  @Override
  public Optional<CompletableComponentExecutorModelProperty> getExecutorModelProperty() {
    return of(new CompletableComponentExecutorModelProperty((model, p) -> new MuleOperationExecutor(model)));
  }

  @Override
  public Optional<NestedChainModelParser> getNestedChainParser() {
    return empty();
  }

  @Override
  public boolean isBlocking() {
    return isBlocking.getValue();
  }

  @Override
  public boolean isIgnored() {
    return false;
  }

  @Override
  public boolean isScope() {
    return false;
  }

  @Override
  public boolean isRouter() {
    return false;
  }

  @Override
  public boolean isConnected() {
    // TODO: MULE-20077
    return false;
  }

  @Override
  public boolean hasConfig() {
    return false;
  }

  @Override
  public boolean supportsStreaming() {
    return this.getOutputType().getType() instanceof BinaryType;
  }

  @Override
  public boolean isTransactional() {
    return isTransactional.getValue();
  }

  @Override
  public boolean isAutoPaging() {
    // TODO: MULE-20081
    return false;
  }

  @Override
  public Optional<ExecutionType> getExecutionType() {
    // TODO: MULE-20082
    return of(CPU_LITE);
  }

  @Override
  public Optional<MediaTypeModelProperty> getMediaTypeModelProperty() {
    return empty();
  }

  @Override
  public Optional<ExceptionHandlerModelProperty> getExceptionHandlerModelProperty() {
    return empty();
  }

  @Override
  public Optional<DeprecationModel> getDeprecationModel() {
    return getSingleChild(operation, DEPRECATED_CONSTRUCT_NAME).map(this::buildDeprecationModel);
  }

  @Override
  public Optional<DisplayModel> getDisplayModel() {
    String summary = this.<String>getOptionalParameter(operation, SUMMARY_PARAMETER).orElse(null);
    String displayName = this.<String>getOptionalParameter(operation, DISPLAY_PARAMETER).orElse(null);

    if (!isBlank(displayName) || !isBlank(summary)) {
      return of(DisplayModel.builder()
          .summary(summary)
          .displayName(displayName)
          .build());
    }

    return empty();
  }

  @Override
  public List<ErrorModelParser> getErrorModelParsers() {
    return emptyList();
  }

  @Override
  public Set<String> getSemanticTerms() {
    return emptySet();
  }

  @Override
  public Optional<StereotypeModel> getStereotype(StereotypeModelFactory factory) {
    return empty();
  }

  private ComponentAst getOutputPayloadTypeElement() {
    final String elementName = "payload-type";
    return getOutputElement(elementName)
        .orElseThrow(() -> new IllegalOperationModelDefinitionException(format(
                                                                               "Operation '%s' is missing its <%s> declaration",
                                                                               getName(), elementName)));
  }

  private Optional<ComponentAst> getOutputAttributesTypeElement() {
    return getOutputElement("attributes-type");
  }

  private Optional<ComponentAst> getOutputElement(String elementName) {
    ComponentAst output = operation.directChildrenStreamByIdentifier(null, "output")
        .findFirst()
        .orElseThrow(() -> new IllegalOperationModelDefinitionException(format(
                                                                               "Operation '%s' is missing its <output> declaration",
                                                                               getName())));

    return output.directChildrenStreamByIdentifier(null, elementName).findFirst();
  }

  private ComponentAst getBody() {
    return getSingleChild(operation, BODY_CHILD).get();
  }

  private Stream<OperationModel> expandOperationWithoutModel(Map<String, MuleSdkOperationModelParserSdk> operationModelParsersByName,
                                                             Set<String> visitedOperations, ComponentAst componentAst,
                                                             Predicate<ComponentAst> filterCondition) {
    final MuleSdkOperationModelParserSdk operationParser =
        operationModelParsersByName.get(componentAst.getIdentifier().getName());

    if (operationParser != null) {
      return operationParser.getOperationModelsRecursiveStream(operationModelParsersByName, visitedOperations, filterCondition);
    } else {
      return Stream.empty();
    }
  }

  private Stream<OperationModel> getOperationModelsRecursiveStream(Map<String, MuleSdkOperationModelParserSdk> operationModelParsersByName) {
    return getOperationModelsRecursiveStream(operationModelParsersByName, componentAst -> false);
  }

  private Stream<OperationModel> getOperationModelsRecursiveStream(Map<String, MuleSdkOperationModelParserSdk> operationModelParsersByName,
                                                                   Predicate<ComponentAst> filterCondition) {
    final Set<String> visitedOperations = new HashSet<>();
    return getOperationModelsRecursiveStream(operationModelParsersByName, visitedOperations, filterCondition);
  }

  private Stream<OperationModel> getOperationModelsRecursiveStream(Map<String, MuleSdkOperationModelParserSdk> operationModelParsersByName,
                                                                   Set<String> visitedOperations,
                                                                   Predicate<ComponentAst> filterCondition) {
    if (!visitedOperations.add(this.getName())) {
      return Stream.empty();
    }

    Set<ComponentAst> filtered = new HashSet<>();

    return getBody().recursiveStream()
        .flatMap(componentAst -> {
          if (filtered.contains(componentAst)) {
            return Stream.empty();
          }
          Optional<OperationModel> operationModel = componentAst.getModel(OperationModel.class);
          if (operationModel.isPresent()) {
            return Stream.of(operationModel.get());
          } else if (filterCondition.test(componentAst)) {
            recursiveAddToFiltered(componentAst, filtered);
            return Stream.empty();
          } else if (componentAst.getComponentType().equals(UNKNOWN)) {
            return expandOperationWithoutModel(operationModelParsersByName, visitedOperations, componentAst, filterCondition);
          } else {
            return Stream.empty();
          }
        });
  }

  private void recursiveAddToFiltered(ComponentAst ast, Set<ComponentAst> filtered) {
    filtered.add(ast);
    for (ComponentAst child : ast.directChildren()) {
      recursiveAddToFiltered(child, filtered);
    }
  }

  public void computeCharacteristics(Map<String, MuleSdkOperationModelParserSdk> operationModelParsersByName) {
    computeCharacteristicsWithoutFiltering(asList(isBlocking, notificationModels), operationModelParsersByName);
    computeCharacteristicsWithFiltering(singletonList(isTransactional), operationModelParsersByName);
  }

  private void computeCharacteristicsWithoutFiltering(List<Characteristic<?>> characteristics,
                                                      Map<String, MuleSdkOperationModelParserSdk> operationModelParsersByName) {
    // For characteristics that don't involve filtering components we get the stream of operation models only once, as a
    // performance improvement
    getOperationModelsRecursiveStream(operationModelParsersByName).anyMatch(operationModel -> {
      for (Characteristic<?> characteristic : characteristics) {
        characteristic.computeFrom(operationModel);
      }
      return areAllCharacteristicsWithDefinitiveValue(characteristics);
    });
    setToDefaultIfNeeded(characteristics);
  }

  private void computeCharacteristicsWithFiltering(List<Characteristic<?>> characteristics,
                                                   Map<String, MuleSdkOperationModelParserSdk> operationModelParsersByName) {
    for (Characteristic<?> characteristic : characteristics) {
      getOperationModelsRecursiveStream(operationModelParsersByName, characteristic::filterComponent).anyMatch(operationModel -> {
        characteristic.computeFrom(operationModel);
        return characteristic.hasDefinitiveValue();
      });
    }
    setToDefaultIfNeeded(characteristics);
  }
}
