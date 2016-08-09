/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.transformers.xml;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.xml.transformer.XsltTransformer;
import org.mule.runtime.module.xml.util.XMLTestUtils;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;

public class InlineXsltTransformerTestCase extends AbstractXmlTransformerTestCase {

  private String srcData;
  private String resultData;

  @Override
  protected void doSetUp() throws Exception {
    srcData = IOUtils.getResourceAsString("simple.xml", getClass());
    resultData = IOUtils.getResourceAsString("simple-out.xml", getClass());
  }

  @Override
  public Transformer getTransformer() throws Exception {
    XsltTransformer transformer = new XsltTransformer();
    transformer
        .setXslt("<?xml version='1.0'?>\n" + "<xsl:stylesheet version='2.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>\n"
            + "<xsl:output method='xml'/>\n" + "<xsl:template match='/'>\n" + "  <some-xml>\n" + "    <xsl:copy-of select='.'/>\n"
            + "  </some-xml>\n" + "</xsl:template>\n" + "</xsl:stylesheet>");
    transformer.setReturnDataType(DataType.STRING);
    transformer.setMuleContext(muleContext);
    transformer.initialise();
    return transformer;
  }

  @Override
  public Transformer getRoundTripTransformer() throws Exception {
    return null;
  }

  @Override
  public void testRoundtripTransform() throws Exception {
    // disable this test
  }

  @Override
  public Object getTestData() {
    return srcData;
  }

  @Override
  public Object getResultData() {
    return resultData;
  }

  @Test
  public void testAllXmlMessageTypes() throws Exception {
    List list = XMLTestUtils.getXmlMessageVariants("simple.xml");
    Iterator it = list.iterator();

    Object expectedResult = getResultData();
    assertNotNull(expectedResult);

    Object msg, result;
    while (it.hasNext()) {
      msg = it.next();
      // TODO Not working for XMLStreamReader
      if (!(msg instanceof javax.xml.stream.XMLStreamReader)) {
        result = getTransformer().transform(msg);
        assertNotNull(result);
        assertTrue("Test failed for message type: " + msg.getClass(), compareResults(expectedResult, result));
      }
    }
  }
}
