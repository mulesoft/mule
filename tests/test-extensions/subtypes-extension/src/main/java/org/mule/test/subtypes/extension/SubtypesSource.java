/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
