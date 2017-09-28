/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.util;

import static org.mule.runtime.config.internal.parsers.generic.AutoIdUtils.ATTRIBUTE_ID;
import static org.mule.runtime.config.internal.parsers.generic.AutoIdUtils.ATTRIBUTE_NAME;

import org.mule.runtime.core.api.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * These only depend on standard (JSE) XML classes and are used by Spring config code. For a more extensive (sub-)class, see the
 * XMLUtils class in the XML module.
 */
public class SpringXMLUtils {

  private static final Logger logger = LoggerFactory.getLogger(SpringXMLUtils.class);

  public static final String MULE_NAMESPACE_PREFIX = "http://www.mulesoft.org/schema/mule/";

  public static boolean isMuleNamespace(Element element) {
    String ns = element.getNamespaceURI();
    return ns != null && ns.startsWith(MULE_NAMESPACE_PREFIX);
  }

  public static boolean isBeansNamespace(Element element) {
    String ns = element.getNamespaceURI();
    return ns != null && ns.equals(BeanDefinitionParserDelegate.BEANS_NAMESPACE_URI);
  }

  public static String getNameOrId(Element element) {
    String id = element.getAttribute(ATTRIBUTE_ID);
    String name = element.getAttribute(ATTRIBUTE_NAME);
    if (StringUtils.isBlank(id)) {
      if (StringUtils.isBlank(name)) {
        return "";
      } else {
        return name;
      }
    } else {
      if (!StringUtils.isBlank(name) && !name.equals(id)) {
        logger.warn("Id (" + id + ") and name (" + name + ") differ for " + elementToString(element));
      }
      return id;
    }
  }

  public static String elementToString(Element e) {
    StringBuilder buf = new StringBuilder();
    buf.append(e.getTagName()).append("{");
    for (int i = 0; i < e.getAttributes().getLength(); i++) {
      if (i > 0) {
        buf.append(", ");
      }
      Node n = e.getAttributes().item(i);
      buf.append(attributeName((Attr) n)).append("=").append(n.getNodeValue());
    }
    buf.append("}");
    return buf.toString();
  }

  public static boolean isLocalName(Element element, String name) {
    return element.getLocalName().equals(name);
  }

  public static String attributeName(Attr attribute) {
    String name = attribute.getLocalName();
    if (null == name) {
      name = attribute.getName();
    }
    return name;
  }

  public static String getTextChild(Element element) {
    NodeList children = element.getChildNodes();
    String value = null;
    for (int i = 0; i < children.getLength(); ++i) {
      Node child = children.item(i);
      if (child.getNodeType() == Node.TEXT_NODE) {
        if (null != value) {
          throw new IllegalStateException("Element " + SpringXMLUtils.elementToString(element)
              + " has more than one text child.");
        } else {
          value = child.getNodeValue();
        }
      }
    }
    return value;
  }
}
