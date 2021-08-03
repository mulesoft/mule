/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.meta.model.ModelProperty;

import java.util.List;
import java.util.Optional;

public interface NestedRouteModelParser {

  String getName();

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

  List<ParameterGroupModelParser> getParameterGroupModelParsers();

  List<ModelProperty> getAdditionalModelProperties();

}
