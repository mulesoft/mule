/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension.model.types;

import org.mule.runtime.core.api.message.BaseAttributes;

public class DEAOfficerAttributes extends BaseAttributes {

  private final boolean isHank;

  public DEAOfficerAttributes(boolean isHank) {
    this.isHank = isHank;
  }

  public boolean isHank() {
    return isHank;
  }
}
