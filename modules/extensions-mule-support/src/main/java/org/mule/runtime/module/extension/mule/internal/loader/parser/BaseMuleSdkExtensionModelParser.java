/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;

import org.mule.runtime.ast.api.ComponentAst;

import java.util.Optional;

abstract class BaseMuleSdkExtensionModelParser {

  protected <T> T getParameter(ComponentAst ast, String paramName) {
    return (T) ast.getParameter(DEFAULT_GROUP_NAME, paramName).getValue().getRight();
  }

  protected <T> Optional<T> getOptionalParameter(ComponentAst ast, String paramName) {
    return ofNullable(getParameter(ast, paramName));
  }
}
