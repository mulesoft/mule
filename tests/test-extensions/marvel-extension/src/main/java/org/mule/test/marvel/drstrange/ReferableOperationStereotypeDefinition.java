/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.marvel.drstrange;

import org.mule.runtime.extension.api.stereotype.MuleStereotypes;
import org.mule.runtime.extension.api.stereotype.StereotypeDefinition;

import java.util.Optional;

public class ReferableOperationStereotypeDefinition implements StereotypeDefinition {

  @Override
  public String getName() {
    return "REFERABLE_OPERATION";
  }

  @Override
  public Optional<StereotypeDefinition> getParent() {
    return Optional.of(MuleStereotypes.PROCESSOR_DEFINITION);
  }
}
