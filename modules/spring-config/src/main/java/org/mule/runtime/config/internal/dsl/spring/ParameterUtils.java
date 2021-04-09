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
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;

public class ParameterUtils {

  private ParameterGroupUtils parameterGroupUtils;

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

  private String searchParameterNameByElementNameBreadthFirst(String name, DslElementSyntax dslElementSyntax) {
    Queue<NameToSyntaxPair> queue = new ArrayDeque<>(makeElementsToNamePairList(dslElementSyntax));

    while (!queue.isEmpty()) {
      NameToSyntaxPair currentNode = queue.remove();

      if (currentNode.getDslElementSyntax().getElementName().equals(name)) {
        return currentNode.getParamName();
      }

      queue.addAll(makeElementsToNamePairList(currentNode.getDslElementSyntax()));
    }

    return null;
  }

  private List<NameToSyntaxPair> makeElementsToNamePairList(DslElementSyntax dslElementSyntax) {
    return dslElementSyntax.getContainedElementsByName().entrySet().stream()
        .map(entry -> new NameToSyntaxPair(entry.getKey(), entry.getValue())).collect(Collectors.toList());
  }

  class NameToSyntaxPair {

    private String paramName;
    private DslElementSyntax dslElementSyntax;

    public NameToSyntaxPair(String paramName, DslElementSyntax dslElementSyntax) {
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
