/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static java.util.stream.Stream.empty;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;

import java.util.stream.Stream;

/**
 * Utilities class to ease the mock of the AST that the operation model parser is going to parse.
 */
final class Utils {

  private Utils() {}

  /**
   * Creates a mock {@link ComponentAst} with the parameters of a "deprecated" construct.
   *
   * @param since      String to return as the "since" parameter of the mock construct.
   * @param message    String to return as the "message" parameter of the mock construct.
   * @param toRemoveIn String to return as the "toRemoveIn" parameter of the mock construct.
   *
   * @return the mock {@link ComponentAst}.
   */
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

  /**
   * Creates a mock {@link ComponentParameterAst} with a single String.
   *
   * @param value the String value.
   *
   * @return the mock {@link ComponentParameterAst}.
   */
  public static ComponentParameterAst stringParameterAst(String value) {
    ComponentParameterAst parameterAst = mock(ComponentParameterAst.class);
    when(parameterAst.getValue()).thenReturn(right(value));
    return parameterAst;
  }

  /**
   * Creates a mock {@link ComponentAst} for an "operation:output" construct.
   *
   * @param payloadType    The type parameter value for the "payload-type" element.
   * @param attributesType The type parameter value for the "attributes-type" element.
   *
   * @return the mock {@link ComponentAst}.
   */
  public static ComponentAst mockOutputAst(String payloadType, String attributesType) {
    ComponentAst outputAst = mock(ComponentAst.class);
    mockTypeElement(outputAst, "payload-type", payloadType);
    mockTypeElement(outputAst, "attributes-type", attributesType);
    return outputAst;
  }

  /**
   * Sets a child ast to the given component ast, with the given child name.
   *
   * @param componentAst The {@link ComponentAst} where the child is going to be added.
   * @param childName    The child name.
   * @param childAst     The child AST.
   */
  public static void setMockAstChild(ComponentAst componentAst, String childName, ComponentAst childAst) {
    when(componentAst.directChildrenStreamByIdentifier(null, childName)).thenAnswer(invocation -> Stream.of(childAst));
  }

  private static void mockTypeElement(ComponentAst outputAst, String elementName, String typeName) {
    when(outputAst.directChildrenStreamByIdentifier(null, elementName)).thenAnswer(invocation -> {
      if (typeName != null) {
        ComponentParameterAst attributesTypeParameterAst = stringParameterAst(typeName);
        ComponentAst attributesTypeAst = mock(ComponentAst.class);
        when(attributesTypeAst.getIdentifier())
            .thenReturn(ComponentIdentifier.builder().namespace("this").name(elementName).build());
        when(attributesTypeAst.getParameter(DEFAULT_GROUP_NAME, "type")).thenReturn(attributesTypeParameterAst);
        return Stream.of(attributesTypeAst);
      } else {
        return empty();
      }
    });
  }
}
