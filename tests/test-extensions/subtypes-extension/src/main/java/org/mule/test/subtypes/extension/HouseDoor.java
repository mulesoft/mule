/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.subtypes.extension;

import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

public class HouseDoor implements Door {

  @Parameter
  private boolean isLocked;

  @Parameter
  @Optional(defaultValue = "pivotal")
  private String handle;

  @Override
  public void open() {}

  public boolean isLocked() {
    return isLocked;
  }

  public void setLocked(boolean locked) {
    isLocked = locked;
  }

  @Override
  public String getHandle() {
    return this.handle;
  }
}
