/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.metadata.api.model.MetadataType;

/**
 * Describes a parameter that can be injected into another componen
 *
 * @since 4.4.0
 */
public class InjectableParameterInfo {

  private String parameterName;
  private MetadataType type;
  private boolean required;

  InjectableParameterInfo(String parameterName, MetadataType type, boolean required) {
    this.parameterName = parameterName;
    this.type = type;
    this.required = required;
  }

  public String getParameterName() {
    return parameterName;
  }

  public void setParameterName(String parameterName) {
    this.parameterName = parameterName;
  }

  public MetadataType getType() {
    return type;
  }

  public void setType(MetadataType type) {
    this.type = type;
  }

  public boolean isRequired() {
    return required;
  }

  public void setRequired(boolean required) {
    this.required = required;
  }
}
