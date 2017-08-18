/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static org.mule.runtime.api.meta.model.error.ErrorModelBuilder.newError;
import static org.mule.runtime.extension.api.error.MuleErrors.ANY;
import static org.mule.runtime.extension.api.error.MuleErrors.CRITICAL;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.module.extension.internal.loader.enricher.ModuleErrors.CONNECTIVITY;
import static org.mule.runtime.module.extension.internal.loader.enricher.ModuleErrors.RETRY_EXHAUSTED;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.error.ErrorModelBuilder;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.error.MuleErrors;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

/**
 * Extension's {@link ErrorModel} factory.
 * Given an {@link Enum} implementing {@link ErrorTypeDefinition} and the namespace of the extension validates
 * consistency and generates the correspondent {@link ErrorModel errorModels}.
 *
 * @since 4.0
 */
public class ErrorsModelFactory {

  private static final String MULE = CORE_PREFIX.toUpperCase();
  private final String extensionNamespace;
  private final Map<String, ErrorModel> errorModelMap;

  /**
   * Creates a new instance of the factory
   *
   * @param errorTypesEnum     an {@link ErrorTypeDefinition} implementation indicating all the errors from an extension
   * @param extensionNamespace the namespace for the {@link ErrorModel} to be generated
   */
  public ErrorsModelFactory(ErrorTypeDefinition<?>[] errorTypesEnum, String extensionNamespace)
      throws IllegalModelDefinitionException {
    this.extensionNamespace = extensionNamespace.toUpperCase();
    final DirectedGraph<ErrorTypeDefinition, Pair<ErrorTypeDefinition, ErrorTypeDefinition>> graph = toGraph(errorTypesEnum);

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
   * From a {@link ErrorTypeDefinition} gives the {@link ErrorModel} representation
   *
   * @param errorTypeDefinition to use to find the {@link ErrorModel} representation
   * @return The correspondent {@link ErrorModel} for a given {@link ErrorTypeDefinition}
   */
  ErrorModel getErrorModel(ErrorTypeDefinition errorTypeDefinition) {
    return errorModelMap.get(toIdentifier(errorTypeDefinition));
  }

  private DefaultDirectedGraph<ErrorTypeDefinition, Pair<ErrorTypeDefinition, ErrorTypeDefinition>> toGraph(ErrorTypeDefinition<?>[] errorTypesEnum) {
    final DefaultDirectedGraph<ErrorTypeDefinition, Pair<ErrorTypeDefinition, ErrorTypeDefinition>> graph =
        new DefaultDirectedWeightedGraph<>(ImmutablePair::new);
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
    return errorType instanceof MuleErrors ? MULE : extensionNamespace;
  }

  private void addType(ErrorTypeDefinition<?> errorType,
                       Graph<ErrorTypeDefinition, Pair<ErrorTypeDefinition, ErrorTypeDefinition>> graph) {
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
