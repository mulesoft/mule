/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.subtypes.extension;

import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

@TypeDsl(allowTopLevelDefinition = true)
public class HouseDoor implements Door {

  @Parameter
  private boolean locked;

  @Parameter
  @Optional(defaultValue = "pivotal")
  private String handle;

  @Override
  public void open() {}

  public boolean isLocked() {
    return locked;
  }

  public void setLocked(boolean locked) {
    this.locked = locked;
  }

  @Override
  public String getHandle() {
    return this.handle;
  }
}
