/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;

public class Utils {

  private Utils() {}

  public static ComponentAst mockDeprecatedAst(String since, String message, String toRemoveIn) {
    ComponentParameterAst sinceAst = stringParameterAst(since);
    ComponentParameterAst messageAst = stringParameterAst(message);
    ComponentParameterAst toRemoveInAst = stringParameterAst(toRemoveIn);

    ComponentAst deprecatedAst = mock(ComponentAst.class);
    when(deprecatedAst.getParameter(DEFAULT_GROUP_NAME, "since")).thenReturn(sinceAst);
    when(deprecatedAst.getParameter(DEFAULT_GROUP_NAME, "message")).thenReturn(messageAst);
    when(deprecatedAst.getParameter(DEFAULT_GROUP_NAME, "toRemoveIn")).thenReturn(toRemoveInAst);

    return deprecatedAst;
  }

  public static ComponentParameterAst stringParameterAst(String value) {
    ComponentParameterAst parameterAst = mock(ComponentParameterAst.class);
    when(parameterAst.getValue()).thenReturn(right(value));
    return parameterAst;
  }
}
