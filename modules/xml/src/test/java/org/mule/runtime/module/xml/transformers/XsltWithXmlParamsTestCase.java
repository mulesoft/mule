/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.transformers;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.module.xml.util.XMLTestUtils;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;

public class XsltWithXmlParamsTestCase extends FunctionalTestCase {

  private static final String EXPECTED =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?><result><body><just>testing</just></body><fromParam>value element</fromParam></result>";

  @Override
  protected String getConfigFile() {
    return "xslt-with-xml-param-config.xml";
  }

  @Test
  public void xmlSourceParam() throws Exception {
    MuleEvent event = flowRunner("xmlSourceParam").withPayload(XMLTestUtils.toSource("simple.xml"))
        .withFlowVariable("xml", XMLTestUtils.toSource("test.xml")).run();

    assertExpected(event);
  }

  @Test
  public void xmlStringParam() throws Exception {
    MuleEvent event = flowRunner("xmlStringParam").withPayload(XMLTestUtils.toSource("simple.xml"))
        .withFlowVariable("xml", XMLTestUtils.toSource("test.xml")).run();

    assertExpected(event);
  }

  private void assertExpected(MuleEvent event) throws Exception {
    assertThat(XMLUnit.compareXML(event.getMessage().getPayload().toString(), EXPECTED).similar(), is(true));
  }


}
