/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;

public final class ParameterGroupModelProperty implements ModelProperty {

  private final ParameterGroupDescriptor groupDescriptor;

  public ParameterGroupModelProperty(ParameterGroupDescriptor groupDescriptor) {
    this.groupDescriptor = groupDescriptor;
  }

  public ParameterGroupDescriptor getDescriptor() {
    return groupDescriptor;
  }

  /**
   * @return {@code parameterGroup}
   */
  @Override
  public String getName() {
    return "parameterGroup";
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isPublic() {
    return false;
  }
}
