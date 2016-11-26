/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws;

import static java.util.Arrays.asList;
import static org.mule.extension.ws.WscTestUtils.HEADER_IN;
import static org.mule.extension.ws.WscTestUtils.HEADER_INOUT;
import static org.mule.extension.ws.WscTestUtils.getRequestResource;
import org.mule.extension.ws.consumer.Simple11Service;
import org.mule.extension.ws.consumer.Simple12Service;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.message.Message;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import org.junit.ClassRule;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public abstract class AbstractSoapServiceTestCase extends MuleArtifactFunctionalTestCase {

  private static final String SERVICE_11 = "soapService11";
  private static final String SERVICE_12 = "soapService12";

  @ClassRule
  public static DynamicPort servicePort = new DynamicPort("servicePort");

  @ClassRule
  public static WebServiceRule service11 = new WebServiceRule(servicePort.getValue(), SERVICE_11, new Simple11Service());

  @ClassRule
  public static WebServiceRule service12 = new WebServiceRule(servicePort.getValue(), SERVICE_12, new Simple12Service());

  @Parameterized.Parameter
  public String service;

  @Parameterized.Parameter(1)
  public String soapVersion;

  @Parameterized.Parameters(name = "{1}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {SERVICE_11, "SOAP11"},
        {SERVICE_12, "SOAP12"}
    });
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    System.setProperty("servicePath", service);
    System.setProperty("soapVersion", soapVersion);
  }

  protected Message runFlowWithRequest(String flowName, String requestXmlResourceName) throws Exception {
    return flowRunner(flowName)
        .withPayload(getRequestResource(requestXmlResourceName))
        .withVariable(HEADER_IN, getRequestResource(HEADER_IN))
        .withVariable(HEADER_INOUT, getRequestResource(HEADER_INOUT))
        .run()
        .getMessage();
  }
}
