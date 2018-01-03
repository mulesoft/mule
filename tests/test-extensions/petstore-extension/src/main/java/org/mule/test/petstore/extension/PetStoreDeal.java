/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

public class PetStoreDeal {

  @ParameterGroup(name = "cashier")
  private ExclusiveCashier cashier;

  @Parameter
  @Optional
  private String petFood;

  @Parameter
  private int dealPrice;

  public int getDealPrice() {
    return dealPrice;
  }

  public ExclusiveCashier getCashier() {
    return cashier;
  }

  public String getPetFood() {
    return petFood;
  }

}
