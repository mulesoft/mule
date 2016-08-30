/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.transformers.xml.xslt;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.util.IOUtils;

import org.custommonkey.xmlunit.XMLUnit;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class Xslt3TestCase extends FunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "xsl/xslt3-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    XMLUnit.setIgnoreWhitespace(true);
  }

  @Test
  public void groupByCities() throws Exception {
    String cities = IOUtils.getResourceAsString("cities.xml", getClass());
    String output = getPayloadAsString(flowRunner("groupCities").withPayload(cities).run().getMessage());
    String expected = IOUtils.getResourceAsString("transformed-cities.xml", getClass());
    assertThat(XMLUnit.compareXML(expected, output).similar(), is(true));
  }

  @Test
  public void booksAsCsv() throws Exception {
    String books = IOUtils.getResourceAsString("books.xml", getClass());
    String output = getPayloadAsString(flowRunner("booksAsCsv").withPayload(books).run().getMessage());

    final String expected =
        "Title,Author,Category,Stock-Value\n" + "\"Pride and Prejudice\",\"Jane Austen\",\"MMP(Unclassified)\",\"N/A\"\n"
            + "\"Wuthering Heights\",\"Charlotte Bronte\",\"P(Unclassified)\",\"N/A\"\n"
            + "\"Tess of the d'Urbervilles\",\"Thomas Hardy\",\"P(Unclassified)\",\"N/A\"\n"
            + "\"Jude the Obscure\",\"Thomas Hardy\",\"P(Unclassified)\",\"N/A\"\n"
            + "\"The Big Over Easy\",\"Jasper Fforde\",\"H(Unclassified)\",\"N/A\"\n"
            + "\"The Eyre Affair\",\"Jasper Fforde\",\"P(Unclassified)\",\"N/A\"";

    assertThat(output.trim(), equalTo(expected));
  }

  @Test
  public void multipleInputs() throws Exception {
    String cities = IOUtils.getResourceAsString("cities.xml", getClass());
    String response = getPayloadAsString(flowRunner("multipleInputs").withPayload(cities).run().getMessage());

    assertThat(response, containsString("<cities>"));
    assertThat(response, containsString("<BOOKS>"));
  }

  @Test
  public void nullParameter() throws Exception {
    MessagingException e = flowRunner("nullParam").withPayload("<parameter/>").runExpectingException();
    assertThat(e.getMessage(), CoreMatchers.containsString("null"));
  }
}
