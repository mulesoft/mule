/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static java.util.regex.Pattern.compile;

import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;

public final class ParameterGroupUtils {

  private final static Pattern SANITIZE_PATTERN = compile("\\s+");

  /**
   * Resolves a parameter from a source component, taking into account that the success/error callbacks on the source may have
   * parameters with the same name.
   * <p>
   * For sources, we need to account for the case where parameters in the callbacks may have colliding names. This logic ensures
   * that the parameter fetching logic is consistent with the logic that handles this scenario in previous implementations.
   *
   * @param ownerComponent
   * @param parameterName
   * @param possibleGroup
   * @param ownerComponentModel
   * @return the resolved parameter
   */
  public ComponentParameterAst getSourceCallbackAwareParameter(ComponentAst ownerComponent, String parameterName,
                                                               ComponentAst possibleGroup,
                                                               SourceModel ownerComponentModel) {

    Optional<ParameterGroupModel> groupModelOptional = getSourceParamGroups(ownerComponentModel).stream()
        .filter(parameterGroupModel -> parameterGroupModel.getParameter(parameterName).isPresent() &&
            parameterGroupModel.isShowInDsl() &&
            getChildElementName(ownerComponent, parameterGroupModel)
                .map(en -> possibleGroup.getIdentifier().getName().equals(en))
                .orElse(false))
        .findFirst();

    if (!groupModelOptional.isPresent()) {
      return null;
    }

    ComponentParameterAst parameter = ownerComponent.getParameter(groupModelOptional.get().getName(), parameterName);

    if (parameter == null) {
      return ownerComponent.getParameter(parameterName);
    }

    return parameter;

  }

  public ComponentParameterAst getComponentParameterAstFromSourceModel(CreateBeanDefinitionRequest createBeanDefinitionRequest,
                                                                       ComponentAst ownerComponent, String paramName,
                                                                       SourceModel ownerComponentModel) {
    // For sources, we need to account for the case where parameters in the callbacks may have colliding names.
    // This logic ensures that the parameter fetching logic is consistent with the logic that handles this scenario in
    // previous implementations.
    int ownerIndex = createBeanDefinitionRequest.getComponentModelHierarchy().indexOf(ownerComponent);
    final ComponentAst possibleGroup = createBeanDefinitionRequest.getComponentModelHierarchy().get(ownerIndex + 1);

    return getSourceCallbackAwareParameter(ownerComponent, paramName, possibleGroup, ownerComponentModel);
  }

  private List<ParameterGroupModel> getSourceParamGroups(SourceModel ownerComponentModel) {
    List<ParameterGroupModel> sourceParamGroups = new ArrayList<>(ownerComponentModel.getParameterGroupModels());

    ownerComponentModel.getSuccessCallback()
        .ifPresent(scb -> sourceParamGroups.addAll(scb.getParameterGroupModels()));

    ownerComponentModel.getErrorCallback()
        .ifPresent(ecb -> sourceParamGroups.addAll(ecb.getParameterGroupModels()));
    return sourceParamGroups;
  }

  private Optional<String> getChildElementName(ComponentAst ownerComponent, NamedObject component) {
    return ownerComponent.getGenerationInformation().getSyntax()
        .flatMap(stx -> stx.getChild(component.getName()))
        .map(DslElementSyntax::getElementName);
  }
}
