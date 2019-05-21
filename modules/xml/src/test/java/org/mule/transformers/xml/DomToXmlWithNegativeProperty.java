/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml;

import static java.lang.Integer.MAX_VALUE;
import static org.dom4j.DocumentHelper.parseText;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.api.config.MuleProperties.MULE_MAX_ATTRIBUTE_SIZE;
import static org.mule.module.xml.util.XMLUtils.toXMLStreamReader;
import static org.mule.util.IOUtils.getResourceAsStream;
import static org.mule.util.IOUtils.getResourceAsString;
import org.mule.module.xml.transformer.XmlToDomDocument;
import org.mule.module.xml.util.XMLTestUtils;
import org.mule.tck.junit4.rule.SystemProperty;

import com.ctc.wstx.stax.WstxInputFactory;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.dom4j.DocumentException;
import org.dom4j.io.DOMWriter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Node;

public class DomToXmlWithNegativeProperty extends AbstractDomXmlTransformersTestCase
{

  @Rule
  public final SystemProperty maxAttributeSizeProperty = new SystemProperty(MULE_MAX_ATTRIBUTE_SIZE, "-500000");

  @Before
  public void doSetUp() throws IOException, DocumentException
  {
    setTestData(getResourceAsString("small.xml", getClass()));
    org.dom4j.Document dom4jDoc = parseText((String) getTestData());
    setResultData(new DOMWriter().write(dom4jDoc));
  }

  @Test
  public void invalidMaxAttributeSizeProperty() throws Exception
  {
    Object expectedResult = getResultData();
    assertThat(expectedResult, is(notNullValue()));

    XmlToDomDocument transformer = (XmlToDomDocument) getTransformer();

    InputStream is = getResourceAsStream("small.xml", XMLTestUtils.class);

    XMLInputFactory xmlInputFactory = transformer.getXMLInputFactory();
    XMLStreamReader sr = toXMLStreamReader(xmlInputFactory, is);

    assertThat(((WstxInputFactory) xmlInputFactory).getConfig().getMaxAttributeSize(), is(MAX_VALUE));

    Object result = transformer.transform(sr);

    writeXml((Node) result);
    assertThat(result, is(notNullValue()));
    assertThat(compareResults(expectedResult, result), is(true));
  }
}
