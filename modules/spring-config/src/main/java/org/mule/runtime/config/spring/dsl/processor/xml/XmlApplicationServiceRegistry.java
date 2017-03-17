/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.processor.xml;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.config.spring.dsl.api.xml.StaticXmlNamespaceInfo;
import org.mule.runtime.config.spring.dsl.api.xml.StaticXmlNamespaceInfoProvider;
import org.mule.runtime.core.api.registry.AbstractServiceRegistry;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfo;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;

import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.List;

/**
 * //TODO
 */
public class XmlApplicationServiceRegistry extends AbstractServiceRegistry {

  private final ServiceRegistry delegate;
  private final XmlNamespaceInfoProvider extensionsXmlInfoProvider;

  public XmlApplicationServiceRegistry(ServiceRegistry delegate, DslResolvingContext context) {
    this.delegate = delegate;

    List<XmlNamespaceInfo> extensionNamespaces;
    if (context != null) {
      extensionNamespaces = context.getExtensions().stream()
          .map(ext -> new StaticXmlNamespaceInfo(ext.getXmlDslModel().getNamespace(), ext.getXmlDslModel().getPrefix()))
          .collect(new ImmutableListCollector<>());
    } else {
      extensionNamespaces = ImmutableList.of();
    }

    extensionsXmlInfoProvider = new StaticXmlNamespaceInfoProvider(extensionNamespaces);
  }

  @Override
  protected <T> Collection<T> doLookupProviders(Class<T> providerClass, ClassLoader classLoader) {
    Collection<T> providers = delegate.lookupProviders(providerClass, classLoader);
    if (XmlNamespaceInfoProvider.class.equals(providerClass)) {
      providers = ImmutableList.<T>builder().addAll(providers).add((T) extensionsXmlInfoProvider).build();
    }

    return providers;
  }

}
