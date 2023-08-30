/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension.stereotype;

import org.mule.runtime.extension.api.stereotype.StereotypeDefinition;

import java.util.Optional;

public class CustomPetstoreConnectionStereotype implements StereotypeDefinition {

  @Override
  public String getName() {
    return "CUSTOM_CONNECTION";
  }

  @Override
  public Optional<StereotypeDefinition> getParent() {
    return Optional.of(new ZooConnectionStereotype());
  }
}
