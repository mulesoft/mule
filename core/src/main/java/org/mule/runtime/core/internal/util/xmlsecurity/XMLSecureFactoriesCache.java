/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.xmlsecurity;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;

import net.sf.saxon.jaxp.SaxonTransformerFactory;

/**
 * Avoid configuring factories each time they are used. Since we started using setFeature to avoid security issues,
 * getting a new XML factory object is very expensive.
 */
public class XMLSecureFactoriesCache {

  private LoadingCache<XMLFactoryConfig, Object> cache = null;

  private static volatile XMLSecureFactoriesCache instance;

  public static XMLSecureFactoriesCache getInstance() {
    if (instance == null) {
      synchronized (XMLSecureFactoriesCache.class) {
        if (instance == null) {
          instance = new XMLSecureFactoriesCache();
        }
      }
    }

    return instance;
  }

  private XMLSecureFactoriesCache() {
    cache = CacheBuilder.newBuilder()
        .build(new CacheLoader<XMLFactoryConfig, Object>() {

          @Override
          public Object load(XMLFactoryConfig key) throws Exception {
            return key.createFactory();
          }
        });
  }

  public DocumentBuilderFactory getDocumentBuilderFactory(final Boolean externalEntities, final Boolean expandEntities) {
    XMLFactoryConfig config = new XMLFactoryConfig(externalEntities, expandEntities, DocumentBuilderFactory.class.toString()) {

      @Override
      public Object createFactory() {
        return DefaultXMLSecureFactories.createDocumentBuilderFactory(this.externalEntities, this.expandEntities);
      }
    };

    return (DocumentBuilderFactory) cache.getUnchecked(config);
  }

  public SAXParserFactory getSAXParserFactory(Boolean externalEntities, Boolean expandEntities) {
    XMLFactoryConfig config = new XMLFactoryConfig(externalEntities, expandEntities, SAXParserFactory.class.toString()) {

      @Override
      public Object createFactory() {
        return DefaultXMLSecureFactories.createSaxParserFactory(this.externalEntities, this.expandEntities);
      }
    };

    return (SAXParserFactory) cache.getUnchecked(config);
  }

  public XMLInputFactory getXMLInputFactory(Boolean externalEntities, Boolean expandEntities) {
    XMLFactoryConfig config = new XMLFactoryConfig(externalEntities, expandEntities, XMLInputFactory.class.toString()) {

      @Override
      public Object createFactory() {
        return DefaultXMLSecureFactories.createXmlInputFactory(this.externalEntities, this.expandEntities);
      }
    };

    return (XMLInputFactory) cache.getUnchecked(config);
  }

  public TransformerFactory getTransformerFactory(Boolean externalEntities, Boolean expandEntities) {
    XMLFactoryConfig config = new XMLFactoryConfig(externalEntities, expandEntities, TransformerFactory.class.toString()) {

      @Override
      public Object createFactory() {
        return DefaultXMLSecureFactories.createTransformerFactory(this.externalEntities, this.expandEntities);
      }
    };

    return (TransformerFactory) cache.getUnchecked(config);
  }

  public TransformerFactory getSaxonTransformerFactory(Boolean externalEntities, Boolean expandEntities) {
    XMLFactoryConfig config = new XMLFactoryConfig(externalEntities, expandEntities, SaxonTransformerFactory.class.toString()) {

      @Override
      public Object createFactory() {
        return DefaultXMLSecureFactories.createSaxonTransformerFactory(this.externalEntities, this.expandEntities);
      }
    };

    return (TransformerFactory) cache.getUnchecked(config);
  }
}
