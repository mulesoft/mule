/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.mule.api.config.MuleProperties;
import org.mule.api.registry.RegistrationException;
import org.mule.api.transformer.TransformerException;
import org.mule.module.xml.transformer.XPathExtractor;
import org.mule.module.xml.util.NamespaceManager;
import org.mule.module.xml.xpath.XPathReturnType;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XPathExtractorTestCase extends AbstractMuleContextTestCase
{
    protected static final String TEST_XML_MULTI_RESULTS = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                                                           + "<root>" + "<node>value1</node>"
                                                           + "<node>value2</node>" + "<node>value3</node>"
                                                           + "</root>";

    protected static final String TEST_XML_MULTI_NESTED_RESULTS = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                                                                  + "<root>"
                                                                  + "<node>"
                                                                  + "<subnode1>val1</subnode1>"
                                                                  + "<subnode2>val2</subnode2>"
                                                                  + "</node>"
                                                                  + "<node>"
                                                                  + "<subnode1>val3</subnode1>"
                                                                  + "<subnode2>val4</subnode2>"
                                                                  + "</node>"
                                                                  + "</root>";

    protected static final String TEST_XML_SINGLE_RESULT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                                                           + "<root>" + "<node>value1</node>"
                                                           + "<node2>2</node2>" + "</root>";

    protected static final String TEST_XML_WITH_NAMESPACES = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root xmlns:f=\"http://www.w3schools.com/furniture\">"
                                                             + "<f:table>"
                                                             + "<f:name>African Coffee Table</f:name>"
                                                             + "<f:width>80</f:width>"
                                                             + "<f:length>120</f:length>"
                                                             + "</f:table>"
                                                             + "</root>";

    @Test(expected=RegistrationException.class)
    public void expressionIsRequired() throws Exception
    {
        createObject(XPathExtractor.class);
    }

    @Test(expected=TransformerException.class)
    public void badExpression() throws Exception
    {
        final String badExpression = "/$@�%$�&�$$�%";
        final XPathExtractor extractor = initialiseExtractor(badExpression, XPathReturnType.STRING);

        final Document doc = getDocumentForString(TEST_XML_SINGLE_RESULT);
        extractor.transform(doc);
    }

    @Test
    public void nodeToStringResult() throws Exception
    {
        final String expression = "/root/node";
        final XPathExtractor extractor = initialiseExtractor(expression, XPathReturnType.STRING);

        // just make code coverage tools happy
        assertEquals("Wrong expression returned.", expression, extractor.getExpression());

        Document doc = getDocumentForString(TEST_XML_SINGLE_RESULT);

        final Object objResult = extractor.transform(doc);
        assertNotNull(objResult);

        final String result = (String)objResult;
        assertEquals("Wrong value extracted.", "value1", result);
    }

    @Test
    public void inputSourceToStringResult() throws Exception
    {
        final String expression = "/root/node";
        final XPathExtractor extractor = initialiseExtractor(expression, XPathReturnType.STRING);

        final Document doc = getDocumentForString(TEST_XML_SINGLE_RESULT);
        final InputSource source = getInputSourceForDocument(doc);

        final Object objResult = extractor.transform(source);
        assertNotNull(objResult);

        final String result = (String)objResult;
        assertEquals("Wrong value extracted.", "value1", result);
    }

    @Test
    public void nodeToNumberResult() throws Exception
    {
        final String expression = "/root/node2";
        final XPathExtractor extractor = initialiseExtractor(expression, XPathReturnType.NUMBER);

        final Document doc = getDocumentForString(TEST_XML_SINGLE_RESULT);

        final Object objResult = extractor.transform(doc);
        assertNotNull(objResult);

        final double result = ((Double) objResult).doubleValue();
        assertEquals("Wrong value extracted.", 2.0, result, 0.0);
    }

    @Test
    public void nodeToBooleanResult() throws Exception
    {
        final String expression = "/root/node2";
        final XPathExtractor extractor = initialiseExtractor(expression, XPathReturnType.BOOLEAN);

        final Document doc = getDocumentForString(TEST_XML_SINGLE_RESULT);

        final Object objResult = extractor.transform(doc);
        assertNotNull(objResult);

        final Boolean result = (Boolean)objResult;
        assertEquals("Wrong value extracted.", Boolean.TRUE, result);
    }

    @Test
    public void nodeToNodeResult() throws Exception
    {
        final String expression = "/root/node2";
        final XPathExtractor extractor = initialiseExtractor(expression, XPathReturnType.NODE);

        final Document doc = getDocumentForString(TEST_XML_SINGLE_RESULT);

        final Object objResult = extractor.transform(doc);
        assertNotNull(objResult);

        final Node result = (Node)objResult;
        assertEquals("Wrong value extracted.", "node2", result.getNodeName());
    }

    @Test
    public void nodeToNodeSetResult() throws Exception
    {
        final String expression = "/root/node2";
        final XPathExtractor extractor = initialiseExtractor(expression, XPathReturnType.NODESET);

        final Document doc = getDocumentForString(TEST_XML_SINGLE_RESULT);

        final Object objResult = extractor.transform(doc);
        assertThat(objResult, instanceOf(NodeList.class));

        NodeList result = (NodeList) objResult;
        assertEquals("Wrong value extracted.", "node2", result.item(0).getNodeName());
    }

    @Test
    public void nodeToStringResultWithNameSpaces() throws Exception
    {
        registerNamespaces();

        final String expression = "//f:width";
        final XPathExtractor extractor = initialiseExtractor(expression, XPathReturnType.STRING);

        // just make code coverage tools happy
        assertEquals("Wrong expression returned.", expression, extractor.getExpression());

        final Document doc = getDocumentForString(TEST_XML_WITH_NAMESPACES);
        final Object objResult = extractor.transform(doc);
        assertNotNull(objResult);

        final String result = (String)objResult;
        assertEquals("Wrong value extracted.", "80", result);
    }

    @Test
    public void xpathNamespacesInitialization() throws Exception
    {
        registerNamespaces();

        final String expression = "//f:width";
        final XPathExtractor extractor = initialiseExtractor(expression, XPathReturnType.STRING);

        final Map<String, String> namespaces = extractor.getXpathEvaluator().getRegisteredNamespaces();
        assertEquals("http://www.w3schools.com/furniture", namespaces.get("f"));
    }

    @Test
    public void namespacesNonOverwritten() throws Exception
    {
        registerNamespaces();

        final Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("g", "http://www.test.com/g");

        final String expression = "//f:width";
        final XPathExtractor extractor = initialiseExtractor(expression, XPathReturnType.STRING);
        extractor.setNamespaces(namespaces);

        assertEquals("http://www.test.com/g", extractor.getNamespaces().get("g"));
    }

    private void registerNamespaces() throws RegistrationException
    {
        final NamespaceManager namespaceManager = new NamespaceManager();
        final Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("f", "http://www.w3schools.com/furniture");
        namespaceManager.setNamespaces(namespaces);
        muleContext.getRegistry().unregisterObject(MuleProperties.OBJECT_MULE_NAMESPACE_MANAGER);
        muleContext.getRegistry().registerObject(MuleProperties.OBJECT_MULE_NAMESPACE_MANAGER,
            namespaceManager);
    }

    private XPathExtractor initialiseExtractor(final String expression, XPathReturnType resultType)
        throws RegistrationException
    {
        final XPathExtractor extractor = new XPathExtractor();
        extractor.setExpression(expression);
        extractor.setResultType(resultType);
        initialiseObject(extractor);
        return extractor;
    }

    private Document getDocumentForString(final String xml) throws Exception
    {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);

        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        return doc;
    }

    private InputSource getInputSourceForDocument(final Document doc) throws Exception
    {
        final DOMSource source = new DOMSource(doc);
        final StringWriter xmlWriter = new StringWriter();
        final StreamResult xmlResult = new StreamResult(xmlWriter);
        TransformerFactory.newInstance().newTransformer().transform(source, xmlResult);
        final StringReader xmlReader = new StringReader(xmlWriter.toString());

        return new InputSource(xmlReader);
    }
}
