/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;

public class ParameterUtils {

  static String getParamName(ComponentAst componentAst, String name) {

    if (!componentAst.getGenerationInformation().getSyntax().isPresent()) {
      return null;
    }

    return getElementNameToParamNameMap(componentAst.getGenerationInformation().getSyntax().get()).get(name);

  }

  static HashMap<String, String> getElementNameToParamNameMap(DslElementSyntax dslElementSyntax) {
    // Map whose key is the DSL representation (element name) and whose value is the model parameter name (the previous key)
    return dslElementSyntax.getContainedElementsByName().entrySet().stream()
        .collect(Collectors.toMap(entry -> entry.getValue().getElementName(), Map.Entry::getKey, (a, b) -> b, HashMap::new));
  }
}
