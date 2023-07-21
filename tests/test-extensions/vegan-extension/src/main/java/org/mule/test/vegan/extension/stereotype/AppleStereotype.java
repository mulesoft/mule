/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.vegan.extension.stereotype;

import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CONFIG_DEFINITION;
import org.mule.runtime.extension.api.stereotype.StereotypeDefinition;

import java.util.Optional;

public class AppleStereotype implements StereotypeDefinition {

  @Override
  public String getName() {
    return "APPLE_CONFIG";
  }

  @Override
  public Optional<StereotypeDefinition> getParent() {
    return Optional.of(CONFIG_DEFINITION);
  }
}
