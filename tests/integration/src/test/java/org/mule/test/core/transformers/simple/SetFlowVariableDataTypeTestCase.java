/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.core.transformers.simple;

import static java.nio.charset.StandardCharsets.UTF_16;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.lifecycle.Callable;

import org.junit.Test;

public class SetFlowVariableDataTypeTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "set-flow-variable-data-type-config.xml";
  }

  @Test
  public void setsPropertyDataType() throws Exception {
    final MuleEvent muleEvent = flowRunner("main").withPayload(TEST_MESSAGE).run();

    MuleMessage response = muleEvent.getMessage();
    DataType dataType = (DataType) response.getPayload();
    assertThat(dataType, like(String.class, MediaType.XML, UTF_16));
  }

  public static class FlowVariableDataTypeAccessor implements Callable {

    @Override
    public Object onCall(MuleEventContext eventContext) throws Exception {
      return eventContext.getEvent().getFlowVariableDataType("testVariable");
    }
  }
}
