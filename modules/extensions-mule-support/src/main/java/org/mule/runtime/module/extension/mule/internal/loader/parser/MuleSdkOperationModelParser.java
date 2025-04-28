/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.UNKNOWN;
import static org.mule.runtime.api.meta.model.ComponentVisibility.PUBLIC;
import static org.mule.runtime.api.meta.model.operation.ExecutionType.CPU_LITE;
import static org.mule.runtime.ast.api.util.AstTraversalDirection.TOP_DOWN;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.metadata.api.TypeLoader;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.ComponentVisibility;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.notification.NotificationModel;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.model.ExtensionModelHelper;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.module.extension.api.loader.java.property.CompletableComponentExecutorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ComposedOperationModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ExceptionHandlerModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.AttributesResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.DefaultOutputModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ErrorModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.NestedChainModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.NestedRouteModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.OperationModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.OutputModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.StereotypeModelFactory;
import org.mule.runtime.module.extension.internal.loader.parser.java.utils.ResolvedMinMuleVersion;
import org.mule.runtime.module.extension.internal.loader.parser.metadata.InputResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.metadata.MetadataKeyModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.metadata.OutputResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.metadata.RoutesChainInputTypesResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.metadata.ScopeChainInputTypeResolverModelParser;
import org.mule.runtime.module.extension.mule.internal.execution.MuleOperationExecutor;
import org.mule.runtime.module.extension.mule.internal.loader.parser.utils.AggregatedErrorsCharacteristic;
import org.mule.runtime.module.extension.mule.internal.loader.parser.utils.Characteristic;
import org.mule.runtime.module.extension.mule.internal.loader.parser.utils.Characteristic.AggregatedNotificationsCharacteristic;
import org.mule.runtime.module.extension.mule.internal.loader.parser.utils.Characteristic.ComponentAstWithHierarchy;
import org.mule.runtime.module.extension.mule.internal.loader.parser.utils.Characteristic.FilteringCharacteristic;
import org.mule.runtime.module.extension.mule.internal.loader.parser.utils.Characteristic.IsBlockingCharacteristic;
import org.mule.runtime.module.extension.mule.internal.loader.parser.utils.Characteristic.IsConnectedCharacteristic;
import org.mule.runtime.module.extension.mule.internal.loader.parser.utils.Characteristic.IsTransactionalCharacteristic;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.slf4j.Logger;

/**
 * {@link OperationModelParser} implementation for Mule SDK
 *
 * @since 4.5.0
 */
class MuleSdkOperationModelParser extends BaseMuleSdkExtensionModelParser implements OperationModelParser {

  private static final Logger LOGGER = getLogger(MuleSdkOperationModelParser.class);

  private static final String BODY_CHILD = "body";
  private static final String DESCRIPTION_PARAMETER = "description";
  private static final String DISPLAY_PARAMETER = "displayName";
  private static final String NAME_PARAMETER = "name";
  private static final String SUMMARY_PARAMETER = "summary";
  private static final String TYPE_PARAMETER = "type";
  private static final String VISIBILITY_PARAMETER = "visibility";
  private static final String PAYLOAD_TYPE_ELEMENT_NAME = "payload-type";
  private static final String OUTPUT_ELEMENT_NAME = "output";
  private static final String MIN_MULE_VERSION = "4.5";

  private final ComponentAst operation;
  private final TypeLoader typeLoader;
  private final ExtensionModelHelper extensionModelHelper;

  private final Characteristic<Boolean> isBlocking = new IsBlockingCharacteristic();
  private final Characteristic<Boolean> isConnected = new IsConnectedCharacteristic();
  private final Characteristic<List<NotificationModel>> notificationModels = new AggregatedNotificationsCharacteristic();
  private final FilteringCharacteristic<Boolean> isTransactional = new IsTransactionalCharacteristic();
  private final Characteristic<List<ErrorModelParser>> errorModels;

  private String name;

  public MuleSdkOperationModelParser(ComponentAst operation, String namespace, TypeLoader typeLoader,
                                     ExtensionModelHelper extensionModelHelper) {
    this.operation = operation;
    this.typeLoader = typeLoader;
    this.extensionModelHelper = extensionModelHelper;
    this.errorModels = new AggregatedErrorsCharacteristic(namespace);

    parseStructure();
  }

  private void parseStructure() {
    name = getParameter(operation, NAME_PARAMETER);
  }

  private OutputModelParser asOutputModelParser(ComponentAst outputTypeElement) {
    String type = getParameter(outputTypeElement, TYPE_PARAMETER);

    return typeLoader.load(type)
        .map(mt -> new DefaultOutputModelParser(mt, false))
        .orElseThrow(() -> new IllegalModelDefinitionException(format("Component <%s:%s> defines %s as '%s' but such type is not defined in the application",
                                                                      outputTypeElement.getIdentifier().getNamespace(),
                                                                      outputTypeElement.getIdentifier().getName(),
                                                                      TYPE_PARAMETER, type)));
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
    return singletonList(new ComposedOperationModelProperty());
  }

