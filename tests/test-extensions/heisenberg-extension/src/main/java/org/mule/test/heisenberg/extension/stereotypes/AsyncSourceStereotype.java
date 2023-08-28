/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.stereotypes;


import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.SOURCE_DEFINITION;
import org.mule.runtime.extension.api.stereotype.StereotypeDefinition;

import java.util.Optional;

public class AsyncSourceStereotype implements StereotypeDefinition {

  @Override
  public String getName() {
    return "AsyncSource";
  }

  @Override
  public Optional<StereotypeDefinition> getParent() {
    return Optional.of(SOURCE_DEFINITION);
  }
}
