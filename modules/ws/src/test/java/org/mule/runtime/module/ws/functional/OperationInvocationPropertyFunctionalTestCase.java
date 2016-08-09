/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.ws.functional;

import static junit.framework.Assert.assertNull;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.module.cxf.CxfConstants;

import org.junit.Test;

/**
 * This test verifies that WS consumer doesn't fail if the message contains an invocation property called "operation". This
 * variable is used by CXF proxy client to allow the user to explicitly set the operation, and as WS consumer uses CXF proxy
 * client if the variable is defined it may change its behavior and make it fail.
 */
public class OperationInvocationPropertyFunctionalTestCase extends AbstractWSConsumerFunctionalTestCase {

  private static final String OPERATION_VALUE = "INVALID";

  @Override
  protected String getConfigFile() {
    return "operation-invocation-property-config.xml";
  }

  @Test
  public void consumerWorksWithOperationInvocationPropertyDefined() throws Exception {
    MuleEvent event =
        flowRunner("echo").withPayload(ECHO_REQUEST).withFlowVariable(CxfConstants.OPERATION, OPERATION_VALUE).run();

    assertXMLEqual(EXPECTED_ECHO_RESPONSE, getPayloadAsString(event.getMessage()));
    assertThat(event.getFlowVariable(CxfConstants.OPERATION), is(OPERATION_VALUE));
  }


  @Test
  public void consumerWorksWithNoOperationInvocationPropertyDefined() throws Exception {
    MuleEvent event = flowRunner("echo").withPayload(ECHO_REQUEST).run();
    assertXMLEqual(EXPECTED_ECHO_RESPONSE, getPayloadAsString(event.getMessage()));

    assertNull(event.getFlowVariable(CxfConstants.OPERATION));
  }
}
