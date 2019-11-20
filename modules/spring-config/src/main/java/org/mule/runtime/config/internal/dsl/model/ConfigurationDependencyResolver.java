/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraphFactory.generateFor;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraph;
import org.mule.runtime.config.internal.model.ApplicationModel;

import java.util.Collection;
import java.util.function.Predicate;

public class ConfigurationDependencyResolver {

  private final ArtifactAstDependencyGraph appModelDependencyGraph;

  /**
   * Creates a new instance associated to a complete {@link ApplicationModel}.
   *
   * @param applicationModel the artifact {@link ApplicationModel}.
   */
  public ConfigurationDependencyResolver(ArtifactAst applicationModel) {
    this.appModelDependencyGraph = generateFor(applicationModel);
  }

  /**
   * @param componentName the name attribute value of the component
   * @return the dependencies of the component with component name {@code #componentName}. An empty collection if there is no
   *         component with such name.
   */
  public Collection<String> resolveComponentDependencies(String componentName) {
    return appModelDependencyGraph
        .minimalArtifactFor(new ComponentNamePredicate(componentName))
        .recursiveStream()
        .filter(comp -> comp.getComponentId().isPresent())
        .map(comp -> comp.getComponentId().get())
        .filter(name -> !name.equals(componentName))
        .collect(toList());
  }

  private static class ComponentNamePredicate implements Predicate<ComponentAst> {

    private final String componentName;

    public ComponentNamePredicate(String componentName) {
      this.componentName = componentName;
    }

    @Override
    public boolean test(ComponentAst comp) {
      return comp.getComponentId().map(n -> n.equals(componentName)).orElse(false);
    }

    @Override
    public String toString() {
      return "componentName='" + componentName + "'";
    }
  }
}
