/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.api.meta.model.nested.ChainExecutionOccurrence;
import org.mule.runtime.api.meta.model.nested.NestedRouteModel;

import java.util.List;
import java.util.Optional;

/**
 * Parses the syntactic definition of a {@link NestedRouteModel} so that the semantics reflected in it can be extracted in a
 * uniform way, regardless of the actual syntax used by the extension developer.
 *
 * @see ExtensionModelParser
 * @since 4.5.0
 */
public interface NestedRouteModelParser extends SemanticTermsParser, AllowedStereotypesModelParser, SdkApiAwareParser {

  /**
   * @return the route's name
   */
  String getName();

  /**
   * @return the route's description
   */
  String getDescription();

  /**
   * Represents the minimum amount of times that this route can be used inside the owning component.
   *
   * @return An int greater or equal to zero
   */
  int getMinOccurs();

  /**
   * {@link Optional} value which represents the maximum amount of times that this route can be used inside the owning component.
   *
   * @return If present, a number greater or equal to zero.
   */
  Optional<Integer> getMaxOccurs();

  /**
   * Returns a list with a {@link ParameterGroupModelParser} per each parameter group defined in the route. Each group is listed
   * in the same order as defined in the syntax.
   *
   * @return a list with the config's {@link ParameterGroupModelParser}
   */
  List<ParameterGroupModelParser> getParameterGroupModelParsers();

  /**
   * Returns a list with all the {@link ModelProperty model properties} to be applied at the route level which are specifically
   * linked to the type of syntax used to define the extension.
   *
   * @return a list with {@link ModelProperty} instances.
   */
  List<ModelProperty> getAdditionalModelProperties();

  /**
   * @return the router's {@link DeprecationModel} if one was defined
   */
  Optional<DeprecationModel> getDeprecationModel();

  /**
   * @return the {@link ChainExecutionOccurrence} for the route's inner chain
   * @since 4.7.0
   */
  ChainExecutionOccurrence getExecutionOccurrence();

  /**
   * @return whether the parsed model may be present more than once in its component.
   *
   * @since 4.9.7
   */
  default boolean isListOfRoutes() {
    return false;
  }

}
