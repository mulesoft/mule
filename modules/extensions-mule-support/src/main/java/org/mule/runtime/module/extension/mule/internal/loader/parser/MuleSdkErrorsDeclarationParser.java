/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.api.util.IdentifierParsingUtils.parseErrorType;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_DSL_ERRORS_CONSTRUCT_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_DSL_ERROR_CONSTRUCT_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_DSL_NAMESPACE;

import static java.lang.String.format;
import static java.util.Locale.ROOT;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.module.extension.internal.loader.parser.ErrorModelParser;

import java.util.HashMap;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.builder.GraphBuilder;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.slf4j.Logger;

/**
 * Parses the declared {@code errors} in an extension AST.
 */
public class MuleSdkErrorsDeclarationParser {

  private static final Logger LOGGER = getLogger(MuleSdkErrorsDeclarationParser.class);
  private static final String MULE = CORE_PREFIX.toUpperCase(ROOT);
  private static final ComponentIdentifier MULE_ANY = parseErrorType("ANY", MULE);

  private final ComponentAst extensionComponentAst;
  private final String extensionErrorNamespace;
  private Map<ComponentIdentifier, ErrorModelParser> parserByIdentifier;

  public MuleSdkErrorsDeclarationParser(ComponentAst extensionComponentAst, String extensionErrorNamespace) {
    this.extensionComponentAst = extensionComponentAst;
    this.extensionErrorNamespace = extensionErrorNamespace;
    this.parserByIdentifier = null;
  }

  public Map<ComponentIdentifier, ErrorModelParser> parse() {
    if (parserByIdentifier == null) {
      parserByIdentifier = actualParse(extensionComponentAst, extensionErrorNamespace);
    }
    return parserByIdentifier;
  }

  private static Map<ComponentIdentifier, ErrorModelParser> actualParse(ComponentAst extensionComponentAst,
                                                                        String extensionErrorNamespace) {
    // Just parse the mappings in the AST, without creating the ErrorModelParser yet.
    Map<ComponentIdentifier, ComponentIdentifier> errorIdToParentId =
        extractMappingFromErrorsToParent(extensionComponentAst, extensionErrorNamespace);

    // Walk the TRANSPOSED graph in a topological order (so the parent parser already created when we see each child).
    Graph<ComponentIdentifier, DefaultEdge> graph = buildTransposedGraph(errorIdToParentId);
    Map<ComponentIdentifier, ErrorModelParser> parserByIdentifier = new HashMap<>();
    new TopologicalOrderIterator<>(graph).forEachRemaining(errorIdentifier -> {
      ErrorModelParser parent = getParent(errorIdToParentId, parserByIdentifier, errorIdentifier);
      LOGGER.info("Creating parser for error '{}' with parent '{}'", errorIdentifier, parent);
      parserByIdentifier.put(errorIdentifier,
                             new MuleSdkErrorModelParser(errorIdentifier.getNamespace(), errorIdentifier.getName(), parent));
    });
    return parserByIdentifier;
  }

  private static ErrorModelParser getParent(Map<ComponentIdentifier, ComponentIdentifier> errorIdToParentId,
                                            Map<ComponentIdentifier, ErrorModelParser> parserByIdentifier,
                                            ComponentIdentifier errorIdentifier) {
    if (errorIdentifier.equals(MULE_ANY)) {
      return null;
    } else {
      return parserByIdentifier.get(errorIdToParentId.get(errorIdentifier));
    }
  }

  private static Graph<ComponentIdentifier, DefaultEdge> buildTransposedGraph(Map<ComponentIdentifier, ComponentIdentifier> edges) {
    GraphBuilder<ComponentIdentifier, DefaultEdge, ? extends DefaultDirectedGraph<ComponentIdentifier, DefaultEdge>> graphBuilder =
        DefaultDirectedGraph.createBuilder(DefaultEdge.class);
    edges.forEach((source, destination) -> graphBuilder.addEdge(destination, source));
    return graphBuilder.buildAsUnmodifiable();
  }

  private static Map<ComponentIdentifier, ComponentIdentifier> extractMappingFromErrorsToParent(ComponentAst extensionComponentAst,
                                                                                                String extensionErrorNamespace) {
    Map<ComponentIdentifier, ComponentIdentifier> errorToParent = new HashMap<>();
    extensionComponentAst
        .directChildrenStreamByIdentifier(MULE_SDK_EXTENSION_DSL_NAMESPACE, MULE_SDK_EXTENSION_DSL_ERRORS_CONSTRUCT_NAME)
        .forEach(errorsAst -> errorsAst
            .directChildrenStreamByIdentifier(MULE_SDK_EXTENSION_DSL_NAMESPACE, MULE_SDK_EXTENSION_DSL_ERROR_CONSTRUCT_NAME)
            .forEach(errorAst -> {
              ComponentIdentifier errorIdentifier = getType(extensionErrorNamespace, errorAst);
              ComponentIdentifier parentIdentifier = getParent(extensionErrorNamespace, errorAst);
              validateMapping(errorIdentifier, parentIdentifier, extensionErrorNamespace);
              LOGGER.info("Found declared error type '{}' with parent '{}' in the AST", errorIdentifier, parentIdentifier);
              errorToParent.put(errorIdentifier, parentIdentifier);
            }));
    return errorToParent;
  }

  private static void validateMapping(ComponentIdentifier errorIdentifier, ComponentIdentifier parentIdentifier,
                                      String extensionErrorNamespace) {
    String errorNamespace = errorIdentifier.getNamespace();
    String parentNamespace = parentIdentifier.getNamespace();
    if (!errorNamespace.equals(extensionErrorNamespace)) {
      throw new IllegalArgumentException(format("The extension with namespace '%s' can't declare the error '%s' with namespace '%s'",
                                                extensionErrorNamespace, errorIdentifier, errorNamespace));
    }
    if (!parentNamespace.equals(extensionErrorNamespace) && !parentNamespace.equals(MULE)) {
      throw new IllegalArgumentException(format("The error '%s' can't declare '%s' as parent. It can only have a parent with namespace '%s' or '%s'",
                                                errorIdentifier, parentIdentifier, extensionErrorNamespace, MULE));
    }
  }

  private static ComponentIdentifier getParent(String extensionErrorNamespace, ComponentAst errorAst) {
    return errorAst.getParameter(DEFAULT_GROUP_NAME, "parent").getValue()
        .<String>getValue().map(asString -> parseErrorType(asString, extensionErrorNamespace)).orElse(MULE_ANY);
  }

  private static ComponentIdentifier getType(String extensionErrorNamespace, ComponentAst errorAst) {
    return errorAst.getParameter(DEFAULT_GROUP_NAME, "type").getValue()
        .<String>getValue().map(asString -> parseErrorType(asString, extensionErrorNamespace)).get();
  }
}
