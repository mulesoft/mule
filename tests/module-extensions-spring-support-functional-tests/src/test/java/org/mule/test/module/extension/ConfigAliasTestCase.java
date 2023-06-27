/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
