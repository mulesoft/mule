/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.stereotypes;


import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.PROCESSOR_DEFINITION;
import org.mule.runtime.extension.api.stereotype.StereotypeDefinition;

import java.util.Optional;

public class EmpireStereotype implements StereotypeDefinition {

  @Override
  public String getName() {
    return "heisenberg-empire";
  }

  @Override
  public Optional<StereotypeDefinition> getParent() {
    return Optional.of(PROCESSOR_DEFINITION);
  }
}
