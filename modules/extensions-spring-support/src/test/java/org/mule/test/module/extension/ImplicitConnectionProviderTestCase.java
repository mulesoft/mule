/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.test.implicit.config.extension.extension.Counter;

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
