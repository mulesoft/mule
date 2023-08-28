/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
