/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model.extension;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;

/**
 * Property to store all {@link ComponentModel} of an operation's <body/> that are contained in an extension written in XML.
 *
 * @since 4.0
 */
public class OperationComponentModelModelProperty implements ModelProperty {

  private final ComponentModel componentModel;

  public OperationComponentModelModelProperty(ComponentModel componentModel) {
    this.componentModel = componentModel;
  }

  /**
   * @return the {@link ComponentModel} that's pointing to the <body/> element
   */
  public ComponentModel getComponentModel() {
    return componentModel;
  }

  @Override
  public String getName() {
    return "componentModelModelProperty";
  }

  @Override
  public boolean isExternalizable() {
    return false;
  }
}
