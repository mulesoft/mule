/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.stereotypes;

import org.mule.runtime.extension.api.stereotype.StereotypeDefinition;

public class DrugKillingStereotype implements StereotypeDefinition {

  @Override
  public String getName() {
    return "drugKilling";
  }

}
