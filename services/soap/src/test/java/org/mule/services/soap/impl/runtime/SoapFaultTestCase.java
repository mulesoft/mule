/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.impl.runtime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.mule.services.soap.api.SoapVersion.SOAP11;
import static org.mule.services.soap.api.message.SoapRequest.builder;
import org.mule.services.soap.impl.AbstractSoapServiceTestCase;
import org.mule.services.soap.impl.exception.BadRequestException;
import org.mule.services.soap.impl.exception.SoapFaultException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Web Service Consumer")
@Stories({"Operation Execution", "Soap Fault"})
public class SoapFaultTestCase extends AbstractSoapServiceTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  @Description("Consumes an operation that throws a SOAP Fault and expects a Soap Fault Exception")
  public void failOperation() throws Exception {
    String req =
        "<con:fail xmlns:con=\"http://service.impl.soap.services.mule.org/\">"
            + "    <text>Fail Message</text>"
            + "</con:fail>";

    try {
      client.consume(builder().withContent(req).withOperation("fail").build());
    } catch (SoapFaultException e) {
      // Server is for 1.1, Receiver for 1.2
      assertThat(e.getFaultCode().getLocalPart(), isOneOf("Server", "Receiver"));
      assertThat(e.getReason(), is("Fail Message"));
      assertThat(e.getDetail(), containsString("EchoException"));
      assertThat(e.getDetail(), containsString("Fail Message"));
    }
  }

  @Test
  @Description("Consumes an operation that does not exist and throws a SOAP Fault because of it and asserts the thrown exception")
  public void noExistentOperation() throws Exception {
    String badRequest = "<con:noOperation xmlns:con=\"http://service.ws.extension.mule.org/\"/>";
    try {
      client.consume(builder().withContent(badRequest).withOperation("fail").build());
    } catch (SoapFaultException e) {
      // Client is for 1.1, Sender for 1.2
      assertThat(e.getFaultCode().getLocalPart(), isOneOf("Client", "Sender"));
      if (soapVersion.equals(SOAP11)) {
        assertThat(e.getReason(), containsString("{http://service.ws.extension.mule.org/}noOperation was not recognized"));
      } else {
        assertThat(e.getReason(),
                   containsString("Unexpected wrapper element {http://service.ws.extension.mule.org/}noOperation found."));
      }
    }
  }

  @Test
  @Description("Consumes an operation with a body that is not a valid XML")
  public void echoBodyIsNotValidXml() throws Exception {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Error consuming the operation [echo], the request body is not a valid XML");
    client.consume(builder().withOperation("echo").withContent("Invalid Test Payload: this is not an XML").build());
  }
}
