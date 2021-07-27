/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.util;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;

import java.util.Optional;

public class AstUtils {

  private AstUtils() {
    // Private constructor to avoid incorrect usage.
  }

  // TODO MULE-19614: Remove this and use {@link ComponentAst#getParameter(groupName, paramName)} instead.
  public static ComponentParameterAst getParameter(ComponentAst element, String parameterName, ParameterGroupModel groupModel) {
    return tryGetGroupName(groupModel).map(groupName -> element.getParameter(groupName, parameterName))
        .orElseGet(() -> element.getParameters().stream().filter(pm -> pm.getModel().getName().equals(parameterName)).findFirst()
            .orElse(null));
  }

  private static Optional<String> tryGetGroupName(ParameterGroupModel groupModel) {
    if (groupModel == null) {
      return empty();
    }

    try {
      return of(groupModel.getName());
    } catch (IllegalArgumentException e) {
      return empty();
    }
  }
}
