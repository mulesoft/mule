/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;

/**
 * Marker {@link ModelProperty} for indicating that the component has been declared using a developer-defined custom
 * {@link StereotypeModel} instead of having the default stereotype for the component.
 *
 * @since 4.2.0
 */
public class CustomStereotypeModelProperty implements ModelProperty {

  public final static CustomStereotypeModelProperty INSTANCE = new CustomStereotypeModelProperty();

  /**
   * @deprecated since 4.5.0. Use {@link #INSTANCE} instead
   */
  @Deprecated
  public CustomStereotypeModelProperty() {}

  @Override
  public String getName() {
    return "CustomStereotype";
  }

  @Override
  public boolean isPublic() {
    return false;
  }
}
