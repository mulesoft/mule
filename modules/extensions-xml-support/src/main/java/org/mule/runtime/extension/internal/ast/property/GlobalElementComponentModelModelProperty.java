/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.ast.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.ast.api.ComponentAst;

import java.util.List;

/**
 * Property to store all Mule global elements that are contained in an extension written in XML.
 *
 * @since 4.0
 */
public class GlobalElementComponentModelModelProperty implements ModelProperty {

  private static final long serialVersionUID = -664797448198186251L;

  private final List<ComponentAst> globalElements;

  /**
   * Constructs a {@link ModelProperty} that will hold the Mule global elements to be later macro expanded into a Mule
   * application.
   *
   * @param globalElements that will be expanded in a Mule application.
   */
  public GlobalElementComponentModelModelProperty(List<ComponentAst> globalElements) {
    this.globalElements = globalElements;
  }

  /**
   * @return collection of {@link ComponentAst} that will be used to expand the current Mule application XML.
   */
  public List<ComponentAst> getGlobalElements() {
    return globalElements;
  }

  @Override
  public String getName() {
    return "globalElementComponentModelModelProperty";
  }

  @Override
  public boolean isPublic() {
    return false;
  }
}
