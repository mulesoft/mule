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

import static java.util.stream.Collectors.toMap;

public class ParameterUtils {

  static String getParamName(ComponentAst componentAst, String name) {

    if (!componentAst.getGenerationInformation().getSyntax().isPresent()) {
      return null;
    }

    return getElementNameToParamNameMap(componentAst.getGenerationInformation().getSyntax().get()).get(name);

  }

  static Map<String, String> getElementNameToParamNameMap(DslElementSyntax dslElementSyntax) {

    Map<String, DslElementSyntax> containedElementsByName = dslElementSyntax.getContainedElementsByName();

    // Add direct children to the dictionary
    Map<String, String> result = containedElementsByName.keySet().stream()
        .collect(
                 toMap(
                       key -> containedElementsByName.get(key).getElementName(),
                       key -> key,
                       (a, b) -> b));

    // Add its childrens to the dictionary
    containedElementsByName.values().stream().map(DslElementSyntax::getContainedElementsByName)
        .forEach(elementsByName -> elementsByName.keySet()
            .forEach(key -> result.put(elementsByName.get(key).getElementName(), key)));

    return result;
  }
}
