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
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;

public class ParameterUtils {

  private final ParameterGroupUtils parameterGroupUtils;

  public ParameterUtils() {
    parameterGroupUtils = new ParameterGroupUtils();
  }

  public ComponentParameterAst getParamInOwnerComponent(CreateBeanDefinitionRequest createBeanDefinitionRequest) {
    ComponentAst ownerComponent = createBeanDefinitionRequest.resolveOwnerComponent();
    ComponentAst componentModel = createBeanDefinitionRequest.getComponentModel();

    if (ownerComponent == null) {
      return null;
    }

    final String paramName = getParamName(ownerComponent, componentModel.getIdentifier().getName());

    ParameterizedModel ownerComponentModel = ownerComponent.getModel(ParameterizedModel.class).get();

    if (ownerComponent != componentModel && ownerComponentModel instanceof SourceModel) {
      return parameterGroupUtils.getComponentParameterAstFromSourceModel(createBeanDefinitionRequest, ownerComponent, paramName,
                                                                         (SourceModel) ownerComponentModel);
    }

    if (paramName == null) {
      return ownerComponent.getParameter(componentModel.getIdentifier().getName());
    }

    ComponentParameterAst paramInOwner = ownerComponent.getParameter(paramName);

    if (paramInOwner == null) {
      // XML SDK 1 allows for hyphenated names in parameters, so need to account for those.
      return ownerComponent.getParameter(componentModel.getIdentifier().getName());
    }

    return paramInOwner;
  }

  protected String getParamName(ComponentAst componentAst, String name) {
    return componentAst.getGenerationInformation().getSyntax()
        .map(dslElementSyntax -> searchParameterNameByElementNameBreadthFirst(name, dslElementSyntax))
        .orElse(null);
  }

  /**
   * A parameter corresponding to a dsl syntax element maybe nested inside parameter groups that will not show in the dsl. Because
   * of this, in order to find the entry corresponding to a dsl parameter name in the generation information, a tree-like
   * structure must be traversed. A breadth search is used because the most common case is that parameters are in the first level.
   *
   * @param name:             The dsl name
   * @param dslElementSyntax: The root syntax with the generation information
   * @return The corresponding parameter name to a dsl name
   */
  private String searchParameterNameByElementNameBreadthFirst(String name, DslElementSyntax dslElementSyntax) {
    Queue<Pair<DslElementSyntax, String>> queue = new ArrayDeque<>(makeElementsToNamePairList(dslElementSyntax));

    while (!queue.isEmpty()) {
      Pair<DslElementSyntax, String> currentNode = queue.remove();

      if (currentNode.getFirst().getElementName().equals(name)) {
        return currentNode.getSecond();
      }

      queue.addAll(makeElementsToNamePairList(currentNode.getFirst()));
    }

    return null;
  }

  private List<Pair<DslElementSyntax, String>> makeElementsToNamePairList(DslElementSyntax dslElementSyntax) {
    return dslElementSyntax.getContainedElementsByName().entrySet().stream()
        .map(entry -> new Pair<>(entry.getValue(), entry.getKey()))
        .collect(Collectors.toList());
  }

}
