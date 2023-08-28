/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.stereotypes;

import org.mule.runtime.extension.api.stereotype.StereotypeDefinition;

import java.util.Optional;

public class KillingStereotype implements StereotypeDefinition {

  @Override
  public String getName() {
    return "killingOperation";
  }

  @Override
  public Optional<StereotypeDefinition> getParent() {
    return Optional.of(new EmpireStereotype());
  }

}
