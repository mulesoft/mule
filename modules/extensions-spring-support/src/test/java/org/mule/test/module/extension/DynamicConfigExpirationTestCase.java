/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.functional.api.flow.FlowRunner;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import org.junit.Test;

public class DynamicConfigExpirationTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "dynamic-config-expiration.xml";
  }

  @Test
  public void expireDynamicConfig() throws Exception {
    final String myName = "Walt";
    FlowRunner runner = flowRunner("dynamic").withPayload(myName);

    final InternalEvent event = runner.buildEvent();
    String returnedName = getPayloadAsString(runner.run().getMessage());

    HeisenbergExtension config =
        (HeisenbergExtension) muleContext.getExtensionManager().getConfiguration("heisenberg", event).getValue();

    // validate we actually hit the correct dynamic config
    assertThat(returnedName, is(myName));
    assertThat(config.getPersonalInfo().getName(), is(myName));

    PollingProber prober = new PollingProber(5000, 1000);
    prober.check(new JUnitLambdaProbe(() -> {
      assertThat(config.getStop(), is(1));
      assertThat(config.getDispose(), is(1));
      return true;
    }, "config was not stopped or disposed"));

    assertThat(config.getInitialise(), is(1));
    assertThat(config.getStart(), is(1));
  }
}
