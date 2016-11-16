/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws;

import static org.mule.extension.ws.WscTestUtils.HEADER_IN;
import static org.mule.extension.ws.WscTestUtils.HEADER_INOUT;
import static org.mule.extension.ws.WscTestUtils.getRequestResource;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.message.Message;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.ClassRule;

public abstract class AbstractSoapServiceTestCase extends MuleArtifactFunctionalTestCase {

  @ClassRule
  public static DynamicPort servicePort = new DynamicPort("servicePort");

  @ClassRule
  public static WebServiceRule service = new WebServiceRule(servicePort.getValue());

  protected Message runFlowWithRequest(String flowName, String requestXmlResourceName) throws Exception {
    return flowRunner(flowName)
        .withPayload(getRequestResource(requestXmlResourceName))
        .withVariable(HEADER_IN, getRequestResource(HEADER_IN))
        .withVariable(HEADER_INOUT, getRequestResource(HEADER_INOUT))
        .run()
        .getMessage();
  }
}
