/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.api.dsl.processor.xml;

import static org.mule.runtime.config.internal.dsl.processor.xml.XmlCustomAttributeHandler.IS_CDATA;
import static org.mule.runtime.config.internal.dsl.processor.xml.XmlCustomAttributeHandler.to;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.DOMAIN_NAMESPACE;
import static org.mule.runtime.internal.dsl.DslConstants.DOMAIN_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.EE_DOMAIN_NAMESPACE;
import static org.mule.runtime.internal.dsl.DslConstants.EE_DOMAIN_PREFIX;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.config.api.dsl.processor.ConfigLine;
import org.mule.runtime.config.api.dsl.processor.ConfigLineProvider;
import org.mule.runtime.config.internal.parsers.XmlMetadataAnnotations;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfo;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * Simple parser that transforms an XML document to a set of {@link org.mule.runtime.config.api.dsl.processor.ConfigLine}
 * objects.
 * <p>
 * It uses the SPI interface {@link XmlNamespaceInfoProvider} to locate for all namespace info provided and normalize the
 * namespace from the XML document.
 *
 * @since 4.0
 */
public class XmlApplicationParser {

  private static final String COLON = ":";
  private static final Map<String, String> predefinedNamespace = new HashMap<>();
  private static final String UNDEFINED_NAMESPACE = "undefined";
  private final List<XmlNamespaceInfoProvider> namespaceInfoProviders;
  private final Cache<String, String> namespaceCache;

  static {
    predefinedNamespace.put(DOMAIN_NAMESPACE, DOMAIN_PREFIX);
    predefinedNamespace.put(EE_DOMAIN_NAMESPACE, EE_DOMAIN_PREFIX);
  }

  public XmlApplicationParser(ServiceRegistry serviceRegistry, List<ClassLoader> pluginsClassLoaders) {
    this(discoverNamespaceInfoProviders(serviceRegistry, pluginsClassLoaders));
  }

  private static List<XmlNamespaceInfoProvider> discoverNamespaceInfoProviders(ServiceRegistry serviceRegistry,
                                                                               List<ClassLoader> pluginsClassLoaders) {
    final Builder<XmlNamespaceInfoProvider> namespaceInfoProvidersBuilder = ImmutableList.builder();
    namespaceInfoProvidersBuilder
        .addAll(serviceRegistry.lookupProviders(XmlNamespaceInfoProvider.class, XmlNamespaceInfoProvider.class.getClassLoader()));
    for (ClassLoader pluginClassLoader : pluginsClassLoaders) {
      namespaceInfoProvidersBuilder.addAll(serviceRegistry.lookupProviders(XmlNamespaceInfoProvider.class, pluginClassLoader));
    }
    return namespaceInfoProvidersBuilder.build();
  }

  public XmlApplicationParser(List<XmlNamespaceInfoProvider> namespaceInfoProviders) {
    this.namespaceInfoProviders = ImmutableList.<XmlNamespaceInfoProvider>builder().addAll(namespaceInfoProviders).build();
    this.namespaceCache = CacheBuilder.newBuilder().build();
  }

  private String loadNamespaceFromProviders(String namespaceUri) {
    if (predefinedNamespace.containsKey(namespaceUri)) {
      return predefinedNamespace.get(namespaceUri);
    }
    for (XmlNamespaceInfoProvider namespaceInfoProvider : namespaceInfoProviders) {
      Optional<XmlNamespaceInfo> matchingXmlNamespaceInfo = namespaceInfoProvider.getXmlNamespacesInfo().stream()
          .filter(xmlNamespaceInfo -> namespaceUri.equals(xmlNamespaceInfo.getNamespaceUriPrefix())).findFirst();
      if (matchingXmlNamespaceInfo.isPresent()) {
        return matchingXmlNamespaceInfo.get().getNamespace();
      }
    }
    // TODO MULE-9638 for now since just return a fake value since guava cache does not support null values. When done right throw
    // a configuration exception with a meaningful message if there's no info provider defined
    return UNDEFINED_NAMESPACE;
  }

  public String getNormalizedNamespace(String namespaceUri, String namespacePrefix) {
    try {
      return namespaceCache.get(namespaceUri, () -> {
        String namespace = loadNamespaceFromProviders(namespaceUri);
        if (namespace == null) {
          namespace = namespacePrefix;
        }
        return namespace;
      });
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  public Optional<ConfigLine> parse(Element configElement) {
    return configLineFromElement(configElement, () -> null);
  }

  private Optional<ConfigLine> configLineFromElement(Node node, ConfigLineProvider parentProvider) {
    if (!isValidType(node)) {
      return Optional.empty();
    }

    String identifier = parseIdentifier(node);
    String namespace = parseNamespace(node);

    ConfigLine.Builder builder =
        new ConfigLine.Builder().setIdentifier(identifier).setNamespace(namespace).setNode(node).setParent(parentProvider);

    XmlMetadataAnnotations userData = (XmlMetadataAnnotations) node.getUserData(XmlMetadataAnnotations.METADATA_ANNOTATIONS_KEY);
    int lineNumber = userData.getLineNumber();
    builder.setLineNumber(lineNumber);

    to(builder).addNode(node);

    Element element = (Element) node;
    NamedNodeMap attributes = element.getAttributes();
    if (element.hasAttributes()) {
      for (int i = 0; i < attributes.getLength(); i++) {
        Node attribute = attributes.item(i);
        Attr attributeNode = element.getAttributeNode(attribute.getNodeName());
        boolean isFromXsd = !attributeNode.getSpecified();
        builder.addConfigAttribute(attribute.getNodeName(), attribute.getNodeValue(), isFromXsd);
      }
    }
    if (node.hasChildNodes()) {
      NodeList children = node.getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        Node child = children.item(i);
        if (isTextContent(child)) {
          builder.setTextContent(child.getNodeValue());
          if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
            builder.addCustomAttribute(IS_CDATA, Boolean.TRUE);
            break;
          }
        } else {
          configLineFromElement(child, builder::build).ifPresent(builder::addChild);
        }
      }
    }
    return Optional.of(builder.build());
  }

  private String parseNamespace(Node node) {
    String namespace = CORE_PREFIX;
    if (node.getNodeType() != Node.CDATA_SECTION_NODE) {
      namespace = getNormalizedNamespace(node.getNamespaceURI(), node.getPrefix());
      if (namespace.equals(UNDEFINED_NAMESPACE)) {
        namespace = node.getPrefix();
      }
    }
    return namespace;
  }

  private String parseIdentifier(Node node) {
    String identifier = node.getNodeName();
    String[] nameParts = identifier.split(COLON);
    if (nameParts.length > 1) {
      identifier = nameParts[1];
    }
    return identifier;
  }

  private boolean isValidType(Node node) {
    return node.getNodeType() != Node.TEXT_NODE && node.getNodeType() != Node.COMMENT_NODE;
  }

  private boolean isTextContent(Node node) {
    return node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE;
  }

}
