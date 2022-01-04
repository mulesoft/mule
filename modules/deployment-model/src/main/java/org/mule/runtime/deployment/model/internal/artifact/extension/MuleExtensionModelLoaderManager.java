/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal.artifact.extension;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.extension.api.loader.xml.XmlExtensionModelLoader.DESCRIBER_ID;
import static org.mule.runtime.module.extension.api.loader.java.CraftedExtensionModelLoader.CRAFTED_LOADER_ID;
import static org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader.JAVA_LOADER_ID;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.collection.SmallMap;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.deployment.model.api.plugin.LoaderDescriber;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.xml.XmlExtensionModelLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.extension.api.loader.java.CraftedExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;

/**
 * Implementation of {@link MuleExtensionModelLoaderManager} that uses SPI to look for the {@link ExtensionModelLoader} available
 * from the container.
 *
 * @since 4.0
 */
public class MuleExtensionModelLoaderManager implements ExtensionModelLoaderManager {

  private static final Logger LOGGER = getLogger(MuleExtensionModelLoaderManager.class);
  private static final Class<ExtensionModelLoader> PROVIDER_CLASS = ExtensionModelLoader.class;

  private final Map<String, ExtensionModelLoader> extensionModelLoaders = new SmallMap<>();
  private Supplier<Collection<ExtensionModelLoader>> extModelLoadersLookup = Collections::emptyList;

  public MuleExtensionModelLoaderManager() {
    extensionModelLoaders.put(JAVA_LOADER_ID, new DefaultJavaExtensionModelLoader());
    extensionModelLoaders.put(CRAFTED_LOADER_ID, new CraftedExtensionModelLoader());
    extensionModelLoaders.put(DESCRIBER_ID, new XmlExtensionModelLoader());
  }

  /**
   * Creates an instance of the manager.
   *
   * @param containerClassLoader {@link ClassLoader} from the container.
   */
  public MuleExtensionModelLoaderManager(ArtifactClassLoader containerClassLoader) {
    this();
    requireNonNull(containerClassLoader, "containerClassLoader cannot be null");
    this.extModelLoadersLookup =
        () -> new SpiServiceRegistry().lookupProviders(PROVIDER_CLASS, containerClassLoader.getClassLoader());
  }

  public void setExtensionModelLoadersLookup(Supplier<Collection<ExtensionModelLoader>> extModelLoadersLookup) {
    this.extModelLoadersLookup = extModelLoadersLookup;
  }

  /**
   * Will look through SPI every class that implements the {@code providerClass} and if there are repeated IDs, it will collect
   * them all to throw an exception with the detailed message.
   * <p/>
   * The exception, if thrown, will have the following message:
   * 
   * <pre>
   *   There are several loaders that return the same ID when looking up providers for 'org.mule.runtime.module.artifact.ExtensionModelLoader'. Full error list:
   *   ID [some-id] is being returned by the following classes [org.foo.FooLoader, org.bar.BarLoader]
   *   ID [another-id] is being returned by the following classes [org.foo2.FooLoader2, org.bar2.BarLoader2]
   * </pre>
   *
   * @throws IllegalStateException if there are loaders with repeated IDs.
   */
  @Override
  public void start() throws MuleException {
    final Collection<ExtensionModelLoader> extensionModelLoaders = extModelLoadersLookup.get();
    validateLoaderLookupUniqueness(extensionModelLoaders);

    extensionModelLoaders.forEach(loader -> this.extensionModelLoaders.put(loader.getId(), loader));

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("ExtensionModelLoader registered identifiers: {}", printExtensionModelLoaderIDs());
    }
  }

  private void validateLoaderLookupUniqueness(Collection<ExtensionModelLoader> extensionModelLoaders) {
    final StringBuilder sb = new StringBuilder();
    extensionModelLoaders.stream().collect(groupingBy(ExtensionModelLoader::getId))
        .entrySet().stream().filter(entry -> entry.getValue().size() > 1)
        .forEach(
                 entry -> {
                   // At this point we are sure there are at least 2 classes that return the same ID, we will append it to the
                   // builder
                   final String classes = entry.getValue().stream()
                       .map(extensionModelLoader -> extensionModelLoader.getClass().getName()).collect(Collectors.joining(", "));
                   sb.append(lineSeparator()).append("ID [").append(entry.getKey())
                       .append("] is being returned by the following classes [").append(classes).append("]");
                 });
    if (sb.length() > 0) {
      throw new MuleRuntimeException(createStaticMessage(format(
                                                                "There are several loaders that return the same identifier when looking up providers for '%s'. Full error list: %s",
                                                                PROVIDER_CLASS.getName(), sb)));
    }
  }

  @Override
  public void stop() throws MuleException {
    extensionModelLoaders.clear();
  }

  @Override
  public Optional<ExtensionModelLoader> getExtensionModelLoader(LoaderDescriber loaderDescriber) {
    return extensionModelLoaders.containsKey(loaderDescriber.getId()) ? of(extensionModelLoaders.get(loaderDescriber.getId()))
        : empty();
  }

  private String printExtensionModelLoaderIDs() {
    return extensionModelLoaders.keySet().stream().collect(joining(", "));
  }

  @Override
  public String toString() {
    return format("%s[extensionModelLoaders=%s]", getClass().getName(), printExtensionModelLoaderIDs());
  }

}
