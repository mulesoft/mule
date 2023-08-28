/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.extension.internal.loader.xml.validator.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;

/**
 * Marker model property to be later picked up by {@link ExtensionModelValidator}
 *
 * @since 4.1.3
 */
public class InvalidTestConnectionMarkerModelProperty implements ModelProperty {

  final String markedElement;
  final String offendingElement;

  public InvalidTestConnectionMarkerModelProperty(String markedElement, String offendingElement) {
    this.markedElement = markedElement;
    this.offendingElement = offendingElement;
  }

  public String getMarkedElement() {
    return markedElement;
  }

  public String getOffendingElement() {
    return offendingElement;
  }

  @Override
  public String getName() {
    return "invalidTestConnectionMarkerModelProperty";
  }

  @Override
  public boolean isPublic() {
    return false;
  }
}
