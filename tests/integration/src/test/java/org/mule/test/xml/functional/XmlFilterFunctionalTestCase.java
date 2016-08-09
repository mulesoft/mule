/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.xml.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.runtime.core.api.MuleMessage;

import java.util.Random;

import org.junit.Test;

public class XmlFilterFunctionalTestCase extends AbstractXmlFunctionalTestCase {

  public static final int MAX_COUNT = 100;
  public static final String STRING_MESSAGE = "Hello world";

  @Override
  protected String getConfigFile() {
    return "org/mule/module/xml/xml-filter-functional-test-flow.xml";
  }

  @Test
  public void testNotXml() throws Exception {
    logger.debug("not xml");
    MuleMessage response = flowRunner("test for xml").withPayload(STRING_MESSAGE).run().getMessage();

    assertNotNull(response);
    assertNotNull(response.getPayload());
    assertEquals(STRING_MESSAGE, getPayloadAsString(response));
  }

  @Test
  public void testOther() throws Exception {
    logger.debug("other");
    doTestXml("other", getResourceAsString("org/mule/issues/many-sends-mule-1758-test-service.xml"));
  }

  @Test
  public void testSelf() throws Exception {
    logger.debug("self");
    doTestXml("self", getConfigAsString());
  }

  public void doTestXml(String endpoint, String xml) throws Exception {
    MuleMessage response = flowRunner("test for xml").withPayload(xml).run().getMessage();
    assertNotNull(response);
    assertNotNull(response.getPayload());
    assertEquals(xml, getPayloadAsString(response));
  }

  @Test
  public void testMany() throws Exception {
    Random random = new Random();
    for (int i = 0; i < MAX_COUNT; ++i) {
      switch (random.nextInt(3)) {
        case 0:
          testNotXml();
          break;
        case 1:
          testOther();
          break;
        case 2:
          testSelf();
          break;
        default:
          throw new IllegalStateException("Bad case");
      }
    }
  }
}
