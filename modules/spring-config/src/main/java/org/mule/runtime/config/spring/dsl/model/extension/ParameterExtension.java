/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model.extension;

import org.mule.metadata.api.model.MetadataType;

import com.google.common.base.Optional;

/**
 * Class that represents either a <parameter/> in an <operation/> or a <property/> for a <module/>
 * //TODO MULE-10866 : once implemented, this class should go away or refactored as ExtensionModel will be good enough to model an object for a smart connector
 */
public class ParameterExtension {

  private String name;
  private MetadataType type;
  private Optional<String> defaultValue;

  /**
   * @param name of the parameter
   * @param type of the parameter
   * @param defaultValue the default value for a parameter, when null it becomes absent
   */
  public ParameterExtension(String name, MetadataType type, String defaultValue) {
    this.name = name;
    this.type = type;
    this.defaultValue = Optional.fromNullable(defaultValue);
  }

  /**
   * @return the name of the parameter
   */
  public String getName() {
    return name;
  }

  /**
   * @return the type of the parameter
   */
  public MetadataType getType() {
    return type;
  }

  /**
   * @return the default value of a parameter
   */
  public Optional<String> getDefaultValue() {
    return defaultValue;
  }
}
