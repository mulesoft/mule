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

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * {@link OperationModelParser} implementation for Mule SDK
 *
 * @since 4.5.0
 */
class MuleSdkOperationModelParserSdk extends BaseMuleSdkExtensionModelParser implements OperationModelParser {

  private final ComponentAst operation;
  private final TypeLoader typeLoader;
  private Boolean isBlocking;

  private String name;

  public MuleSdkOperationModelParserSdk(ComponentAst operation, TypeLoader typeLoader) {
    this.operation = operation;
    this.typeLoader = typeLoader;

    parseStructure();
  }

  private void parseStructure() {
    name = getParameter(operation, "name");
  }

  private OutputModelParser asOutputModelParser(ComponentAst outputTypeElement) {
    String type = getParameter(outputTypeElement, "type");

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
    return this.<String>getOptionalParameter(operation, "description").orElse("");
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
    return this.<String>getOptionalParameter(operation, "visibility")
        .map(visibility -> ComponentVisibility.valueOf(visibility.toUpperCase())).orElse(PUBLIC);
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
    checkState(isBlocking != null, "Characteristics have not been computed yet.");
    return isBlocking;
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
    // TODO: MULE-20083
    return empty();
  }

  @Override
  public Optional<DisplayModel> getDisplayModel() {
    String summary = this.<String>getOptionalParameter(operation, "summary").orElse(null);
    String displayName = this.<String>getOptionalParameter(operation, "displayName").orElse(null);

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
    return getSingleChild(operation, "body").get();
  }

  private Stream<OperationModel> getOperationModelsRecursiveStream() {
    return getBody().recursiveStream()
        .map(innerComponent -> innerComponent.getModel(OperationModel.class))
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  private Stream<ComponentAst> getOperationsWithoutModelRecursiveStream() {
    return getBody().recursiveStream()
        .filter(innerComponent -> innerComponent.getComponentType().equals(UNKNOWN))
        .filter(innerComponent -> !innerComponent.getModel(OperationModel.class).isPresent());
  }

  private void computeIsBlocking(Map<String, MuleSdkOperationModelParserSdk> operationModelParsersByName) {
    // We use a stack to perform an iterative DFS traversal skipping already visited elements in case there were cycles.
    final Set<String> visitedOperations = new HashSet<>();
    final Deque<MuleSdkOperationModelParserSdk> parsersToCheck = new ArrayDeque<>();

    visitedOperations.add(this.getName());
    parsersToCheck.push(this);

    while (!parsersToCheck.isEmpty()) {
      final MuleSdkOperationModelParserSdk operationParser = parsersToCheck.pop();

      // If already computed, don't do it again
      if (operationParser.isBlocking != null) {
        if (operationParser.isBlocking()) {
          isBlocking = true;
          return;
        }
        continue;
      }

      // Inspects the inner operations that have an OperationModel first
      final boolean hasInnerBlockingOperationModels =
          operationParser.getOperationModelsRecursiveStream().anyMatch(OperationModel::isBlocking);
      if (hasInnerBlockingOperationModels) {
        // At this point we can also update the computed value for the parser currently being checked, this will save time if
        // other operations also reference it
        operationParser.isBlocking = isBlocking = true;
        return;
      }

      // Expands inner operations that don't have an OperationModel yet (i.e.: operations from the same extension currently being
      // parsed)
      operationParser.getOperationsWithoutModelRecursiveStream()
          .map(component -> operationModelParsersByName.get(component.getIdentifier().getName()))
          .filter(Objects::nonNull)
          .forEach(innerOperationParser -> {
            // Adds inner operations to the stack, only if they haven't been seen yet
            if (visitedOperations.add(innerOperationParser.getName())) {
              parsersToCheck.push(innerOperationParser);
            }
          });
    }

    isBlocking = false;
  }

  public void computeCharacteristics(Map<String, MuleSdkOperationModelParserSdk> operationModelParsersByName) {
    computeIsBlocking(operationModelParsersByName);
  }
}
