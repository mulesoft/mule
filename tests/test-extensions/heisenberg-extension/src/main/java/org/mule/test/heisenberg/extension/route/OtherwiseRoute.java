/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension.route;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.stereotype.AllowedStereotypes;
import org.mule.runtime.extension.api.runtime.route.Route;
import org.mule.test.heisenberg.extension.stereotypes.DrugKillingStereotype;

@AllowedStereotypes(DrugKillingStereotype.class)
public class OtherwiseRoute extends Route {

  @Parameter
  @Optional
  private boolean execute;

  public boolean shouldExecute() {
    return execute;
  }

}
