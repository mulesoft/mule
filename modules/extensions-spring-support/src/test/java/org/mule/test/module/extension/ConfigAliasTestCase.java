/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.tck.testmodels.fruit.Banana;

import org.junit.Test;

public class ConfigAliasTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "vegan-config-alias.xml";
  }

  @Test
  public void parseConnectionProviderWithAlias() throws Exception {
    Banana connection = (Banana) runFlow("alias").getMessage().getPayload().getValue();
    assertThat(connection, is(notNullValue()));
    assertThat(connection.getOrigin(), is("Brazil"));
  }
}
