/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;

import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.extension.api.model.deprecated.ImmutableDeprecationModel;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Base class for the Mule SDK extension model parsers. Contains base behaviour for parsing the Mule DSL definitions using the AST
 * API.
 *
 * @since 4.5.0
 */
abstract class BaseMuleSdkExtensionModelParser {

  static final String DEPRECATED_CONSTRUCT_NAME = "deprecated";
  private static final String MESSAGE_PARAMETER = "message";
  private static final String SINCE_PARAMETER = "since";
  private static final String TO_REMOVE_IN_PARAMETER = "toRemoveIn";

  /**
   * Returns the value of a parameter in the given {@code ast}. The parameter is assumed to either be required or have a default
   * defined.
   *
   * @param ast       a {@link ComponentAst}
   * @param paramName the name of the parameter
   * @param <T>       the param's generic type
   * @return The parameter value
   */
  protected <T> T getParameter(ComponentAst ast, String paramName) {
    return (T) ast.getParameter(DEFAULT_GROUP_NAME, paramName).getValue().getRight();
  }

  /**
   * Returns the value of an optional parameter in the give {@code ast}.
   *
   * @param ast       a {@link ComponentAst}
   * @param paramName the name of the parameter
   * @param <T>       the param's generic type
   * @return an {@link Optional} for the parameter's value
   */
  protected <T> Optional<T> getOptionalParameter(ComponentAst ast, String paramName) {
    return ofNullable(ast.getParameter(DEFAULT_GROUP_NAME, paramName)).map(paramAst -> paramAst.<T>getValue().getRight());
  }

  /**
   * @param component a {@link ComponentAst}
   * @param childName the child element name
   * @return A {@link Stream} with all the direct children with the given {@code childName}
   */
  protected Stream<ComponentAst> getChildren(ComponentAst component, String childName) {
    return component.directChildrenStreamByIdentifier(null, childName);
  }

  /**
   * @param component
   * @param childName
   * @return The first direct child (if any) with the given {@code childName}
   */
  protected Optional<ComponentAst> getSingleChild(ComponentAst component, String childName) {
    return getChildren(component, childName).findFirst();
  }

  /**
   * Builds a deprecation model by parsing the content of the {@code deprecated} parameter AST.
   *
   * @param deprecationAst The deprecation parameter AST.
   * @return The corresponding {@link DeprecationModel}.
   */
  protected DeprecationModel buildDeprecationModel(ComponentAst deprecationAst) {
    String message = getParameter(deprecationAst, MESSAGE_PARAMETER);
    String since = getParameter(deprecationAst, SINCE_PARAMETER);
    String toRemoveIn = this.<String>getOptionalParameter(deprecationAst, TO_REMOVE_IN_PARAMETER).orElse(null);
    return new ImmutableDeprecationModel(message, since, toRemoveIn);
  }
}
