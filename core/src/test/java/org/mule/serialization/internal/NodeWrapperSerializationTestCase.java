/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.serialization.internal;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.mule.serialization.AbstractObjectSerializerContractTestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;

public class NodeWrapperSerializationTestCase extends AbstractObjectSerializerContractTestCase {

  private XPath xpath;

  @Override
  protected void doSetUp() throws Exception {
    serializer = muleContext.getObjectSerializer();
    xpath = XPathFactory.newInstance().newXPath();
  }

  @Test
  public void testWrappedTextNode() throws Exception {
    Node node = getNodeByXpath("<root>text</root>", "//root/text()");
    byte[] bytes = serializer.serialize(new NodeWrapper(node));

    Object deserialized = serializer.deserialize(bytes);

    assertThat(((Node) deserialized).getNodeName(), Matchers.containsString("text"));
  }

  @Test
  public void testWrappedCdataNode() throws Exception {
    Node node = getNodeByXpath("<root><someelemwithcdata><![CDATA[Irene, myself & I]]></someelemwithcdata></root>", "//root/someelemwithcdata/text()");
    byte[] bytes = serializer.serialize(new NodeWrapper(node));

    Object deserialized = serializer.deserialize(bytes);

    assertThat(((Node) deserialized).getNodeName(), Matchers.containsString("cdata"));
    assertThat(((Node) deserialized).getTextContent(), Matchers.containsString("Irene, myself & I"));
  }

  @Test
  public void testWrappedElementNode() throws Exception {
    Node node = getNodeByXpath("<root><elem><subelem>Hi</subelem></elem></root>", "//root/elem");
    byte[] bytes = serializer.serialize(new NodeWrapper(node));

    NodeWrapper deserialized = serializer.deserialize(bytes);

    Node found = findNodeByXPathExpression(deserialized, "//elem/subelem/text()");

    assertThat(found.getNodeName(), Matchers.containsString("text"));
    assertThat(found.getTextContent(), Matchers.containsString("Hi"));
  }

  @Test
  public void testWrappedAttributeNode() throws Exception {
    Node node = getNodeByXpath("<root><elem someatr=\"Howdy\"></elem></root>", "//root/elem/@someatr");
    byte[] bytes = serializer.serialize(new NodeWrapper(node));

    NodeWrapper deserialized = serializer.deserialize(bytes);

    assertThat(deserialized.getNodeName(), Matchers.containsString("someatr"));
    assertThat(deserialized.getTextContent(), Matchers.containsString("Howdy"));
  }

  @Test
  public void testWrappedCommentBeforeTextNode() throws Exception {
    Node node = getNodeByXpath("<root><elem><!-- this is a comment --> HI!</elem></root>", "//root/elem/comment()");
    byte[] bytes = serializer.serialize(new NodeWrapper(node));

    NodeWrapper deserialized = serializer.deserialize(bytes);

    assertThat(deserialized.getNodeName(), Matchers.containsString("comment"));
    assertThat(deserialized.getTextContent(), Matchers.containsString("this is a comment"));
  }

  @Test
  public void testWrappedCommentAfterTextNode() throws Exception {
    Node node = getNodeByXpath("<root><elem>HI! <!-- this is a comment --></elem></root>", "//root/elem/comment()");
    byte[] bytes = serializer.serialize(new NodeWrapper(node));

    NodeWrapper deserialized = serializer.deserialize(bytes);

    assertThat(deserialized.getNodeName(), Matchers.containsString("comment"));
    assertThat(deserialized.getTextContent(), Matchers.containsString("this is a comment"));
  }

  @Test
  public void testWrappedCommentBeforeElementNode() throws Exception {
    Node node = getNodeByXpath("<root><elem><!-- this is a comment --><sub>Bye</sub></elem></root>", "//root/elem/comment()");
    byte[] bytes = serializer.serialize(new NodeWrapper(node));

    NodeWrapper deserialized = serializer.deserialize(bytes);

    assertThat(deserialized.getNodeName(), Matchers.containsString("comment"));
    assertThat(deserialized.getTextContent(), Matchers.containsString("this is a comment"));
  }

  @Test
  public void testWrappedCommentAfterElementNode() throws Exception {
    Node node = getNodeByXpath("<root><elem><sub>Bye</sub><!-- this is a comment --></elem></root>", "//root/elem/comment()");
    byte[] bytes = serializer.serialize(new NodeWrapper(node));

    NodeWrapper deserialized = serializer.deserialize(bytes);

    assertThat(deserialized.getNodeName(), Matchers.containsString("comment"));
    assertThat(deserialized.getTextContent(), Matchers.containsString("this is a comment"));
  }

  @Test
  public void testWrappedProcessingInstructionNode() throws Exception {
    Node node = getNodeByXpath("<?xml-stylesheet type=\"text/xsl\" href=\"style.xsl\"?><root><elem><sub>Bye</sub><!-- this is a comment --></elem></root>", "//processing-instruction('xml-stylesheet')");
    byte[] bytes = serializer.serialize(new NodeWrapper(node));

    NodeWrapper deserialized = serializer.deserialize(bytes);

    assertThat(deserialized.getNodeName(), Matchers.containsString("xml-stylesheet"));
    assertThat(deserialized.getTextContent(), Matchers.containsString("type=\"text/xsl\" href=\"style.xsl\""));
  }

  private Node findNodeByXPathExpression(NodeWrapper nodeWrapper, String expression) throws XPathExpressionException {
    nodeWrapper.normalize();

    XPathExpression expr = xpath.compile(expression);
    return (Node) expr.evaluate(nodeWrapper.getNode(), XPathConstants.NODE);
  }

  private Node getNodeByXpath(String xmlString, String expression) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document document = dBuilder.parse(new ByteArrayInputStream(xmlString.getBytes()));

    XPathExpression expr = xpath.compile(expression);
    return (Node) expr.evaluate(document, XPathConstants.NODE);
  }

}