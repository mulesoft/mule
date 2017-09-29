/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mule.runtime.core.api.util.IOUtils.closeQuietly;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Check that all top-level elements (direct children of Mule) are instances of "annotatedType"
 */
public class CheckAnnotatedTestCase extends AbstractMuleTestCase {

  private Document schema;
  private Element top;
  private Set<String> annotated = new HashSet<String>();
  private static final String annotatedType = "annotatedType";
  private static final String annotatedMixedContentType = "annotatedMixedContentType";

  // Add base annotated types
  {
    annotated.add(annotatedType);
    annotated.add(annotatedMixedContentType);
  }

  // Stdio will never support services or models
  private Set<String> allowedExceptions = new HashSet<String>();
  {
    allowedExceptions.add("abstractModelType");
  }

  /**
   * Check that all direct mule-specific children of the <mule/> element support Studio annotations
   */
  @Test
  public void testElementTypes() throws Exception {
    schema = createDOM(this.getClass().getClassLoader().getResourceAsStream("META-INF/mule-core-common.xsd"));
    top = schema.getDocumentElement();
    Element muleRootElements = findElement(top, "group", "muleRootElements");
    assertNotNull(muleRootElements);
    Set<Element> muleChildren = collectElementChildren(muleRootElements);
    for (Element elm : muleChildren) {
      checkElementTypeIsAnnotated(elm);
    }
  }

  /**
   * Check a single element defined in the schema
   */
  private void checkElementTypeIsAnnotated(Element elm) {
    Element type = getType(elm);
    if (type != null) {
      checkTypeIsAnnotated(type);
    }
  }

  /**
   * check a single type defined in the schema
   */
  private void checkTypeIsAnnotated(Element type) {
    String typeName = type.getAttribute("name");
    if (annotated.contains(typeName) || allowedExceptions.contains(typeName)) {
      return;
    }
    Element complexContent = findChild(type, "complexContent");
    Element extension = findChild(complexContent, "extension");
    String base = extension.getAttribute("base");
    assertFalse(base.equals(""));
    checkTypeIsAnnotated(findElement(top, "complexType", base));
    annotated.add(typeName);
  }

  /**
   * Get the type from the schema
   */
  private Element getType(Element elm) {
    if (!elm.getAttribute("type").equals("")) {
      return findElement(top, "complexType", elm.getAttribute("type"));
    }
    return findChild(elm, "complexType");
  }

  /**
   * Create a DOM from an input stream
   */
  private Document createDOM(InputStream input) throws Exception {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      return builder.parse(input);
    } finally {
      closeQuietly(input);
    }
  }

  /**
   * Find a child element in the schema
   */
  private Element findElement(Element parent, String type, String name) {
    for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node instanceof Element && type.equals(node.getLocalName()) && ((Element) node).getAttribute("name").equals(name)) {
        return (Element) node;
      }
    }
    return null;
  }

  /**
   * Find an element, possibly traversing a reference
   */
  private Element findElement(Element elm) {
    if (!elm.getAttribute("name").equals("")) {
      return elm;
    } else {
      return findElement(top, elm.getLocalName(), elm.getAttribute("ref"));
    }
  }

  /**
   * Collect all the children of an element
   */
  private Set<Element> collectElementChildren(Element parent) {
    Set<Element> children = new HashSet<Element>();
    for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node instanceof Element) {
        Element elm = (Element) node;
        if ("element".equals(node.getLocalName())) {
          children.add(findElement(elm));
        } else if ("sequence".equals(node.getLocalName()) || "choice".equals(node.getLocalName())
            || "group".equals(node.getLocalName())) {
          Set<Element> elms = collectElementChildren(elm);
          for (Element e : elms) {
            children.add(findElement(e));
          }
        }
      }
    }

    return children;
  }

  /**
   * Find an element child of an element
   */
  private Element findChild(Element parent, String localName) {
    for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (localName.equals(node.getLocalName())) {
        return (Element) node;
      }
    }
    fail("Cannot find child " + localName + " for " + parent.getAttribute("name"));
    return null;
  }
}
