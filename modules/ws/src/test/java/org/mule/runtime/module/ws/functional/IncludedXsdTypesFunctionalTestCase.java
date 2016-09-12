/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.ws.functional;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import org.mule.runtime.core.api.Event;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

public class IncludedXsdTypesFunctionalTestCase extends AbstractWSConsumerFunctionalTestCase {

  @Rule
  public SystemProperty wsdlLocation = new SystemProperty("wsdlLocation", "TestIncludedTypes.wsdl");

  @Override
  protected String getConfigFile() {
    return "included-wsdl-types-config.xml";
  }

  /**
   * Verifies that a no parameter operation is successful having a WSDL definition file that imports the types from another WSDL
   * file, which includes the schema from yet another XSD file. If the metadata cannot be generated then the payload would be used
   * as SOAP body and the test would fail.
   * 
   * @throws Exception
   */
  @Test
  public void metadataIsGeneratedAndPayloadIsIgnored() throws Exception {
    Event event = flowRunner("noParams").withPayload(TEST_MESSAGE).run();

    String expectedResponse = "<ns:noParamsResponse xmlns:ns=\"http://consumer.ws.module.runtime.mule.org/\">"
        + "<text>TEST</text></ns:noParamsResponse>";
    assertXMLEqual(expectedResponse, getPayloadAsString(event.getMessage()));
  }
}
