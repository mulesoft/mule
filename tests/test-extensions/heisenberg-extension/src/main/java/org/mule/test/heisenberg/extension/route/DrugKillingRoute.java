/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.route;

import org.mule.runtime.extension.api.annotation.param.stereotype.AllowedStereotypes;
import org.mule.test.heisenberg.extension.stereotypes.DrugKillingStereotype;

@AllowedStereotypes(DrugKillingStereotype.class)
public class DrugKillingRoute extends OtherwiseRoute {
}
