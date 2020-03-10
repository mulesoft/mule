/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.test.implicit.config.extension.extension.api.Counter;

import java.util.Map;

import org.junit.Test;

public class AsyncOperationTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "implicit-config-async-operation.xml";
  }

  @Test
  public void simpleAsync() throws Exception {
    String async = (String) flowRunner("simpleAsync").run().getMessage().getPayload().getValue();

    assertThat(async, is(equalTo("async!")));
  }

  @Test
  public void scatteredAsync() throws Exception {
    Map<String, TypedValue<?>> vars = flowRunner("scatterAsync").run().getVariables();

    assertThat(vars.get("route1").getValue(), is(equalTo("async!")));
    assertThat(vars.get("route2").getValue(), is(equalTo("async!")));
  }
}
