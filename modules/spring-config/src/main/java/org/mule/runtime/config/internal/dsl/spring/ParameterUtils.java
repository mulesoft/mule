/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.util.Pair;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

public class ParameterUtils {

  private final ParameterGroupUtils parameterGroupUtils;

  public ParameterUtils() {
    parameterGroupUtils = new ParameterGroupUtils();
  }

  /**
   * Obtains the param name from a given dsl element name
   * 
   * @param componentAst: Parent or root AST component
   * @param name:         The dsl name
   * @return
   */
  protected String getParamName(ComponentAst componentAst, String name) {
    return componentAst.getGenerationInformation().getSyntax()
        .map(dslElementSyntax -> searchParameterNameByElementNameBreadthFirst(name, dslElementSyntax))
        .orElse(null);
  }

  /**
   * A parameter corresponding to a dsl syntax element may be nested inside parameter groups that will not show in the dsl.
   * Because of this, in order to find the entry corresponding to a dsl parameter name in the generation information, a tree-like
   * structure must be traversed. The nodes represent the one-to-one relationship between a parameter name and a dsl syntax
   * element (with it's element name). A breadth search is used because the most common case is that parameters are in the first
   * level.
   *
   * @param name:             The dsl name
   * @param dslElementSyntax: The root syntax with the generation information
   * @return The corresponding parameter name to a dsl name
   */
  private String searchParameterNameByElementNameBreadthFirst(String name, DslElementSyntax dslElementSyntax) {
    // Add first level children to the queue
    Queue<Pair<DslElementSyntax, String>> queue = new ArrayDeque<>(makeChildrenElementsToNamePairList(dslElementSyntax));

    while (!queue.isEmpty()) {
      // Get the oldest node
      Pair<DslElementSyntax, String> currentNode = queue.remove();

      // If it is the one we are looking for, return it
      if (currentNode.getFirst().getElementName().equals(name)) {
        return currentNode.getSecond();
      }

      // Add current node's children to the back of the queue
      queue.addAll(makeChildrenElementsToNamePairList(currentNode.getFirst()));
    }

    return null;
  }

  /**
   * Given dsl element, generates a list of tuples (Pair) that bundle the parameter name with the dsl syntax information of this
   * element's children
   * 
   * @param dslElementSyntax A root syntax
   * @return
   */
  private List<Pair<DslElementSyntax, String>> makeChildrenElementsToNamePairList(DslElementSyntax dslElementSyntax) {
    return dslElementSyntax.getContainedElementsByName().entrySet().stream()
        .map(entry -> new Pair<>(entry.getValue(), entry.getKey()))
        .collect(toList());
  }

}
