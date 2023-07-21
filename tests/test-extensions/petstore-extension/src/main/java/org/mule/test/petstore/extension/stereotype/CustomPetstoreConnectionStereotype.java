/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
