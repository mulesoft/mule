/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.UNKNOWN;
import static org.mule.runtime.api.meta.model.ComponentVisibility.PUBLIC;
import static org.mule.runtime.api.meta.model.operation.ExecutionType.CPU_LITE;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;

import org.mule.metadata.api.TypeLoader;
import org.mule.runtime.api.meta.model.ComponentVisibility;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.extension.api.model.deprecated.ImmutableDeprecationModel;
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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
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
  private static final String CHARACTERISTICS_NOT_COMPUTED_MSG = "Characteristics have not been computed yet.";

  private final ComponentAst operation;
  private final TypeLoader typeLoader;

  private final Characteristic<Boolean> isBlocking = new AnyMatchCharacteristic(OperationModel::isBlocking);

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
    return emptyList();
  }

  @Override
  public List<String> getEmittedNotifications() {
    // TODO: MULE-20075
    return emptyList();
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
    // TODO: MULE-20079
    return false;
  }

  @Override
  public boolean isTransactional() {
    // TODO: MULE-20080
    return false;
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
                                                             Set<String> visitedOperations,
                                                             ComponentAst componentAst) {
    final MuleSdkOperationModelParserSdk operationParser =
        operationModelParsersByName.get(componentAst.getIdentifier().getName());

    if (operationParser != null) {
      return operationParser.getOperationModelsRecursiveStream(operationModelParsersByName, visitedOperations);
    } else {
      // Null here represents an empty stream, but it is more efficient because we avoid constructing one.
      return null;
    }
  }

  private Stream<OperationModel> getOperationModelsRecursiveStream(Map<String, MuleSdkOperationModelParserSdk> operationModelParsersByName) {
    final Set<String> visitedOperations = new HashSet<>();
    return getOperationModelsRecursiveStream(operationModelParsersByName, visitedOperations);
  }

  private Stream<OperationModel> getOperationModelsRecursiveStream(Map<String, MuleSdkOperationModelParserSdk> operationModelParsersByName,
                                                                   Set<String> visitedOperations) {
    if (!visitedOperations.add(this.getName())) {
      return Stream.empty();
    }

    return getBody().recursiveStream()
        .flatMap(componentAst -> {
          Optional<OperationModel> operationModel = componentAst.getModel(OperationModel.class);
          if (operationModel.isPresent()) {
            return Stream.of(operationModel.get());
          } else if (componentAst.getComponentType().equals(UNKNOWN)) {
            return expandOperationWithoutModel(operationModelParsersByName, visitedOperations, componentAst);
          } else {
            // Null here represents an empty stream, but it is more efficient because we avoid constructing one.
            return null;
          }
        });
  }

  public void computeCharacteristics(Map<String, MuleSdkOperationModelParserSdk> operationModelParsersByName) {
    computeCharacteristics(singletonList(isBlocking), operationModelParsersByName);
  }

  private void computeCharacteristics(List<Characteristic<?>> characteristics,
                                      Map<String, MuleSdkOperationModelParserSdk> operationModelParsersByName) {
    getOperationModelsRecursiveStream(operationModelParsersByName).anyMatch(operationModel -> {
      for (Characteristic<?> characteristic : characteristics) {
        characteristic.computeFrom(operationModel);
      }

      return areAllCharacteristicsWithDefinitiveValue(characteristics);
    });

    for (Characteristic<?> characteristic : characteristics) {
      if (!characteristic.hasValue()) {
        characteristic.setWithDefault();
      }
    }
  }

  private boolean areAllCharacteristicsWithDefinitiveValue(List<Characteristic<?>> characteristics) {
    return characteristics.stream().allMatch(Characteristic::hasDefinitiveValue);
  }

  private static class Characteristic<T> {

    private final BiFunction<OperationModel, T, T> mapper;
    private final T defaultValue;
    private final T stopValue;

    private T value;

    private Characteristic(BiFunction<OperationModel, T, T> mapper, T defaultValue, T stopValue) {
      this.mapper = mapper;
      this.defaultValue = defaultValue;
      this.stopValue = stopValue;
    }

    public void computeFrom(OperationModel operationModel) {
      value = mapper.apply(operationModel, value);
    }

    public void setWithDefault() {
      value = defaultValue;
    }

    public boolean hasDefinitiveValue() {
      return stopValue.equals(value);
    }

    public boolean hasValue() {
      return value != null;
    }

    public T getValue() {
      checkState(hasValue(), CHARACTERISTICS_NOT_COMPUTED_MSG);
      return value;
    }
  }

  private static class BooleanCharacteristic extends Characteristic<Boolean> {

    private BooleanCharacteristic(Predicate<OperationModel> predicate, Boolean defaultValue, Boolean stopValue) {
      super(((operationModel,
              curValue) -> (curValue != null && curValue == stopValue) ? curValue : predicate.test(operationModel)),
            defaultValue, stopValue);
    }
  }

  private static class AnyMatchCharacteristic extends BooleanCharacteristic {

    private AnyMatchCharacteristic(Predicate<OperationModel> predicate) {
      super(predicate, false, true);
    }
  }
}
