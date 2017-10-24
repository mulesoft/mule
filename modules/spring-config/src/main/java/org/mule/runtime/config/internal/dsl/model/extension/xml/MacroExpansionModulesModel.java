/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.extension.xml;

import static java.lang.String.format;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.traverse.GraphIterator;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.extension.api.property.XmlExtensionModelProperty;

import javax.xml.XMLConstants;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A {@link MacroExpansionModulesModel} goes over all the parametrized {@link ExtensionModel} by filtering them if they have
 * the {@link XmlExtensionModelProperty} (implies that has to be macro expanded).
 * <p/>
 * For every occurrence that happens, it will expand the operations/configurations by working with the
 * {@link MacroExpansionModuleModel} passing through just one {@link ExtensionModel} to macro expand in the current Mule
 * Application (held by the {@link ApplicationModel}.
 *
 * @since 4.0
 */
public class MacroExpansionModulesModel {

  private final ApplicationModel applicationModel;
  private final List<ExtensionModel> sortedExtensions;

  /**
   * From a mutable {@code applicationModel}, it will store it to apply changes when the {@link #expand()} method is executed.
   *
   * @param applicationModel to modify given the usages of elements that belong to the {@link ExtensionModel}s contained in the
   *        {@code extensions} map.
   * @param extensions set with all the loaded {@link ExtensionModel}s from the deployment that will be filtered by looking up
   *        only those that are coming from an XML context through the {@link XmlExtensionModelProperty} property.
   */
  public MacroExpansionModulesModel(ApplicationModel applicationModel, Set<ExtensionModel> extensions) {
    this.applicationModel = applicationModel;
    this.sortedExtensions = calculateExtensionByTopologicalOrder(extensions);
  }

  /**
   * Goes through the entire xml mule application looking for the message processors that can be expanded, and then takes care of
   * the global elements if there are at least one {@link ExtensionModel} to macro expand.
   */
  public void expand() {
    for (ExtensionModel sortedExtension : sortedExtensions) {
      new MacroExpansionModuleModel(applicationModel, sortedExtension).expand();
    }
  }

  /**
   * Constructs a Direct Acyclic Graph (DAG) with the dependencies at namespace level of those {@link ExtensionModel} that must
   * be macro expanded with a topological order.
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
        .collect(Collectors.toMap(extModel -> extModel.getXmlDslModel().getNamespace(), Function.identity()));

    // we firt check there are at least one extension to macro expand
    if (!allExtensionsByNamespace.isEmpty()) {
      Set<String> extensionsUsedInApp = new HashSet<>();
      applicationModel.executeOnEveryMuleComponentTree(rootComponentModel -> extensionsUsedInApp
          .addAll(getDirectExpandableNamespaceDependencies(rootComponentModel, allExtensionsByNamespace.keySet())));
      // then we assure there are at least one of those extensions being used by the app
      if (!extensionsUsedInApp.isEmpty()) {
        // generation of the DAG and then the topological iterator.
        // it's important to be 100% sure the DAG is not empty, or the TopologicalOrderIterator will fail at start up.
        DirectedGraph<String, DefaultEdge> namespaceDAG = new DirectedMultigraph<>(DefaultEdge.class);
        extensionsUsedInApp.stream().forEach(namespace -> fillDependencyGraph(namespaceDAG, namespace, allExtensionsByNamespace));
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

  private void fillDependencyGraph(DirectedGraph<String, DefaultEdge> g, String sourceVertex,
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

  /**
   * Given a root element of an XML (this is for you Kraan), look for all the used namespaces to which will intersect with the
   * current namespaces of those {@link ExtensionModel}s that must be macro expanded.
   *
   * @param rootComponentModel XML to look for namespace dependencies that *must* be macro expanded.
   * @param namespacesExtensions collection of namespaces from {@link ExtensionModel} that are macro expendables.
   * @return a collection of used namespaces in the current {@code rootComponentModel} that *must* be macro expanded.
   */
  private Set<String> getDirectExpandableNamespaceDependencies(ComponentModel rootComponentModel,
                                                               Set<String> namespacesExtensions) {
    return getUsedNamespaces(rootComponentModel).stream()
        .filter(namespacesExtensions::contains)
        .collect(Collectors.toSet());
  }

  /**
   * Given an XML, will look for all attributes of the root element that start with {@link XMLConstants#XMLNS_ATTRIBUTE} and for
   * each of those, it will pick up the value of it, to then add it to a collection of used namespaces.
   *
   * @param rootComponentModel element to look for the attributes.
   * @return a collection of used namespaces.
   */
  public static Set<String> getUsedNamespaces(ComponentModel rootComponentModel) {
    return rootComponentModel.getParameters().entrySet().stream()
        .filter(parameter -> parameter.getKey().startsWith(XMLNS_ATTRIBUTE + ":"))
        .map(Map.Entry::getValue)
        .collect(Collectors.toSet());
  }
}
