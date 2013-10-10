/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.expression;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

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

import static org.junit.Assert.assertTrue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

@SmallTest
public class XPathExpressionEvaluatorTestCase extends AbstractMuleTestCase
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
}
