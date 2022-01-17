/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.app.internal.loader.parser;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;

import java.util.Optional;

abstract class BaseAppExtensionModelParser {

  protected String requiredString(ComponentAst ast, String paramName) {
    String value = getResolvedParameter(ast, paramName);

    if (isBlank(value)) {
      throw new IllegalModelDefinitionException(format("Element <%s:s> defines a blank value for required parameter '%s'. Component Location: %s",
          ast.getIdentifier().getNamespace(), ast.getIdentifier().getName(), ast.getLocation().getLocation()));
    }
    return value;
  }

  protected Optional<String> optionalString(ComponentAst ast, String paramName) {
    return ofNullable(getResolvedParameter(ast, paramName));
  }

  protected String optionalString(ComponentAst ast, String paramName, String defaultValue) {
    return optionalString(ast, paramName).orElse(defaultValue);
  }

  protected String getResolvedParameter(ComponentAst ast, String paramName) {
    ComponentParameterAst parameter = ast.getParameter(DEFAULT_GROUP_NAME, paramName);
    return parameter != null ? parameter.getResolvedRawValue() : null;
  }

}
