/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws;

import static org.custommonkey.xmlunit.XMLUnit.compareXML;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.custommonkey.xmlunit.Diff;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class WscTestUtils {

  public static final String ECHO = "echo";
  public static final String ECHO_HEADERS = "echoWithHeaders";
  public static final String ECHO_ACCOUNT = "echoAccount";
  public static final String NO_PARAMS = "noParams";
  public static final String NO_PARAMS_HEADER = "noParamsWithHeader";
  public static final String FAIL = "fail";

  public static final String XML = ".xml";
  public static final String ECHO_XML = ECHO + XML;
  public static final String ECHO_HEADERS_XML = ECHO_HEADERS + XML;
  public static final String ECHO_ACCOUNT_XML = ECHO_ACCOUNT + XML;
  public static final String NO_PARAMS_XML = NO_PARAMS + XML;
  public static final String NO_PARAMS_HEADER_XML = NO_PARAMS_HEADER + XML;
  public static final String FAIL_XML = FAIL + XML;

  public static final String HEADER_INOUT = "headerInOut";
  public static final String HEADER_IN = "headerIn";
  public static final String HEADER_OUT = "headerOut";

  public static final String HEADER_INOUT_XML = HEADER_INOUT + XML;
  public static final String HEADER_IN_XML = HEADER_IN + XML;
  public static final String HEADER_OUT_XML = HEADER_OUT + XML;

  public static final String[] OPERATIONS = {ECHO, ECHO_ACCOUNT, ECHO_HEADERS, FAIL, NO_PARAMS_HEADER, NO_PARAMS};

  public static String resourceAsString(final String resource) throws XMLStreamException, IOException {
    final InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
    StringWriter writer = new StringWriter();
    IOUtils.copy(is, writer);
    return writer.toString();
  }

  public static void assertSimilarXml(String expected, String result) throws Exception {
    Diff diff = compareXML(result, expected);
    if (!diff.similar()) {
      System.out.println("Expected xml is: \n");
      System.out.println(prettyPrint(expected));
      System.out.println("\n\n\n But got: \n");
      System.out.println(prettyPrint(result));
    }
    assertThat(diff.similar(), is(true));
  }

  private static String prettyPrint(String a)
      throws TransformerException, ParserConfigurationException, IOException, SAXException {
    DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    InputSource is = new InputSource();
    is.setCharacterStream(new StringReader(a));
    Document doc = db.parse(is);
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    //initialize StreamResult with File object to save to file
    StreamResult result = new StreamResult(new StringWriter());
    DOMSource source = new DOMSource(doc);
    transformer.transform(source, result);
    return result.getWriter().toString();
  }
}