  @Override
  public boolean hasStreamingConfiguration() {
    return false;
  }

  @Override
  public boolean hasTransactionalAction() {
    return false;
  }

  @Override
  public boolean hasReconnectionStrategy() {
    return false;
  }

  @Override
  public boolean propagatesConnectivityError() {
    return false;
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
            .<ParameterGroupModelParser>singletonList(new MuleSdkParameterGroupModelParser(parameters, typeLoader,
                                                                                           extensionModelHelper)))
        .orElse(emptyList());
  }

  @Override
  public ComponentVisibility getComponentVisibility() {
    return this.<String>getOptionalParameter(operation, VISIBILITY_PARAMETER)
        .map(ComponentVisibility::valueOf).orElse(PUBLIC);
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
    return isConnected.getValue();
  }

  @Override
  public boolean hasConfig() {
    return false;
  }

  @Override
  public boolean supportsStreaming() {
    // TODO(W-11293645): Discussion for more accurate implementation
    return false;
  }

  @Override
  public boolean isTransactional() {
    return isTransactional.getValue();
  }

  @Override
  public boolean isAutoPaging() {
    // Due to composition, reusable operations will never be considered to support auto paging.
    return false;
  }

  @Override
  public Optional<ExecutionType> getExecutionType() {
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
    return errorModels.getValue();
  }

  @Override
  public Optional<ResolvedMinMuleVersion> getResolvedMinMuleVersion() {
    return of(new ResolvedMinMuleVersion(name, new MuleVersion(MIN_MULE_VERSION),
                                         format("Operation %s has min mule version %s because the Mule Sdk was introduced in that version.",
                                                name, MIN_MULE_VERSION)));
  }

  @Override
  public Set<String> getSemanticTerms() {
    return emptySet();
  }

  @Override
  public Optional<StereotypeModel> getStereotype(StereotypeModelFactory factory) {
    return empty();
  }

  @Override
  public Optional<OutputResolverModelParser> getOutputResolverModelParser() {
    return empty();
  }

  @Override
  public Optional<AttributesResolverModelParser> getAttributesResolverModelParser() {
    return empty();
  }

  @Override
  public List<InputResolverModelParser> getInputResolverModelParsers() {
    return emptyList();
  }

  @Override
  public Optional<MetadataKeyModelParser> getMetadataKeyModelParser() {
    return empty();
  }

  @Override
  public Optional<ScopeChainInputTypeResolverModelParser> getScopeChainInputTypeResolverModelParser() {
    return empty();
  }

  @Override
  public Optional<RoutesChainInputTypesResolverModelParser> getRoutesChainInputTypesResolverModelParser() {
    return empty();
  }

  private ComponentAst getOutputPayloadTypeElement() {
    return getOutputElement(PAYLOAD_TYPE_ELEMENT_NAME)
        .orElseThrow(() -> new IllegalOperationModelDefinitionException(format(
                                                                               "Operation '%s' is missing its <%s> declaration",
                                                                               getName(), PAYLOAD_TYPE_ELEMENT_NAME)));
  }

  private Optional<ComponentAst> getOutputAttributesTypeElement() {
    return getOutputElement("attributes-type");
  }

  private Optional<ComponentAst> getOutputElement(String elementName) {
    ComponentAst output = operation.directChildrenStreamByIdentifier(null, OUTPUT_ELEMENT_NAME)
        .findFirst()
        .orElseThrow(() -> new IllegalOperationModelDefinitionException(format(
                                                                               "Operation '%s' is missing its <%s> declaration",
                                                                               getName(), OUTPUT_ELEMENT_NAME)));

    return output.directChildrenStreamByIdentifier(null, elementName).findFirst();
  }

  private ComponentAst getBody() {
    return getSingleChild(operation, BODY_CHILD).get();
  }

  private Stream<ComponentAstWithHierarchy> expandOperationWithoutModel(Map<String, MuleSdkOperationModelParser> operationModelParsersByName,
                                                                        Set<String> visitedOperations,
                                                                        ComponentAstWithHierarchy componentAst,
                                                                        Predicate<ComponentAstWithHierarchy> filterCondition,
                                                                        Predicate<ComponentAstWithHierarchy> ignoreCondition) {
    final MuleSdkOperationModelParser operationParser =
        operationModelParsersByName.get(componentAst.getComponentAst().getIdentifier().getName());

    if (operationParser != null) {
      return operationParser.getOperationsAstRecursiveStream(operationModelParsersByName, visitedOperations, filterCondition,
                                                             ignoreCondition);
    } else {
      return Stream.empty();
    }
  }

  private Stream<ComponentAstWithHierarchy> getOperationsAstRecursiveStream(Map<String, MuleSdkOperationModelParser> operationModelParsersByName) {
    return getOperationsAstRecursiveStream(operationModelParsersByName, componentAst -> false, componentAst -> false);
  }

  /**
   * @return returns a stream of {@link ComponentAstWithHierarchy} with all the operations ASTs within a Mule Operation's body.
   *
   * @param filterCondition when it returns true for a {@link ComponentAstWithHierarchy}, it will be filtered, as well as all its
   *                        inner children.
   * @param ignoreCondition when it returns true, that particular component is ignored.
   */
  private Stream<ComponentAstWithHierarchy> getOperationsAstRecursiveStream(Map<String, MuleSdkOperationModelParser> operationModelParsersByName,
                                                                            Predicate<ComponentAstWithHierarchy> filterCondition,
                                                                            Predicate<ComponentAstWithHierarchy> ignoreCondition) {
    final Set<String> visitedOperations = new HashSet<>();
    return getOperationsAstRecursiveStream(operationModelParsersByName, visitedOperations, filterCondition, ignoreCondition);
  }

  private Stream<ComponentAstWithHierarchy> getOperationsAstRecursiveStream(Map<String, MuleSdkOperationModelParser> operationModelParsersByName,
                                                                            Set<String> visitedOperations,
                                                                            Predicate<ComponentAstWithHierarchy> filterCondition,
                                                                            Predicate<ComponentAstWithHierarchy> ignoreCondition) {
    if (!visitedOperations.add(this.getName())) {
      return Stream.empty();
    }

    Set<ComponentAst> filtered = new HashSet<>();

    return TOP_DOWN.recursiveStreamWithHierarchy(Stream.of(getBody()))
        .map(ComponentAstWithHierarchy::new)
        .flatMap(componentAstWithHierarchy -> {
          ComponentAst componentAst = componentAstWithHierarchy.getComponentAst();
          List<ComponentAst> hierarchy = componentAstWithHierarchy.getHierarchy();
          LOGGER.trace("Iteration is processing the ast: [{}] -> [{}]", hierarchy, componentAst);

          if (filtered.contains(componentAst) || ignoreCondition.test(componentAstWithHierarchy)) {
            return Stream.empty();
          } else if (filterCondition.test(componentAstWithHierarchy)) {
            recursiveAddToFiltered(componentAst, filtered);
            return Stream.empty();
          }
          Optional<OperationModel> operationModel = componentAst.getModel(OperationModel.class);
          if (operationModel.isPresent()) {
            // It's an operation ast, with an operation model.
            return Stream.of(componentAstWithHierarchy);
          } else if (componentAst.getComponentType().equals(UNKNOWN)) {
            return expandOperationWithoutModel(operationModelParsersByName, visitedOperations, componentAstWithHierarchy,
                                               filterCondition, ignoreCondition);
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

  public void computeCharacteristics(Map<String, MuleSdkOperationModelParser> operationModelParsersByName) {
    computeCharacteristicsWithoutFiltering(asList(isBlocking, isConnected, notificationModels, errorModels),
                                           operationModelParsersByName);
    computeCharacteristicsWithFiltering(singletonList(isTransactional), operationModelParsersByName);
  }

  private void computeCharacteristicsWithoutFiltering(List<Characteristic<?>> characteristics,
                                                      Map<String, MuleSdkOperationModelParser> operationModelParsersByName) {
    // For characteristics that don't involve filtering components we get the stream of operation models only once, as a
    // performance improvement
    getOperationsAstRecursiveStream(operationModelParsersByName).anyMatch(operationAst -> {
      for (Characteristic<?> characteristic : characteristics) {
        characteristic.computeFrom(operationAst);
      }
      return areAllCharacteristicsWithDefinitiveValue(characteristics);
    });
    characteristics.stream().filter(c -> !c.hasValue()).forEach(Characteristic::setWithDefault);
  }

  private void computeCharacteristicsWithFiltering(List<FilteringCharacteristic<?>> characteristics,
                                                   Map<String, MuleSdkOperationModelParser> operationModelParsersByName) {
    for (FilteringCharacteristic<?> characteristic : characteristics) {
      getOperationsAstRecursiveStream(operationModelParsersByName, characteristic::filterComponent,
                                      characteristic::ignoreComponent)
          .anyMatch(operationAst -> {
            characteristic.computeFrom(operationAst);
            return characteristic.hasDefinitiveValue();
          });
    }
    characteristics.stream().filter(c -> !c.hasValue()).forEach(Characteristic::setWithDefault);
  }

  private boolean areAllCharacteristicsWithDefinitiveValue(List<Characteristic<?>> characteristics) {
    return characteristics.stream().allMatch(Characteristic::hasDefinitiveValue);
  }
}
