/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.runtime;

import static java.lang.Thread.currentThread;
import static org.mule.services.soap.api.client.SoapClientConfiguration.builder;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.services.soap.AbstractSoapServiceTestCase;
import org.mule.services.soap.api.client.SoapClientFactory;
import org.mule.services.soap.SoapServiceImplementation;

import java.net.URL;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Web Service Consumer")
@Stories("Connection")
public class SoapClientFactoryTestCase extends AbstractSoapServiceTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private SoapClientFactory factory = new SoapServiceImplementation(null).getClientFactory();

  @Test
  @Description("Tries to instantiate a connection with an RPC WSDL and fails.")
  public void rpcWsdlFails() throws Exception {
    expectedException.expect(ConnectionException.class);
    expectedException.expectMessage("RPC WSDLs are not supported");
    URL resource = currentThread().getContextClassLoader().getResource("wsdl/rpc.wsdl");
    factory.create(builder()
        .withPort("SoapResponderPortType")
        .withService("SoapResponder")
        .withWsdlLocation(resource.getPath())
        .build());
  }
}
