/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static java.util.regex.Pattern.compile;
import static org.apache.commons.lang3.text.WordUtils.capitalize;
import static org.mule.runtime.api.util.NameUtils.hyphenize;
import static org.mule.runtime.api.util.NameUtils.sanitizeName;

import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class ParameterGroupUtils {

  private final static Pattern SANITIZE_PATTERN = compile("\\s+");

  private ParameterGroupUtils() {
    // Nothing to do
  }

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
  public static ComponentParameterAst getSourceCallbackAwareParameter(ComponentAst ownerComponent, String parameterName,
                                                                      ComponentAst possibleGroup,
                                                                      SourceModel ownerComponentModel) {
    List<ParameterGroupModel> sourceParamGroups = new ArrayList<>();
    sourceParamGroups.addAll(ownerComponentModel.getParameterGroupModels());
    ownerComponentModel.getSuccessCallback()
        .ifPresent(scb -> sourceParamGroups.addAll(scb.getParameterGroupModels()));
    ownerComponentModel.getErrorCallback()
        .ifPresent(ecb -> sourceParamGroups.addAll(ecb.getParameterGroupModels()));

    for (ParameterGroupModel parameterGroupModel : sourceParamGroups) {
      if (parameterGroupModel.getParameter(parameterName).isPresent()
          && parameterGroupModel.isShowInDsl()
          && possibleGroup.getIdentifier().getName().equals(getSanitizedElementName(parameterGroupModel))) {
        ComponentParameterAst parameter = ownerComponent.getParameter(parameterGroupModel.getName(), parameterName);

        if (parameter == null) {
          return ownerComponent.getParameter(parameterName);
        } else {
          return parameter;
        }
      }
    }

    return null;
  }

  /**
   * Provides a sanitized, hyphenized, space-free name that can be used as an XML element-name for a given {@link NamedObject}
   *
   * @param component the {@link NamedObject} who's name we want to convert
   * @return a sanitized, hyphenized, space-free name that can be used as an XML element-name
   */
  // TODO MULE-18660: remove and use a resolved DSLElementSyntax available in the ast
  private static String getSanitizedElementName(NamedObject component) {
    return SANITIZE_PATTERN.matcher(hyphenize(sanitizeName(capitalize(component.getName())))).replaceAll("");
  }

}
