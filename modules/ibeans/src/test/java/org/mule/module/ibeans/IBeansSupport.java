/*
 * $Id:  $
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * TODO
 */
public class IBeansSupport
{
    /**
     * logger used by this class
     */
    protected static transient final Log logger = LogFactory.getLog(IBeansSupport.class);

    /**
     * Creates an XPath object with a custom NamespaceContext given the Node to operate on
     * @param node the Node or document to operate on.  Note that namespace handling will not work if a Node fragment is passed in
     * @return a new XPath object
     */
    private static XPath createXPath(Node node)
    {
        XPath xp = XPathFactory.newInstance().newXPath();
        if (node instanceof Document)
        {
            xp.setNamespaceContext(new XPathNamespaceContext((Document) node));
        }
        return xp;
    }

    /**
     * Select a single XML node using an Xpath
     * @param xpath the XPath expression to evaluate
     * @param node the node (or document) to exaluate on
     * @return the result of the evaluation.  Note that if an error occurs, the error is logged and null is returned
     */
    public static Node selectOne(String xpath, Node node)
    {
        try
        {
            XPath xp = createXPath(node);
            return (Node) xp.evaluate(xpath, node, XPathConstants.NODE);
        }
        catch (XPathExpressionException e)
        {
            logger.error(e.getMessage());
            return null;
        }
    }

    /**
     * Select a single XML String value using an Xpath
     * @param xpath the XPath expression to evaluate
     * @param node the node (or document) to exaluate on
     * @return the result of the evaluation.  Note that if an error occurs, the error is logged and null is returned
     */
    public static String selectValue(String xpath, Node node)
    {
        try
        {
            XPath xp = createXPath(node);
            return (String) xp.evaluate(xpath, node, XPathConstants.STRING);
        }
        catch (XPathExpressionException e)
        {
            logger.error(e.getMessage());
            return null;
        }
    }

    /**
     * Select a set of Node objects using the Xpath expression
     * @param xpath the XPath expression to evaluate
     * @param node the node (or document) to exaluate on
     * @return the result of the evaluation.  Note that if an error occurs, the error is logged and null is returned
     */
    public static List<Node> select(String xpath, Node node)
    {
        try
        {
            XPath xp = createXPath(node);
            NodeList nl = (NodeList) xp.evaluate(xpath, node, XPathConstants.NODESET);
            List<Node> nodeList = new ArrayList<Node>(nl.getLength());
            for (int i = 0; i < nl.getLength(); i++)
            {
                nodeList.add(nl.item(i));
            }
            return nodeList;
        }
        catch (XPathExpressionException e)
        {
            logger.error(e.getMessage());
            return null;
        }
    }


    /**
     * Returns a formatted XML string representation of an XML Node or Document.  This is useful for debugging or for
     * capturing data that can be used for Mock testing.
     * @param node the Xml to read
     * @return a formated XML string
     */
    public static String prettyPrintXml(Node node)
    {
        try
        {
            // Set up the output transformer
            TransformerFactory transfac = TransformerFactory.newInstance();
            javax.xml.transform.Transformer trans = transfac.newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");

            // Print the DOM node
            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            DOMSource source = new DOMSource(node);
            trans.transform(source, result);
            return sw.toString();
        }
        catch (TransformerException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * The default namespace context that will read namespaces from the current document if the
     * Node being processed is a Document
     */
    private static class XPathNamespaceContext implements NamespaceContext
    {
        private Document document;

        public XPathNamespaceContext(Document document)
        {
            this.document = document;
        }

        public String getNamespaceURI(String prefix)
        {
            if (prefix == null || prefix.equals(""))
            {
                return document.getDocumentElement().getNamespaceURI();
            }
            else
            {
                return document.lookupNamespaceURI(prefix);
            }
        }

        public String getPrefix(String namespaceURI)
        {
            return document.lookupPrefix(namespaceURI);
        }

        public Iterator<String> getPrefixes(String namespaceURI)
        {
            List<String> list = new ArrayList<String>();
            list.add(getPrefix(namespaceURI));
            return list.iterator();
        }
    }
}
