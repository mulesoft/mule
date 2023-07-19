/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.test.implicit.config.extension.extension.api.Counter;

import org.junit.Test;

public class ImplicitConnectionProviderTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "implicit-connection-provider.xml";
  }

  @Test
  public void getImplicitConnection() throws Exception {
    Counter connection =
        (Counter) flowRunner("implicitConnection").withVariable("number", 5).run().getMessage().getPayload().getValue();
    assertThat(connection.getValue(), is(5));

    connection = (Counter) flowRunner("implicitConnection").withVariable("number", 10).run().getMessage().getPayload().getValue();
    assertThat(connection.getValue(), is(10));
  }
}
