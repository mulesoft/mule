/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.error;

import static org.mule.runtime.api.meta.model.error.ErrorModelBuilder.newError;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.module.extension.internal.error.ErrorModelUtils.isMuleError;
import static org.mule.runtime.module.extension.internal.error.ModuleErrors.CONNECTIVITY;
import static org.mule.runtime.module.extension.internal.error.ModuleErrors.RETRY_EXHAUSTED;
import static org.mule.sdk.api.error.MuleErrors.ANY;
import static org.mule.sdk.api.error.MuleErrors.CRITICAL;

import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.error.ErrorModelBuilder;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.module.extension.internal.loader.parser.ErrorModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.error.JavaErrorModelParser;
import org.mule.sdk.api.error.ErrorTypeDefinition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

/**
 * Extension's {@link ErrorModel} factory. Given an {@link Enum} implementing {@link ErrorTypeDefinition} and the namespace of the
 * extension validates consistency and generates the correspondent {@link ErrorModel errorModels}.
 *
 * @since 4.0
 */
public class ErrorsModelFactory {

  private static ErrorTypeDefinition toErrorTypeDefinition(ErrorModelParser parser) {
    return (parser instanceof JavaErrorModelParser)
        ? ((JavaErrorModelParser) parser).getErrorTypeDefinition()
        : new ErrorTypeDefinitionParserAdapter(parser);
  }

  private static ErrorTypeDefinition<?>[] toDefinitionArray(List<ErrorModelParser> parsers) {
    return parsers.stream()
        .map(ErrorsModelFactory::toErrorTypeDefinition)
        .toArray(ErrorTypeDefinition[]::new);
  }

  private static ErrorTypeDefinition<?>[] adaptLegacyArray(org.mule.runtime.extension.api.error.ErrorTypeDefinition<?>[] errorTypesEnum) {
    return Stream.of(errorTypesEnum)
        .map(SdkErrorTypeDefinitionAdapter::from)
        .toArray(ErrorTypeDefinition[]::new);
  }

  private static final String MULE = CORE_PREFIX.toUpperCase();
  private final String extensionNamespace;
  private final Map<String, ErrorModel> errorModelMap;

  /**
   * Creates a new instance of the factory
   *
   * @param extensionErrorParsers a list of {@link ErrorModelParser} describing all the errors from an extension
   * @param extensionNamespace    the namespace for the {@link ErrorModel} to be generated
   */
  public ErrorsModelFactory(List<ErrorModelParser> extensionErrorParsers, String extensionNamespace)
      throws IllegalModelDefinitionException {
    this(toDefinitionArray(extensionErrorParsers), extensionNamespace);
  }

  /**
   * Creates a new instance of the factory
   *
   * @param errorTypesEnum     a legacy {@link org.mule.runtime.extension.api.error.ErrorTypeDefinition} describing all the errors
   *                           from an extension
   * @param extensionNamespace the namespace for the {@link ErrorModel} to be generated
   */
  public ErrorsModelFactory(org.mule.runtime.extension.api.error.ErrorTypeDefinition<?>[] errorTypesEnum,
                            String extensionNamespace) {
    this(adaptLegacyArray(errorTypesEnum), extensionNamespace);
  }

  /**
   * Creates a new instance of the factory
   *
   * @param errorTypesEnum     an {@link ErrorTypeDefinition} implementation indicating all the errors from an extension
   * @param extensionNamespace the namespace for the {@link ErrorModel} to be generated
   */
  public ErrorsModelFactory(ErrorTypeDefinition<?>[] errorTypesEnum, String extensionNamespace) {
    this.extensionNamespace = extensionNamespace.toUpperCase();
    final Graph<ErrorTypeDefinition, DefaultEdge> graph = toGraph(errorTypesEnum);

    errorModelMap = new HashMap<>();
    initErrorModelMap(errorModelMap);

    new TopologicalOrderIterator<>(graph).forEachRemaining(errorType -> {
      ErrorModel errorModel = toErrorModel(errorType, errorModelMap);
      errorModelMap.put(errorModel.toString(), errorModel);
    });
    addConnectivityErrors(errorModelMap);
  }

  /**
   * Creates a new instance of the factory populated with Mule's errors as well as the extension default errors
   *
   * @param extensionNamespace the namespace for the {@link ErrorModel} to be generated
   */
  public ErrorsModelFactory(String extensionNamespace)
      throws IllegalModelDefinitionException {
    this.extensionNamespace = extensionNamespace.toUpperCase();
    errorModelMap = new HashMap<>();
    initErrorModelMap(errorModelMap);
    addConnectivityErrors(errorModelMap);
  }

