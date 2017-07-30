/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.catalog.loader.xml;

import static com.google.common.base.Throwables.propagate;
import static javax.xml.parsers.DocumentBuilderFactory.newInstance;
import static org.mule.runtime.extension.internal.loader.catalog.loader.common.XmlMatcher.match;
import org.mule.runtime.extension.internal.loader.catalog.builder.TypesCatalogBuilder;
import org.mule.runtime.extension.internal.loader.catalog.loader.TypesCatalogLoaderContext;
import org.mule.runtime.extension.internal.loader.catalog.model.TypesCatalog;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URL;

/**
 * Loads a {@link TypesCatalog} from a {@link URL}.
 * TODO MULE-13214: this class could be removed once MULE-13214 is done
 *
 * @since 4.0
 */
public class TypesCatalogXmlLoader {

  private static final String NS_TYPES = "http://www.mulesoft.org/schema/mule/types";
  public static final QName ELEM_MULE = new QName(NS_TYPES, "mule");
  private static final QName ELEM_CATALOG = new QName(NS_TYPES, "catalog");
  private static final QName ELEM_TYPE = new QName(NS_TYPES, "type");
  private static final String ELEM_TYPE_ATTR_NAME = "name";
  private static final String ELEM_TYPE_ATTR_LOCATION = "location";
  private static final String ELEM_TYPE_ATTR_ELEMENT = "element";

  public TypesCatalog load(URL resourceType) throws Exception {
    TypesCatalogBuilder typesCatalogBuilder = new TypesCatalogBuilder(resourceType.toURI());
    TypesCatalogLoaderContext typesCatalogLoaderContext = new TypesCatalogLoaderContext(typesCatalogBuilder);
    load(resourceType, typesCatalogLoaderContext);
    return typesCatalogBuilder.build();
  }

  private void load(URL resourceType, TypesCatalogLoaderContext typesCatalogLoaderContext) {
    final Element documentElement;
    try {
      documentElement = parseRootElement(resourceType);
      final TypesCatalogBuilder typesCatalogBuilder = typesCatalogLoaderContext.getTypesCatalogBuilder();
      match(documentElement, ELEM_MULE).ifPresent(xmlMatcher -> {
        xmlMatcher.matchMany(ELEM_CATALOG).forEach(catalog -> {
          catalog.matchMany(ELEM_TYPE).forEach(type -> {
            typesCatalogBuilder.addTypesResolver(typesResolverBuilder -> {
              type.matchAttribute(ELEM_TYPE_ATTR_NAME).ifPresent(typesResolverBuilder::name);
              type.matchAttribute(ELEM_TYPE_ATTR_LOCATION).ifPresent(typesResolverBuilder::location);
              type.matchAttribute(ELEM_TYPE_ATTR_ELEMENT).ifPresent(typesResolverBuilder::element);
            });
          });
        });

      });
    } catch (Exception e) {
      propagate(e);
    }
  }

  public static Element parseRootElement(URL resourceType)
      throws ParserConfigurationException, SAXException, IOException {
    DocumentBuilderFactory documentBuilderFactory = newInstance();
    documentBuilderFactory.setNamespaceAware(true);
    documentBuilderFactory.setValidating(false);
    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
    Document document = documentBuilder.parse(resourceType.openStream());
    return document.getDocumentElement();
  }
}
