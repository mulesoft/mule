/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.parameterization.ComponentParameterization;
import java.util.Map;

/**
 * Utils class to create {@link ComponentParameterization}s.
 *
 * @since 4.5
 */
public class ComponentParameterizationUtils {

  private ComponentParameterizationUtils() {}

  /**
   * Creates a {@link ComponentParameterization} of a {@link ParameterizedModel} based on static values of its parameters. Keep in
   * mind that this method will not work if the given {@link ParameterizedModel} contains parameter that share the same name,
   * since the map representation falls short in that case.
   *
   * @param parameterizedModel parameterizedModel to be described by the result
   * @param parameters         static parameter values.
   * @return a component parameterization that describes the given value.
   */
  public static ComponentParameterization createComponentParameterization(ParameterizedModel parameterizedModel,
                                                                          Map<String, Object> parameters) {
    ComponentParameterization.Builder componentParameterizationBuilder = ComponentParameterization.builder(parameterizedModel);
    parameters.entrySet().stream()
        .forEach(parameterEntry -> componentParameterizationBuilder.withParameter(parameterEntry.getKey(),
                                                                                  parameterEntry.getValue()));
    return componentParameterizationBuilder.build();
  }

}
