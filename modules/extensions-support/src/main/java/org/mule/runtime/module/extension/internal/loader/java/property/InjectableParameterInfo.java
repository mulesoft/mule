/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.metadata.api.model.MetadataType;

/**
 * Describes a parameter that can be injected into another component
 *
 * @since 4.4.0
 */
public class InjectableParameterInfo {

  private String parameterName;
  private MetadataType type;
  private boolean required;
  private String extractionExpression;

  public InjectableParameterInfo(String parameterName, MetadataType type, boolean required, String extractionExpression) {
    this.parameterName = parameterName;
    this.type = type;
    this.required = required;
    this.extractionExpression = extractionExpression;
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

  public String getExtractionExpression() {
    return extractionExpression;
  }

  public void setExtractionExpression(String extractionExpression) {
    this.extractionExpression = extractionExpression;
  }
}
