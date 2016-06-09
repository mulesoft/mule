/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.spring.dsl.processor.xml;

import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.SPRING_CONTEXT_NAMESPACE;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.SPRING_NAMESPACE;
import static org.mule.runtime.config.spring.dsl.processor.xml.CoreXmlNamespaceInfoProvider.CORE_NAMESPACE_NAME;
import static org.mule.runtime.config.spring.dsl.processor.xml.XmlCustomAttributeHandler.to;
import org.mule.runtime.config.spring.dsl.api.xml.XmlNamespaceInfo;
import org.mule.runtime.config.spring.dsl.api.xml.XmlNamespaceInfoProvider;
import org.mule.runtime.config.spring.dsl.processor.ConfigLine;
import org.mule.runtime.config.spring.dsl.processor.ConfigLineProvider;
import org.mule.runtime.core.api.MuleRuntimeException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Simple parser that transforms an XML document to a set of {@link org.mule.runtime.config.spring.dsl.processor.ConfigLine}
 * objects.
 *
 * It uses the SPI interface {@link org.mule.runtime.config.spring.dsl.api.xml.XmlNamespaceInfoProvider} to locate for
 * all namespace info provided and normalize the namespace from the XML document.
 *
 * @since 4.0
 */
public class XmlApplicationParser
{

    private static final String COLON = ":";
    private static final Map<String, String> predefinedNamespace = new HashMap<>();
    private static final String UNDEFINED_NAMESPACE = "undefined";
    private final ServiceLoader<XmlNamespaceInfoProvider> xmlNamespaceInfoProviders;
    private final Cache<String, String> namespaceCache;

    static
    {
        predefinedNamespace.put("http://www.springframework.org/schema/beans", SPRING_NAMESPACE);
        predefinedNamespace.put("http://www.springframework.org/schema/context", SPRING_CONTEXT_NAMESPACE);
    }

    public XmlApplicationParser()
    {
        xmlNamespaceInfoProviders = ServiceLoader.load(XmlNamespaceInfoProvider.class);
        namespaceCache = CacheBuilder.newBuilder().build();
    }

    private String loadNamespaceFromProviders(String namespaceUri)
    {
        if (predefinedNamespace.containsKey(namespaceUri))
        {
            return predefinedNamespace.get(namespaceUri);
        }
        Iterator<XmlNamespaceInfoProvider> iterator = xmlNamespaceInfoProviders.iterator();
        while (iterator.hasNext())
        {
            XmlNamespaceInfoProvider namespaceInfoProvider = iterator.next();
            Optional<XmlNamespaceInfo> matchingXmlNamespaceInfo = namespaceInfoProvider
                    .getXmlNamespacesInfo()
                    .stream()
                    .filter(xmlNamespaceInfo -> namespaceUri.startsWith(xmlNamespaceInfo.getNamespaceUriPrefix()))
                    .findFirst();
            if (matchingXmlNamespaceInfo.isPresent())
            {
                return matchingXmlNamespaceInfo.get().getNamespace();
            }
        }
        //TODO MULE-9638 for now since just return a fake value since guava cache does not support null values. When done right throw a configuration exception with a meaningful message if there's no info provider defined
        return UNDEFINED_NAMESPACE;
    }

    public String getNormalizedNamespace(String namespaceUri, String namespacePrefix)
    {
        try
        {
            return namespaceCache.get(namespaceUri, () -> {
                String namespace = loadNamespaceFromProviders(namespaceUri);
                if (namespace == null)
                {
                    namespace = namespacePrefix;
                }
                return namespace;
            });
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    public Optional<ConfigLine> parse(Element configElement)
    {
        return configLineFromElement(configElement, () -> {
            return null;
        });
    }

    private Optional<ConfigLine> configLineFromElement(Node node, ConfigLineProvider parentProvider)
    {
        if (!isValidType(node))
        {
            return Optional.empty();
        }

        String identifier = parseIdentifier(node);
        String namespace = parseNamespace(node);

        ConfigLine.Builder builder = new ConfigLine.Builder()
                .setIdentifier(identifier)
                .setNamespace(namespace)
                .setNode(node)
                .setParent(parentProvider);
        to(builder).addNode(node);

        NamedNodeMap attributes = node.getAttributes();
        if (node.hasAttributes())
        {
            for (int i = 0; i < attributes.getLength(); i++)
            {
                Node attribute = attributes.item(i);
                builder.addConfigAttribute(attribute.getNodeName(), attribute.getNodeValue());
            }
        }
        if (node.hasChildNodes())
        {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++)
            {
                Node child = children.item(i);
                if (isTextContent(child))
                {
                    builder.setTextContent(child.getNodeValue());
                }
                else
                {
                    configLineFromElement(child, () -> {
                        return builder.build();
                    }).ifPresent(configLine -> {
                        builder.addChild(configLine);
                    });
                }
            }
        }
        return Optional.of(builder.build());
    }

    private String parseNamespace(Node node)
    {
        String namespace = CORE_NAMESPACE_NAME;
        if (node.getNodeType() != Node.CDATA_SECTION_NODE)
        {
            namespace = getNormalizedNamespace(node.getNamespaceURI(), node.getPrefix());
            if (namespace.equals(UNDEFINED_NAMESPACE))
            {
                namespace = node.getPrefix();
            }
        }
        return namespace;
    }

    private String parseIdentifier(Node node)
    {
        String identifier = node.getNodeName();
        String[] nameParts = identifier.split(COLON);
        if (nameParts.length > 1)
        {
            identifier = nameParts[1];
        }
        return identifier;
    }

    private boolean isValidType(Node node)
    {
        return node.getNodeType() != Node.TEXT_NODE && node.getNodeType() != Node.COMMENT_NODE;
    }

    private boolean isTextContent(Node node)
    {
        return node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE;
    }

}
