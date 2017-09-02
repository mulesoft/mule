/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
