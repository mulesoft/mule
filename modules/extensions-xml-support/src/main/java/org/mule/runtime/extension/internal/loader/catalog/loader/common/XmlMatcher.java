/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.catalog.loader.common;

import com.google.common.base.Preconditions;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Helper class used to go through the catalog's file.
 * TODO MULE-13214: this class could be removed once MULE-13214 is done
 *
 * @since 4.0
 */
public class XmlMatcher {

  private final Element element;

  public static Optional<XmlMatcher> match(Element element, QName qName) {
    Optional<XmlMatcher> result = Optional.empty();
    if (Objects.equals(element.getNamespaceURI(), qName.getNamespaceURI())
        && Objects.equals(element.getLocalName(), qName.getLocalPart())) {
      result = Optional.of(new XmlMatcher(element));
    }
    return result;
  }

  private Stream<Node> nodeStream(NodeList nodeList) {
    return IntStream.range(0, nodeList.getLength()).mapToObj(nodeList::item);
  }

  private XmlMatcher(Element element) {
    Preconditions.checkNotNull(element);
    this.element = element;
  }

  public Optional<XmlMatcher> match(QName qName) {
    if (qName == null) {
      return Optional.empty();
    }
    return matchMany(qName).findFirst();
  }

  public Stream<XmlMatcher> matchMany(QName qName) {
    return nodeStream(element.getElementsByTagNameNS(qName.getNamespaceURI(), qName.getLocalPart()))
        .filter(node -> node instanceof Element)
        .map(node -> (Element) node)
        .filter(element -> qName.equals(new QName(element.getNamespaceURI(), element.getLocalName())))
        .map(XmlMatcher::new);
  }

  private Optional<String> matchAttribute(String namespaceUri, String localPart) {
    final Attr attributeNodeNS = element.getAttributeNodeNS(namespaceUri, localPart);
    return Optional.ofNullable(attributeNodeNS == null ? null : attributeNodeNS.getValue());
  }

  public Optional<String> matchAttribute(String localPart) {
    return matchAttribute(null, localPart);
  }

  public Element element() {
    return element;
  }
}
