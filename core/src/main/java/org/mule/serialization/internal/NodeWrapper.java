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
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.WriteAbortedException;

import static javax.xml.transform.OutputKeys.ENCODING;
import static javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION;

/**
 * Ref: MULE-18799
 * Issues while serializing DOM java objects caused an OOM error
 * (See https://bz.apache.org/bugzilla/show_bug.cgi?id=18925 and
 * https://bugs.java.com/bugdatabase/view_bug.do?bug_id=4152790)
 * To prevent this writeReplace and readResolve methods are implemented through this wrapper which allows us to control
 * how these objects are serialized (https://docs.oracle.com/javase/7/docs/api/java/io/Serializable.html).
 * The transient modifier prevents the actual DOM object from being serialized and instead we use "nodeAsString" to save
 * and retrieve the information we need. We use writeReplace and javax.xml.transform.Transformer to create the string
 * representation when serializing, and readResolve and javax.xml.parsers.DocumentBuilderFactory to recreate the node
 * object when deserializing.
 */
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
      // This ensures the CDATA is not transformed into a text node
      if (CDATA_SECTION_NODE == node.getNodeType()) {
        this.nodeAsString = NODE_WRAPPER_ENVELOPE_OPEN_TAG + CDATA_OPEN + node.getNodeValue() + CDATA_CLOSE + NODE_WRAPPER_ENVELOPE_CLOSE_TAG;
        return this;
      }

      // Create and setup transformer
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(ENCODING, "UTF-8");

      transformer.setOutputProperty(OMIT_XML_DECLARATION, "yes");

      // Turn the node into a string
      StringWriter writer = new StringWriter();
      transformer.transform(new DOMSource(getSerializingNode(node)), new StreamResult(writer));

      this.nodeAsString = NODE_WRAPPER_ENVELOPE_OPEN_TAG + writer.toString() + NODE_WRAPPER_ENVELOPE_CLOSE_TAG;
      return this;
    } catch (TransformerException e) {
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
      case ATTRIBUTE_NODE:
        return ((Attr) this.node).getOwnerElement();
      default:
        return this.node;
    }
  }

  private Node getDeserializingNode(Node node) {
    switch (nodeType) {
      case ATTRIBUTE_NODE:
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
