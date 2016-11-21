/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model.extension.xml;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.core.api.processor.Processor;

/**
 * Property to store all {@link ComponentModel} of an operation's <body/> that are contained in an extension written in XML.
 *
 * @since 4.0
 */
public class OperationComponentModelModelProperty implements ModelProperty {

  private final ComponentModel componentModel;

  /**
   * Constructs a {@link ModelProperty} that will hold the {@link Processor}s defined in a <body/> element of an <operation/>
   * to be later macro expanded into a Mule application.
   *
   * @param componentModel <body/> element with all the {@link Processor} represented through {@link ComponentModel}s.
   */
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
