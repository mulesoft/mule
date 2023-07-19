/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.api.dsl.model;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.util.Pair;

import java.util.List;

/**
 * Representation of the DSL configuration parameters of a component.
 * 
 * @since 4.1
 */
@NoImplement
public interface ConfigurationParameters {

  /**
   * Gets the value of a simple parameter as an string.
   * 
   * @param parameterName parameter name, same as in the config.
   * @return the parameter value as an string.
   */
  String getStringParameter(String parameterName);

  /**
   * Gets all the configuration of a complex parameter type with an specific {@link ComponentIdentifier}.
   * 
   * @param componentIdentifier the component identifier of the parameter.
   * @return the complex parameter with the given {@link ComponentIdentifier}
   */
  List<ConfigurationParameters> getComplexConfigurationParameter(ComponentIdentifier componentIdentifier);

  /**
   * Gets all the complex parameters and they related identifier
   * 
   * @return all the complex parameters.
   */
  List<Pair<ComponentIdentifier, ConfigurationParameters>> getComplexConfigurationParameters();

}