  /**
   * @return A {@link Set} of converted {@link ErrorModel}s generated from the given {@link ErrorTypeDefinition} array
   */
  public Set<ErrorModel> getErrorModels() {
    return new HashSet<>(errorModelMap.values());
  }

  /**
   * Transforms an {@link ErrorModelParser} into an {@link ErrorModel}
   *
   * @param errorModelParser the input representation
   * @return The correspondent {@link ErrorModel} for a given {@link ErrorModelParser}
   */
  public ErrorModel getErrorModel(ErrorModelParser errorModelParser) {
    String errorKey = toIdentifier(toErrorTypeDefinition(errorModelParser));
    if (errorModelMap.containsKey(errorKey)) {
      return errorModelMap.get(errorKey);
    }

    return toErrorModel(toErrorTypeDefinition(errorModelParser), errorModelMap);
  }

  private DefaultDirectedGraph<ErrorTypeDefinition, DefaultEdge> toGraph(ErrorTypeDefinition<?>[] errorTypesEnum) {
    final DefaultDirectedGraph<ErrorTypeDefinition, DefaultEdge> graph =
        new DefaultDirectedWeightedGraph<>(DefaultEdge.class);
    Stream.of(errorTypesEnum).forEach(error -> addType(error, graph));
    detectCycleReferences(graph);
    return graph;
  }

  /**
   * @param errorTypeDefinition
   * @param errorModelMap
   * @return
   */
  private ErrorModel toErrorModel(ErrorTypeDefinition<?> errorTypeDefinition, Map<String, ErrorModel> errorModelMap) {
    if (errorModelMap.containsKey(toIdentifier(errorTypeDefinition))) {
      return errorModelMap.get(toIdentifier(errorTypeDefinition));
    } else {
      ErrorModelBuilder builder = newError(errorTypeDefinition.getType(), getErrorNamespace(errorTypeDefinition));
      builder.withParent(toErrorModel(errorTypeDefinition.getParent().orElse(ANY), errorModelMap));
      ErrorModel errorModel = builder.build();
      errorModelMap.put(toIdentifier(errorTypeDefinition), errorModel);
      return errorModel;
    }
  }

  private String toIdentifier(ErrorTypeDefinition errorTypeDefinition) {
    return getErrorNamespace(errorTypeDefinition) + ":" + errorTypeDefinition.getType();
  }

  private String getErrorNamespace(ErrorTypeDefinition errorType) {
    return isMuleError(errorType) ? MULE : extensionNamespace;
  }

  private void addType(ErrorTypeDefinition<?> errorType, Graph<ErrorTypeDefinition, DefaultEdge> graph) {
    graph.addVertex(errorType);
    String type = errorType.getType();
    if (!ANY.name().equals(type) && !CRITICAL.name().equals(type)) {
      ErrorTypeDefinition parentErrorType = errorType.getParent().orElse((ANY));
      graph.addVertex(parentErrorType);
      graph.addEdge(errorType, parentErrorType);
    }
  }

  private void detectCycleReferences(DefaultDirectedGraph<?, ?> graph) {
    CycleDetector<?, ?> cycleDetector = new CycleDetector<>(graph);
    if (cycleDetector.detectCycles()) {
      throw new IllegalModelDefinitionException("Cyclic Error Types reference detected, offending types: "
          + cycleDetector.findCycles());
    }
  }

  private void addConnectivityErrors(Map<String, ErrorModel> errorModelMap) {
    ErrorModel connectivityError = toErrorModel(CONNECTIVITY, errorModelMap);
    String key = connectivityError.toString();
    if (!errorModelMap.containsKey(key)) {
      errorModelMap.put(key, connectivityError);
    }

    ErrorModel retryExhaustedError = toErrorModel(RETRY_EXHAUSTED, errorModelMap);
    String retry = retryExhaustedError.toString();
    if (!errorModelMap.containsKey(retry)) {
      errorModelMap.put(retry, retryExhaustedError);
    }
  }

  private void initErrorModelMap(Map<String, ErrorModel> errorModelMap) {
    errorModelMap.put(toIdentifier(ANY), newError(ANY.getType(), MULE).build());
  }
}
