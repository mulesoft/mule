/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.subtypes.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.tck.message.StringAttributes;

@MediaType(TEXT_PLAIN)
public class SubtypesSource extends Source<String, StringAttributes> {

  @Parameter
  public Door doorParam;

  @Override
  public void onStart(SourceCallback<String, StringAttributes> sourceCallback) throws MuleException {
    if (doorParam == null) {
      throw new RuntimeException("Door was null");
    }
    if (!(doorParam instanceof HouseDoor)) {
      throw new RuntimeException("Expected HouseDoor");
    }
    if (((HouseDoor) doorParam).isLocked()) {
      throw new RuntimeException("Expected unlocked door");
    }
  }

  @Override
  public void onStop() {

  }
}
