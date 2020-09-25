/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.serialization.internal;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.WriteAbortedException;

public class NodeWrapper implements Serializable, Node {
  public static final String NODE_WRAPPER_ENVELOPE_OPEN_TAG = "<node-wrapper-envelope>";
  public static final String NODE_WRAPPER_ENVELOPE_CLOSE_TAG = "</node-wrapper-envelope>";
  public static final String CDATA_OPEN = "<![CDATA[";
  public static final String CDATA_CLOSE = "]]>";
  private final short nodeType;
  private final String nodeName;
  private transient Node node;
  private String nodeAsString;

  public NodeWrapper(Node node) {
    this.node = node;
    this.nodeType = node.getNodeType();
    this.nodeName = node.getNodeName();
  }

  private Object writeReplace() throws ObjectStreamException {

    try {
      if (Node.CDATA_SECTION_NODE == node.getNodeType()) {
        this.nodeAsString = NODE_WRAPPER_ENVELOPE_OPEN_TAG + CDATA_OPEN + node.getNodeValue() + CDATA_CLOSE + NODE_WRAPPER_ENVELOPE_CLOSE_TAG;
        return this;
      }

      // Remove unwanted whitespaces
      node.normalize();
      XPath xpath = XPathFactory.newInstance().newXPath();
      XPathExpression expr = xpath.compile("//text()[normalize-space()='']");
      NodeList nodeList = (NodeList) expr.evaluate(node, XPathConstants.NODESET);

      for (int i = 0; i < nodeList.getLength(); ++i) {
        Node nd = nodeList.item(i);
        nd.getParentNode().removeChild(nd);
      }

      // Create and setup transformer
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

      // Turn the node into a string
      StringWriter writer = new StringWriter();
      transformer.transform(new DOMSource(getSerializingNode(node)), new StreamResult(writer));

      String serializedNode = writer.toString();

      this.nodeAsString = NODE_WRAPPER_ENVELOPE_OPEN_TAG + serializedNode + NODE_WRAPPER_ENVELOPE_CLOSE_TAG;
      return this;
    } catch (TransformerException e) {
      throw new WriteAbortedException("Error while serializing Dom object", e);
    } catch (XPathExpressionException e) {
      throw new WriteAbortedException("Error while serializing Dom object", e);
    }
  }

  private Object readResolve() throws ObjectStreamException {
    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document parse = dBuilder.parse(new ByteArrayInputStream(this.nodeAsString.getBytes()));
      this.node = getDeserializingNode(parse.getFirstChild());
    } catch (SAXException e) {
      throw new WriteAbortedException("Error while deserializing Dom object", e);
    } catch (IOException e) {
      throw new WriteAbortedException("Error while deserializing Dom object", e);
    } catch (ParserConfigurationException e) {
      throw new WriteAbortedException("Error while deserializing Dom object", e);
    }

    return this;
  }

  private Node getSerializingNode(Node node) {
    switch (node.getNodeType()) {
      case Node.ATTRIBUTE_NODE:
        return ((Attr) this.node).getOwnerElement();
      default:
        return this.node;
    }
  }

  private Node getDeserializingNode(Node node) {
    switch (nodeType) {
      case Node.ATTRIBUTE_NODE:
        return node.getFirstChild().getAttributes().getNamedItem(this.nodeName);
      default:
        return node.getFirstChild();
    }
  }

  public Node getNode() {
    return node;
  }

  @Override
  public String getNodeName() {
    return node.getNodeName();
  }

  @Override
  public String getNodeValue() throws DOMException {
    return node.getNodeValue();
  }

  @Override
  public void setNodeValue(String nodeValue) throws DOMException {
    node.setNodeValue(nodeValue);
  }

  @Override
  public short getNodeType() {
    return node.getNodeType();
  }

  @Override
  public Node getParentNode() {
    return node.getParentNode();
  }

  @Override
  public NodeList getChildNodes() {
    return node.getChildNodes();
  }

  @Override
  public Node getFirstChild() {
    return node.getFirstChild();
  }

  @Override
  public Node getLastChild() {
    return node.getLastChild();
  }

  @Override
  public Node getPreviousSibling() {
    return node.getPreviousSibling();
  }

  @Override
  public Node getNextSibling() {
    return node.getNextSibling();
  }

  @Override
  public NamedNodeMap getAttributes() {
    return node.getAttributes();
  }

  @Override
  public Document getOwnerDocument() {
    return node.getOwnerDocument();
  }

  @Override
  public Node insertBefore(Node newChild, Node refChild) throws DOMException {
    return node.insertBefore(newChild, refChild);
  }

  @Override
  public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
    return node.replaceChild(newChild, oldChild);
  }

  @Override
  public Node removeChild(Node oldChild) throws DOMException {
    return node.removeChild(oldChild);
  }

  @Override
  public Node appendChild(Node newChild) throws DOMException {
    return node.appendChild(newChild);
  }

  @Override
  public boolean hasChildNodes() {
    return node.hasChildNodes();
  }

  @Override
  public Node cloneNode(boolean deep) {
    return node.cloneNode(deep);
  }

  @Override
  public void normalize() {
    node.normalize();
  }

  @Override
  public boolean isSupported(String feature, String version) {
    return node.isSupported(feature, version);
  }

  @Override
  public String getNamespaceURI() {
    return node.getNamespaceURI();
  }

  @Override
  public String getPrefix() {
    return node.getPrefix();
  }

  @Override
  public void setPrefix(String prefix) throws DOMException {
    node.setPrefix(prefix);
  }

  @Override
  public String getLocalName() {
    return node.getLocalName();
  }

  @Override
  public boolean hasAttributes() {
    return node.hasAttributes();
  }

  @Override
  public String getBaseURI() {
    return node.getBaseURI();
  }

  @Override
  public short compareDocumentPosition(Node other) throws DOMException {
    return node.compareDocumentPosition(other);
  }

  @Override
  public String getTextContent() throws DOMException {
    return node.getTextContent();
  }

  @Override
  public void setTextContent(String textContent) throws DOMException {
    node.setTextContent(textContent);
  }

  @Override
  public boolean isSameNode(Node other) {
    return node.isSameNode(other);
  }

  @Override
  public String lookupPrefix(String namespaceURI) {
    return node.lookupPrefix(namespaceURI);
  }

  @Override
  public boolean isDefaultNamespace(String namespaceURI) {
    return node.isDefaultNamespace(namespaceURI);
  }

  @Override
  public String lookupNamespaceURI(String prefix) {
    return node.lookupNamespaceURI(prefix);
  }

  @Override
  public boolean isEqualNode(Node arg) {
    return node.isEqualNode(arg);
  }

  @Override
  public Object getFeature(String feature, String version) {
    return node.getFeature(feature, version);
  }

  @Override
  public Object setUserData(String key, Object data, UserDataHandler handler) {
    return node.setUserData(key, data, handler);
  }

  @Override
  public Object getUserData(String key) {
    return node.getUserData(key);
  }

  @Override
  public String toString() {
    return this.node.toString();
  }
}
