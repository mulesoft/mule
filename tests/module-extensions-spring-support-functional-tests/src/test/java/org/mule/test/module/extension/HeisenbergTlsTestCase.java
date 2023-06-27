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
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.test.heisenberg.extension.HeisenbergConnection;

import org.junit.Test;

public class HeisenbergTlsTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "heisenberg-tls-connection-config.xml";
  }

  @Test
  public void globalTls() throws Exception {
    assertSecureConnection("getGlobalTls");
  }

  @Test
  public void inlineTls() throws Exception {
    assertSecureConnection("getInlineTls");
  }

  private void assertSecureConnection(String flowName) throws Exception {
    HeisenbergConnection connection = (HeisenbergConnection) flowRunner(flowName).run().getMessage().getPayload().getValue();
    assertThat(connection.getTlsContextFactory(), is(notNullValue()));
  }
}
