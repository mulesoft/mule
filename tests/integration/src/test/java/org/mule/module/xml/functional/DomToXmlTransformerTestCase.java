/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.functional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.cxf.ProxyRPCBindingTestCase.HTTP_REQUEST_OPTIONS;
import static org.mule.util.IOUtils.getResourceAsString;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class DomToXmlTransformerTestCase extends FunctionalTestCase
{

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile()
  {
    return "org/mule/module/xml/dom-xml-transformer.xml";
  }

  @Test
  public void transformDomDocumentToXml() throws Exception
  {
    String msg = getResourceAsString("org/mule/module/xml/large.xml", getClass());

    MuleClient client = muleContext.getClient();
    MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber(), getTestMuleMessage(msg), HTTP_REQUEST_OPTIONS);

    String resString = result.getPayloadAsString();

    assertThat(resString, is("OK"));
  }

}
