/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model.extension;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;

import java.util.List;

/**
 * Property to store all Mule global elements that are contained in an extension written in XML.
 *
 * @since 4.0
 */
public class GlobalElementComponentModelModelProperty implements ModelProperty {

  private final List<ComponentModel> globalElements;

  public GlobalElementComponentModelModelProperty(List<ComponentModel> globalElements) {
    this.globalElements = globalElements;
  }

  /**
   * @return collection of {@link ComponentModel} that will be used to expand the current Mule application XML.
   */
  public List<ComponentModel> getGlobalElements() {
    return globalElements;
  }

  @Override
  public String getName() {
    return "globalElementComponentModelModelProperty";
  }

  @Override
  public boolean isExternalizable() {
    return false;
  }
}
