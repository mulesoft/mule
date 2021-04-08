/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;

import static java.util.stream.Collectors.toMap;

public class ParameterUtils {

  public String getParamName(ComponentAst componentAst, String name) {

    if (!componentAst.getGenerationInformation().getSyntax().isPresent()) {
      return null;
    }

    DslElementSyntax dslElementSyntax = componentAst.getGenerationInformation().getSyntax().get();

    return breathSearchParameterNameByElementName(name, dslElementSyntax);
  }

  private String breathSearchParameterNameByElementName(String name, DslElementSyntax dslElementSyntax) {
    Queue<NamePair> queue = new ArrayDeque<>(makeElementsToNamePairList(dslElementSyntax));

    while (!queue.isEmpty()) {
      NamePair currentNode = queue.remove();

      if (currentNode.getDslElementSyntax().getElementName().equals(name)) {
        return currentNode.getParamName();
      }

      queue.addAll(makeElementsToNamePairList(currentNode.getDslElementSyntax()));
    }

    return null;
  }

  private List<NamePair> makeElementsToNamePairList(DslElementSyntax dslElementSyntax) {
    return dslElementSyntax.getContainedElementsByName().entrySet().stream()
        .map(entry -> new NamePair(entry.getKey(), entry.getValue())).collect(Collectors.toList());
  }

  class NamePair {

    private String paramName;
    private DslElementSyntax dslElementSyntax;

    public NamePair(String paramName, DslElementSyntax dslElementSyntax) {
      this.paramName = paramName;
      this.dslElementSyntax = dslElementSyntax;
    }

    public String getParamName() {
      return paramName;
    }

    public void setParamName(String paramName) {
      this.paramName = paramName;
    }

    public DslElementSyntax getDslElementSyntax() {
      return dslElementSyntax;
    }

    public void setDslElementSyntax(DslElementSyntax dslElementSyntax) {
      this.dslElementSyntax = dslElementSyntax;
    }
  }

}
