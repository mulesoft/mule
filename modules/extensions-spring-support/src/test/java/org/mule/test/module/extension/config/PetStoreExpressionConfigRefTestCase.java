/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.PetStoreConnector;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;

public class PetStoreExpressionConfigRefTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "petstore-expression-config-ref.xml";
  }

  @Test
  public void getPetsWithReference() throws Exception {
    ConfigurationInstance config = muleContext.getExtensionManager().getConfiguration("paw-patrol-store", testEvent());
    assertThat(config, is(notNullValue()));

    CoreEvent response = runFlow("getPetsWithReference");
    List<String> pets = (List<String>) response.getMessage().getPayload().getValue();
    PetStoreConnector configValue = (PetStoreConnector) config.getValue();
    assertThat(pets, containsInAnyOrder(configValue.getPets().toArray()));
  }

  @Test
  public void getPetsWithExpression() throws Exception {
    ConfigurationInstance config = muleContext.getExtensionManager().getConfiguration("paw-patrol-store", testEvent());
    assertThat(config, is(notNullValue()));

    CoreEvent response = flowRunner("getPetsWithExpression").withVariable("storeName", "paw-patrol").run();
    List<String> pets = (List<String>) response.getMessage().getPayload().getValue();
    PetStoreConnector configValue = (PetStoreConnector) config.getValue();
    assertThat(pets, containsInAnyOrder(configValue.getPets().toArray()));
  }
}
