/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.ast;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.FLOW;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SUB_FLOW;
import static org.mule.runtime.ast.api.util.MuleArtifactAstCopyUtils.copyRecursively;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.CORE_PREFIX;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import static org.apache.commons.lang3.StringUtils.repeat;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.util.BaseComponentAstDecorator;
import org.mule.runtime.extension.api.property.XmlExtensionModelProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.traverse.GraphIterator;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link MacroExpansionModulesModel} goes over all the parametrized {@link ExtensionModel} by filtering them if they have the
 * {@link XmlExtensionModelProperty} (implies that has to be macro expanded).
 * <p/>
 * For every occurrence that happens, it will expand the operations/configurations by working with the
 * {@link MacroExpansionModuleModel} passing through just one {@link ExtensionModel} to macro expand in the current Mule
 * Application (held by the {@link ArtifactAst}.
 *
 * @since 4.0
 */
public class MacroExpansionModulesModel {

  private static final Logger LOGGER = LoggerFactory.getLogger(MacroExpansionModulesModel.class);
  private static final String FILE_MACRO_EXPANSION_DELIMITER = repeat('*', 80) + lineSeparator();
  private static final String FILE_MACRO_EXPANSION_SECTION_DELIMITER = repeat('-', 80) + lineSeparator();
  private static final String FLOW_ELEMENT = "flow";
  private static final String SUB_FLOW_ELEMENT = "sub-flow";
  private static final ComponentIdentifier FLOW_IDENTIFIER = builder().namespace(CORE_PREFIX).name(FLOW_ELEMENT).build();
  private static final ComponentIdentifier SUB_FLOW_IDENTIFIER = builder().namespace(CORE_PREFIX).name(SUB_FLOW_ELEMENT).build();

  private ArtifactAst applicationModel;
  private final List<ExtensionModel> sortedExtensions;
  private final Optional<FeatureFlaggingService> featureFlaggingService;

  /**
   * From a mutable {@code applicationModel}, it will store it to apply changes when the {@link #expand()} method is executed.
   *
   * @param applicationModel to modify given the usages of elements that belong to the {@link ExtensionModel}s contained in the
   *                         {@code extensions} map.
   * @param extensions       set with all the loaded {@link ExtensionModel}s from the deployment that will be filtered by looking
   *                         up only those that are coming from an XML context through the {@link XmlExtensionModelProperty}
   *                         property.
   */
  public MacroExpansionModulesModel(ArtifactAst applicationModel, Set<ExtensionModel> extensions,
                                    Optional<FeatureFlaggingService> featureFlaggingService) {
    this.applicationModel = applicationModel;
    this.sortedExtensions = calculateExtensionByTopologicalOrder(extensions);
    this.featureFlaggingService = featureFlaggingService;
  }

  /**
   * Goes through the entire xml mule application looking for the message processors that can be expanded, and then takes care of
   * the global elements if there are at least one {@link ExtensionModel} to macro expand.
   */
  public ArtifactAst expand() {
    boolean hasMacroExpansionExtension = false;

    for (ExtensionModel extensionModel : sortedExtensions) {
      if (extensionModel.getModelProperty(XmlExtensionModelProperty.class).isPresent()) {
        hasMacroExpansionExtension = true;

        LOGGER.debug("macro expanding '{}' connector, xmlns:{}=\"{}\"",
                     extensionModel.getName(),
                     extensionModel.getXmlDslModel().getPrefix(),
                     extensionModel.getXmlDslModel().getNamespace());
        applicationModel = new MacroExpansionModuleModel(applicationModel, extensionModel, featureFlaggingService).expand();
      }
    }

    if (hasMacroExpansionExtension) {
      // The macro expansion of xml sdk components causes inner flow components to be generated as a child of another component;
      // so when generating the minimal app during lazy init, those flow components are not registered as top level components
      // causing certain validations to fail. This code extracts those flow and sub-flow components and adds them as top level.
      List<ComponentAst> componentsToAdd = applicationModel.recursiveStream().filter(c -> isFlow(c) || isSubFlow(c))
          .filter(comp -> !applicationModel.topLevelComponents().contains(comp)).collect(toList());
      applicationModel = copyRecursively(applicationModel, comp -> {
        List<ComponentAst> childrenToKeep = comp.directChildrenStream()
            .filter(child -> componentsToAdd.stream().noneMatch(c -> c.getIdentifier().equals(child.getIdentifier())))
            .collect(toList());
        return new BaseComponentAstDecorator(comp) {

          @Override
          public List<ComponentAst> directChildren() {
            return childrenToKeep;
          }

          @Override
          public Stream<ComponentAst> directChildrenStream() {
            return childrenToKeep.stream();
          }
        };
      }, () -> componentsToAdd, c -> false);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.atDebug()
            .log(() -> {
              // only log the macro expanded app if there are smart connectors in it
              final StringBuilder buf = new StringBuilder(1024);
              buf.append(lineSeparator()).append(FILE_MACRO_EXPANSION_DELIMITER);

              AtomicReference<String> lastFile = new AtomicReference<>();

              applicationModel.topLevelComponentsStream().forEach(comp -> {
                final String fileName = comp.getMetadata().getFileName().orElse("<unnamed>");

                if (!fileName.equals(lastFile.get())) {
                  if (lastFile.get() != null) {
                    buf.append(lineSeparator()).append(FILE_MACRO_EXPANSION_SECTION_DELIMITER);
                  }
                  buf.append("Filename: ").append(fileName);
                  buf.append(lineSeparator()).append(FILE_MACRO_EXPANSION_SECTION_DELIMITER);

                  lastFile.set(fileName);
                }

                buf
                    .append(comp.getMetadata().getSourceCode().orElse(""))
                    .append(lineSeparator());
              });

              buf.append(lineSeparator()).append(FILE_MACRO_EXPANSION_DELIMITER);

              return buf.toString();
            });
      }
    }

    return applicationModel;
  }

  /**
   * Constructs a Direct Acyclic Graph (DAG) with the dependencies at namespace level of those {@link ExtensionModel} that must be
   * macro expanded with a topological order.
   * <p/>
   * It starts by taking the namespaces of macro expandable <module/>s from the Mule Application, to then assembly a DAG using
   * those namespaces as starting point. For each <module/> namespace, it will go over it's dependencies using
   * {@link #fillDependencyGraph(DirectedGraph, String, Map)}.
   * <p/>
   * Once finished, generates a {@link TopologicalOrderIterator} as the macro expansion relies entirely in the correct order to
   * plain it in a simple {@link List} to be later used in the {@link #expand()} method.
   *
   * @param extensions complete set of {@link ExtensionModel}s used in the app that might or might not be macro expandable (it
   *                   will filter them.
   * @return a <bold>sorted</bold> collection of {@link ExtensionModel} to macro expand. This order must not be altered.
   */
  private List<ExtensionModel> calculateExtensionByTopologicalOrder(Set<ExtensionModel> extensions) {
    final List<ExtensionModel> result = new ArrayList<>();
    final Map<String, ExtensionModel> allExtensionsByNamespace = extensions.stream()
        .filter(extensionModel -> extensionModel.getModelProperty(XmlExtensionModelProperty.class).isPresent())
        .collect(toMap(extModel -> extModel.getXmlDslModel().getNamespace(), Function.identity()));

    // we first check there are at least one extension to macro expand
    if (!allExtensionsByNamespace.isEmpty()) {
      Set<String> extensionsUsedInApp = applicationModel.recursiveStream()
          .map(comp -> comp.getIdentifier().getNamespaceUri())
          .filter(ns -> allExtensionsByNamespace.keySet().contains(ns))
          .collect(toSet());

      // then we assure there are at least one of those extensions being used by the app
      if (!extensionsUsedInApp.isEmpty()) {
        // generation of the DAG and then the topological iterator.
        // it's important to be 100% sure the DAG is not empty, or the TopologicalOrderIterator will fail at start up.
        Graph<String, DefaultEdge> namespaceDAG = new DirectedMultigraph<>(DefaultEdge.class);
        extensionsUsedInApp.forEach(namespace -> fillDependencyGraph(namespaceDAG, namespace, allExtensionsByNamespace));
        GraphIterator<String, DefaultEdge> graphIterator = new TopologicalOrderIterator<>(namespaceDAG);
        while (graphIterator.hasNext()) {
          final String namespace = graphIterator.next();
          if (allExtensionsByNamespace.containsKey(namespace)) {
            result.add(allExtensionsByNamespace.get(namespace));

          }
        }
      }
    }
    return result;
  }

  private void fillDependencyGraph(Graph<String, DefaultEdge> g, String sourceVertex,
                                   Map<String, ExtensionModel> allExtensionsByNamespace) {
    final ExtensionModel extensionModel = allExtensionsByNamespace.get(sourceVertex);
    g.addVertex(sourceVertex);
    for (String dependencyNamespace : getDependenciesOrFail(extensionModel)) {
      if (allExtensionsByNamespace.containsKey(dependencyNamespace)) {
        g.addVertex(dependencyNamespace);
        g.addEdge(sourceVertex, dependencyNamespace);
        fillDependencyGraph(g, dependencyNamespace, allExtensionsByNamespace);
      }
    }
  }

  private Set<String> getDependenciesOrFail(ExtensionModel extensionModel) {
    return extensionModel.getModelProperty(XmlExtensionModelProperty.class)
        .orElseThrow(() -> new IllegalArgumentException(format("The current extension [%s] (namespace [%s]) does not have the macro expansion model property, it should have never reach here.",
                                                               extensionModel.getName(),
                                                               extensionModel.getXmlDslModel().getNamespace())))
        .getNamespacesDependencies();
  }

  private boolean isFlow(ComponentAst component) {
    return FLOW_IDENTIFIER.equals(component.getIdentifier()) && FLOW.equals(component.getComponentType());
  }

  private boolean isSubFlow(ComponentAst component) {
    return SUB_FLOW_IDENTIFIER.equals(component.getIdentifier()) && SUB_FLOW.equals(component.getComponentType());
  }
}
