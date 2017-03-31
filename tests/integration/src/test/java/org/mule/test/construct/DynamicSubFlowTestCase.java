/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class DynamicSubFlowTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "dynamic-subflow-test-config.xml";
  }

  @Test
  public void testCofiguration() throws Exception {
    final Event muleEvent = flowRunner("ApplicationFlow").withPayload("").run();
    Message result = muleEvent.getMessage();
    assertThat(result, is(notNullValue()));
    assertThat(muleEvent.getError().isPresent(), is(false));
    assertThat(result.getPayload().getValue(), is(notNullValue()));
  }
}
