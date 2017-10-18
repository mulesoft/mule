/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.extension.xml.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.core.api.processor.Processor;

/**
 * Property to store the <operation/>'s {@link ComponentModel}  and the <body/> that are contained in an extension written in XML.
 *
 * @since 4.0
 */
public class OperationComponentModelModelProperty implements ModelProperty {

  private final ComponentModel operationComponentModel;
  private final ComponentModel bodyComponentModel;

  /**
   * Constructs a {@link ModelProperty} that will hold the complete <operation/> and its {@link Processor}s defined in a <body/>
   * element to be later macro expanded into a Mule application.
   *
   * @param operationComponentModel <operation/> element represented through {@link ComponentModel}s.
   * @param bodyComponentModel <body/> element with all the {@link Processor} represented through {@link ComponentModel}s.
   */
  public OperationComponentModelModelProperty(ComponentModel operationComponentModel, ComponentModel bodyComponentModel) {
    this.operationComponentModel = operationComponentModel;
    this.bodyComponentModel = bodyComponentModel;
  }

  /**
   * @return the {@link ComponentModel} that's pointing to the <operation/> element
   */
  public ComponentModel getOperationComponentModel() {
    return operationComponentModel;
  }

  /**
   * @return the {@link ComponentModel} that's pointing to the <body/> element
   */
  public ComponentModel getBodyComponentModel() {
    return bodyComponentModel;
  }

  @Override
  public String getName() {
    return "componentModelModelProperty";
  }

  @Override
  public boolean isPublic() {
    return false;
  }
}
