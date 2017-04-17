/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;


import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.isNull;
import static org.mule.runtime.config.spring.XmlConfigurationDocumentLoader.schemaValidatingDocumentLoader;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import org.hamcrest.core.StringContains;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XmlConfigurationDocumentLoaderTestCase extends AbstractMuleTestCase {

  /**
   * The following {@link #LINE_NUMBER_ERROR} and {@link #COLUMN_NUMBER_ERROR} are directly related to the mule-config-malformed.xml
   * document. Any change in that file will directly impact these tests as we assert on the file to be sure the errors
   * reading is properly done.
   */
  private static final int LINE_NUMBER_ERROR = 6;
  private static final int COLUMN_NUMBER_ERROR = 12;

  @Test
  public void testWellformedXml() {
    final Document document = getDocument("mule-config.xml");
    assertThat(document, not(isNull()));
    assertThat(document.getDocumentElement().getNodeName(), is("mule"));
    assertThat(document.getDocumentElement().getChildNodes().getLength(), is(3));

    assertThat(document.getDocumentElement().getChildNodes().item(1).getNodeName(), is("flow"));
    assertThat(document.getDocumentElement().getChildNodes().item(1).getAttributes().getNamedItem("name").getNodeValue(),
               is("service"));
  }

  @Test
  public void testMalformedXmlDefaultConstructor() throws XPathExpressionException {
    try {
      getDocument("mule-config-malformed.xml");
      fail("Should not have reach here as the document is malformed and an exception should have been thrown using the default constructor");
    } catch (MuleRuntimeException e) {
      // We want to be sure that the line and column are properly picked up
      assertThat(e.getMessage(),
                 StringContains.containsString("lineNumber: " + LINE_NUMBER_ERROR + "; columnNumber: " + COLUMN_NUMBER_ERROR));
    }
  }

  @Test
  public void testMalformedXmlCustomGatherer() throws XPathExpressionException {
    // We will use a custom gathered that stores the errors but returns an empty list when asked
    final XmlGathererErrorHandlerTest xmlGathererErrorHandlerTest = new XmlGathererErrorHandlerTest();
    final XmlConfigurationDocumentLoader xmlConfigurationDocumentLoader =
        schemaValidatingDocumentLoader(() -> xmlGathererErrorHandlerTest);

    // Validates that the DOM was properly parsed even when it was non-XSD valid
    final Document document = getDocument("mule-config-malformed.xml", xmlConfigurationDocumentLoader);
    assertThat(document, not(isNull()));
    assertThat(document.getDocumentElement().getNodeName(), is("mule"));
    assertThat(document.getDocumentElement().getChildNodes().getLength(), is(3));

    final Node flow = document.getDocumentElement().getChildNodes().item(1);
    assertThat(flow.getNodeName(), is("flow"));
    assertThat(flow.getAttributes().getNamedItem("name").getNodeValue(), is("missing-inner-element"));

    // Asserts over the gathered errors
    assertThat(xmlGathererErrorHandlerTest.errors.size(), is(1));
    assertThat(xmlGathererErrorHandlerTest.errors.get(0).getColumnNumber(), is(COLUMN_NUMBER_ERROR));
    assertThat(xmlGathererErrorHandlerTest.errors.get(0).getLineNumber(), is(LINE_NUMBER_ERROR));
  }

  private Document getDocument(String filename) {
    final XmlConfigurationDocumentLoader xmlConfigurationDocumentLoader = schemaValidatingDocumentLoader();
    return getDocument(filename, xmlConfigurationDocumentLoader);
  }

  private Document getDocument(String filename, XmlConfigurationDocumentLoader xmlConfigurationDocumentLoader) {
    final InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
    return xmlConfigurationDocumentLoader.loadDocument(filename, inputStream);
  }

  /**
   * Custom implementation to count errors for the current XSD validation for testing purposes only
   */
  private class XmlGathererErrorHandlerTest implements XmlGathererErrorHandler {

    private final List<SAXParseException> errors = new ArrayList<>();

    @Override
    public void warning(SAXParseException exception) throws SAXException {
      throw new UnsupportedOperationException("Current tests only work with ERRORs (found a warning)");
    }

    @Override
    public void error(SAXParseException exception) throws SAXException {
      errors.add(exception);
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
      throw new UnsupportedOperationException("Current tests only work with ERRORs (found a fatal)");
    }

    /**
     * @return {@link Collections#emptyList()} always. We want to make the
     * {@link XmlConfigurationDocumentLoader#loadDocument(java.lang.String, java.io.InputStream)}
     * method believe there was no errors while working with the current file.
     */
    @Override
    public List<SAXParseException> getErrors() {
      return emptyList();
    }
  }
}
