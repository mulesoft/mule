/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.expression;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.IOUtils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.dom4j.dom.DOMDocument;
import org.dom4j.tree.DefaultDocument;
import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jaxen.dom.DOMXPath;
import org.jaxen.dom4j.Dom4jXPath;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

@SmallTest
public class XPathExpressionEvaluatorTestCase extends AbstractMuleContextTestCase
{
    private static final String EXPRESSION = "//isTest[test() = 'true']";
    private static final String OTHER_EXPRESSION = "//isNotTest[test = 'false']";

    @Test
    public void testXPathCache() throws JaxenException
    {
        XPathExpressionEvaluator xPathExpressionEvaluator = new XPathExpressionEvaluator();
        XPath xPathDOM1 = xPathExpressionEvaluator.getXPath(EXPRESSION, new DOMDocument());
        assertTrue(xPathDOM1 instanceof DOMXPath);
        XPath xPathDOM2 = xPathExpressionEvaluator.getXPath(EXPRESSION, new DOMDocument());
        assertTrue(xPathDOM2 == xPathDOM1);
        XPath xPathDom4j3 = xPathExpressionEvaluator.getXPath(EXPRESSION, new DefaultDocument());
        assertTrue(xPathDom4j3 instanceof Dom4jXPath);
        assertTrue(xPathDOM1 != xPathDom4j3);
        XPath xPathDOM3 = xPathExpressionEvaluator.getXPath(OTHER_EXPRESSION, new DOMDocument());
        assertTrue(xPathDOM1 != xPathDOM3);
    }

    @Test
    public void testEmptyElement() throws ParserConfigurationException
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document dom = db.newDocument();
        Node node = dom.createTextNode(null);

        XPathExpressionEvaluator xPathExpressionEvaluator = new XPathExpressionEvaluator();
        Object result = xPathExpressionEvaluator.extractResultFromNode(node);
        Assert.assertEquals("", result);
    }

    @Test
    public void textNotSeparated() throws Exception
    {
        String payload = IOUtils.getResourceAsString("sswa2smtp.xml", getClass());
        Object value = muleContext.getExpressionManager().evaluate("#[xpath:/*/toAddress/text()]", getTestEvent(payload));
        assertEquals("XPath expression extracted the wrong value", "prachurya.barua@bt.com", value);
    }
}
