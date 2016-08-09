/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.transformers.xml.xslt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.xml.transformer.XsltTransformer;
import org.mule.runtime.module.xml.transformers.xml.AbstractXmlTransformerTestCase;
import org.mule.runtime.module.xml.util.XMLTestUtils;
import org.mule.runtime.module.xml.util.XMLUtils;

import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamReader;

import org.junit.Test;

public class XsltTransformerJDKTransformerTestCase extends AbstractXmlTransformerTestCase {

  private String srcData;
  private String resultData;

  @Override
  protected void doSetUp() throws Exception {
    srcData = IOUtils.getResourceAsString("cdcatalog.xml", getClass());
    resultData = IOUtils.getResourceAsString("cdcatalog.html", getClass());
  }

  @Override
  public Transformer getTransformer() throws Exception {
    XsltTransformer transformer = new XsltTransformer();
    transformer.setReturnDataType(DataType.STRING);
    transformer.setXslFile("cdcatalog.xsl");
    transformer.setMaxActiveTransformers(42);
    // Will default to JDK
    transformer.setXslTransformerFactory(null);
    initialiseObject(transformer);
    return transformer;
  }

  @Override
  public Transformer getRoundTripTransformer() throws Exception {
    return null;
  }

  @Override
  @Test
  public void testRoundtripTransform() throws Exception {
    // disable this test
  }

  @Override
  public Object getTestData() {
    Map<String, Serializable> props = new HashMap<>();
    props.put("ListTitle", "MyList");
    props.put("ListRating", new Integer(6));
    return MuleMessage.builder().payload(srcData).inboundProperties(props).build();
  }

  @Override
  public Object getResultData() {
    return resultData;
  }

  @Test
  public void testAllXmlMessageTypes() throws Exception {
    List<?> list = XMLTestUtils.getXmlMessageVariants("cdcatalog.xml");
    Iterator<?> it = list.iterator();

    Object expectedResult = getResultData();
    assertNotNull(expectedResult);

    Object msg, result;
    while (it.hasNext()) {
      msg = it.next();
      result = getTransformer().transform(msg);
      assertNotNull(result);
      assertTrue("Test failed for message type: " + msg.getClass(), compareResults(expectedResult, result));
    }
  }

  @Test
  public void testTransformXMLStreamReader() throws Exception {
    Object expectedResult = getResultData();
    assertNotNull(expectedResult);

    XsltTransformer transformer = (XsltTransformer) getTransformer();

    InputStream is = IOUtils.getResourceAsStream("cdcatalog.xml", XMLTestUtils.class);
    XMLStreamReader sr = XMLUtils.toXMLStreamReader(transformer.getXMLInputFactory(), is);

    Object result = transformer.transform(sr);
    assertNotNull(result);
    assertTrue("expected: " + expectedResult + "\nresult: " + result, compareResults(expectedResult, result));
  }

  @Test
  public void testCustomTransformerFactoryClass() throws InitialisationException {
    XsltTransformer t = new XsltTransformer();
    t.setXslTransformerFactory("com.nosuchclass.TransformerFactory");
    t.setXslFile("cdcatalog.xsl");

    try {
      t.initialise();
      fail("should have failed with ClassNotFoundException");
    } catch (InitialisationException iex) {
      assertEquals(ClassNotFoundException.class, iex.getCause().getClass());
    }

    t = new XsltTransformer();
    t.setXslFile("cdcatalog.xsl");
    // try again with JDK default
    t.setXslTransformerFactory(null);
    t.initialise();
  }

  @Test
  public void testTransformWithStaticParam() throws TransformerException, InitialisationException {

    String xml = "<node1>" + "<subnode1>sub node 1 original value</subnode1>" + "<subnode2>sub node 2 original value</subnode2>"
        + "</node1>";

    String param = "sub node 2 cool new value";

    String expectedTransformedxml =
        "<node1>" + "<subnode1>sub node 1 original value</subnode1>" + "<subnode2>" + param + "</subnode2>" + "</node1>";

    String xsl = "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"2.0\""
        + " xmlns:wsdlsoap=\"http://schemas.xmlsoap.org/wsdl/soap/\"" + " xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\">"
        + "<xsl:param name=\"param1\"/>" + "<xsl:template match=\"@*|node()\">"
        + "<xsl:copy><xsl:apply-templates select=\"@*|node()\"/></xsl:copy>" + "</xsl:template>"
        + "<xsl:template match=\"/node1/subnode2/text()\">" + "<xsl:value-of select=\"$param1\"/>" + "</xsl:template>"
        + "</xsl:stylesheet>";

    XsltTransformer transformer = new XsltTransformer();

    transformer.setMuleContext(muleContext);
    transformer.setReturnDataType(DataType.STRING);
    // set stylesheet
    transformer.setXslt(xsl);

    // set parameter
    Map<String, Object> params = new HashMap<>();
    params.put("param1", param);
    transformer.setContextProperties(params);

    // init transformer
    transformer.initialise();

    // do transformation
    String transformerResult = (String) transformer.transform(xml);

    // remove doc type and CRLFs
    transformerResult = transformerResult.substring(transformerResult.indexOf("?>") + 2);

    assertTrue(transformerResult.indexOf(expectedTransformedxml) > -1);

  }

  @Test
  public void testTransformWithDynamicParam() throws Exception {

    String xml = "<node1>" + "<subnode1>sub node 1 original value</subnode1>" + "<subnode2>sub node 2 original value</subnode2>"
        + "</node1>";

    String param = "sub node 2 cool new value";

    String expectedTransformedxml =
        "<node1>" + "<subnode1>sub node 1 original value</subnode1>" + "<subnode2>" + param + "</subnode2>" + "</node1>";

    String xsl = "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"2.0\""
        + " xmlns:wsdlsoap=\"http://schemas.xmlsoap.org/wsdl/soap/\"" + " xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\">"
        + "<xsl:param name=\"param1\"/>" + "<xsl:template match=\"@*|node()\">"
        + "<xsl:copy><xsl:apply-templates select=\"@*|node()\"/></xsl:copy>" + "</xsl:template>"
        + "<xsl:template match=\"/node1/subnode2/text()\">" + "<xsl:value-of select=\"$param1\"/>" + "</xsl:template>"
        + "</xsl:stylesheet>";

    XsltTransformer transformer = new XsltTransformer();

    transformer.setMuleContext(muleContext);
    transformer.setReturnDataType(DataType.STRING);
    transformer.setMuleContext(muleContext);
    // set stylesheet
    transformer.setXslt(xsl);

    // set parameter
    Map<String, Object> params = new HashMap<>();
    params.put("param1", "#[message.outboundProperties.myproperty]");
    transformer.setContextProperties(params);

    // init transformer
    transformer.initialise();

    MuleMessage message = MuleMessage.builder().payload(xml).addOutboundProperty("myproperty", param).build();
    // do transformation
    String transformerResult = (String) transformer.transform(message);

    // remove doc type and CRLFs
    transformerResult = transformerResult.substring(transformerResult.indexOf("?>") + 2);

    assertTrue(transformerResult.indexOf(expectedTransformedxml) > -1);
  }

}
