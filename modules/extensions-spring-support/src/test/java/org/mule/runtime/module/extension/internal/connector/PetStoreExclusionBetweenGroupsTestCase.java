/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.ExclusiveCashier;
import org.mule.test.petstore.extension.PetStoreConnector;

import org.junit.Test;

public class PetStoreExclusionBetweenGroupsTestCase extends ExtensionFunctionalTestCase {

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {PetStoreConnector.class};
  }

  @Override
  protected String getConfigFile() {
    return "petstore-exclusion-between-groups-config.xml";
  }

  @Test
  public void ExclusiveGroupWithNestedParameters() throws Exception {
    ExclusiveCashier cashier = flowRunner("getCashierNestedParams").run().getMessage().getPayload();
    assertThat(cashier.getAccount().getCash(), is(100));
    assertThat(cashier.getAccount().getDebt(), is(-100));
  }
}
