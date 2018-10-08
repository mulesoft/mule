/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.xml;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.Collections.emptyList;
import static org.mule.runtime.api.util.collection.Collectors.toImmutableList;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfo;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;

import com.google.common.collect.ImmutableList;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Function;

public class XmlNamespaceInfoProviderSupplier {


  /**
   * Creates an list of {@link XmlNamespaceInfoProvider} based on the list of {@link ExtensionModel}s used by the artifact.
   * <p/>
   * The list of {@link XmlNamespaceInfoProvider} will be discovered based on those extensions and the one discovered using by
   * SPI.
   *
   * @param extensionModels the {@link ExtensionModel}s of the artifact that contains the configuration.
   * @param xmlNamespaceInfoProvidersSupplierOptional function to discover {@link XmlNamespaceInfoProvider} implementations within
   *        a certain classloader.
   * @return a list of {@link XmlNamespaceInfoProvider}.
   */
  public static List<XmlNamespaceInfoProvider> createFromExtensionModels(Set<ExtensionModel> extensionModels,
                                                                         Optional<Function<ClassLoader, List<XmlNamespaceInfoProvider>>> xmlNamespaceInfoProvidersSupplierOptional) {
    Function<ClassLoader, List<XmlNamespaceInfoProvider>> supplier =
        xmlNamespaceInfoProvidersSupplierOptional.orElse(classLoader -> {
          Iterator<XmlNamespaceInfoProvider> iterator =
              ServiceLoader.load(XmlNamespaceInfoProvider.class, classLoader).iterator();
          if (iterator.hasNext()) {
            return copyOf(iterator);
          } else {
            return emptyList();
          }
        });
    List<XmlNamespaceInfoProvider> xmlNamespaceInfoProviders =
        ImmutableList.<XmlNamespaceInfoProvider>builder()
            .add(createStaticNamespaceInfoProviders(extensionModels))
            .addAll(discoverRuntimeXmlNamespaceInfoProvider(supplier))
            .addAll(discoverArtifactNamespaceInfoProvider(supplier))
            .build();
    return xmlNamespaceInfoProviders;
  }

  public static List<XmlNamespaceInfoProvider> createFromPluginClassloaders(Function<ClassLoader, List<XmlNamespaceInfoProvider>> xmlNamespaceInfoProvidersSupplier,
                                                                            List<ClassLoader> pluginsClassLoaders) {
    final ImmutableList.Builder<XmlNamespaceInfoProvider> namespaceInfoProvidersBuilder = ImmutableList.builder();
    namespaceInfoProvidersBuilder
        .addAll(xmlNamespaceInfoProvidersSupplier.apply(Thread.currentThread().getContextClassLoader()));
    for (ClassLoader pluginClassLoader : pluginsClassLoaders) {
      namespaceInfoProvidersBuilder.addAll(xmlNamespaceInfoProvidersSupplier.apply(pluginClassLoader));
    }
    return namespaceInfoProvidersBuilder.build();
  }

  private static List<XmlNamespaceInfoProvider> discoverArtifactNamespaceInfoProvider(Function<ClassLoader, List<XmlNamespaceInfoProvider>> xmlNamespaceInfoProvidersSupplier) {
    return xmlNamespaceInfoProvidersSupplier.apply(Thread.currentThread().getContextClassLoader());
  }

  private static List<XmlNamespaceInfoProvider> discoverRuntimeXmlNamespaceInfoProvider(Function<ClassLoader, List<XmlNamespaceInfoProvider>> xmlNamespaceInfoProvidersSupplier) {
    return xmlNamespaceInfoProvidersSupplier.apply(XmlNamespaceInfoProvider.class.getClassLoader());
  }

  private static XmlNamespaceInfoProvider createStaticNamespaceInfoProviders(Set<ExtensionModel> extensionModels) {
    List<XmlNamespaceInfo> extensionNamespaces = extensionModels.stream()
        .map(ext -> new StaticXmlNamespaceInfo(ext.getXmlDslModel().getNamespace(), ext.getXmlDslModel().getPrefix()))
        .collect(toImmutableList());

    return new StaticXmlNamespaceInfoProvider(extensionNamespaces);
  }


}
