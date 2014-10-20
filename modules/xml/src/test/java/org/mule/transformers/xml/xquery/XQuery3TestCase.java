/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml.xquery;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;

import net.sf.saxon.dom.DocumentBuilderImpl;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XQuery3TestCase extends FunctionalTestCase
{

    private String input;

    @Override
    protected String getConfigFile()
    {
        return "xquery/xquery3-config.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        XMLUnit.setIgnoreWhitespace(true);
        input = IOUtils.getResourceAsString("cd-catalog.xml", getClass());
    }

    @Test
    public void tryCatch() throws Exception
    {
        List<Element> elements = (List<Element>) runFlow("tryCatch", input).getMessage().getPayload();
        assertThat(elements, hasSize(1));
        assertThat(elements.get(0).getTagName(), equalTo("error"));
        assertThat(elements.get(0).getTextContent(), containsString("Caught error"));
    }

    @Test
    public void switchStatement() throws Exception
    {
        List<Element> elements = (List<Element>) runFlow("switch", input).getMessage().getPayload();

        assertThat(elements, hasSize(1));
        assertThat(elements.get(0).getTagName(), equalTo("Quack"));
    }

    @Test
    public void groupBy() throws Exception
    {
        List<Element> elements = (List<Element>) runFlow("groupBy", input).getMessage().getPayload();

        assertThat(elements, hasSize(2));
        assertThat(elements.get(0).getTagName(), equalTo("odd"));
        assertThat(elements.get(1).getTagName(), equalTo("even"));
        assertThat(elements.get(0).getTextContent(), equalTo("1 3 5 7 9"));
        assertThat(elements.get(1).getTextContent(), equalTo("2 4 6 8 10"));
    }

    @Test
    public void books() throws Exception
    {
        List<Node> nodes = (List<Node>) runFlow("books", getBooks()).getMessage().getPayload();
        assertThat(nodes, hasSize(6));
        assertThat(nodes.get(0).getLastChild().getTextContent(), equalTo("The Eyre Affair"));
    }

    @Test
    public void multipleInputsByPath() throws Exception
    {
        URL booksUrl = IOUtils.getResourceAsUrl("books.xml", getClass());
        URL citiesURL = IOUtils.getResourceAsUrl("cities.xml", getClass());

        assertMultipleInputs("multipleInputsByPath", booksUrl.getPath(), citiesURL.getPath());
    }

    @Test
    public void multipleInputsByParam() throws Exception
    {
        try (InputStream books = IOUtils.getResourceAsStream("books.xml", getClass());
             InputStream cities = IOUtils.getResourceAsStream("cities.xml", getClass()))
        {

            DocumentBuilder documentBuilder = new DocumentBuilderImpl();
            Document booksDocument = documentBuilder.parse(books);
            Document citiesDocument = documentBuilder.parse(cities);

            // test both parameters as a document or as a node
            assertMultipleInputs("multipleInputsByParam", booksDocument, citiesDocument.getFirstChild());
        }
    }

    private void assertMultipleInputs(String flowName, Object books, Object cities) throws Exception
    {
        MuleEvent event = getTestEvent(input);
        event.setFlowVariable("books", books);
        event.setFlowVariable("cities", cities);

        Flow flow = (Flow) getFlowConstruct(flowName);
        List<Element> elements = (List<Element>) flow.process(event).getMessage().getPayload();
        assertThat(elements, hasSize(1));

        NodeList childNodes = elements.get(0).getChildNodes();
        assertThat(childNodes.getLength(), greaterThan(0));

        NamedNodeMap firstChildAttributes = childNodes.item(0).getAttributes();
        assertThat(firstChildAttributes.getNamedItem("title").getNodeValue(), equalTo("Pride and Prejudice"));
        assertThat(firstChildAttributes.getNamedItem("city").getNodeValue(), equalTo("milan"));
    }

    private String getBooks() throws IOException
    {
        return IOUtils.getResourceAsString("books.xml", getClass());
    }
}
