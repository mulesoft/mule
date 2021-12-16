/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema;

import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.loadExtension;
import static org.mule.runtime.module.extension.internal.loader.base.delegate.MuleExtensionAnnotationParser.getExtensionInfo;

import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import static com.google.common.collect.ImmutableSet.copyOf;
import static org.reflections.util.ClasspathHelper.forClassLoader;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.type.TypeCatalog;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.module.extension.api.util.MuleExtensionUtils;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

/**
 * Implementation of {@link DslResolvingContext} that scans the Classpath looking for the {@link Class} associated to the provided
 * Extension {@code name}
 *
 * @since 4.0
 */
class ClasspathBasedDslContext implements DslResolvingContext {

  private final ClassLoader classLoader;
  private final Map<String, Class<?>> extensionsByName = new HashMap<>();
  private final Map<String, ExtensionModel> resolvedModels = new HashMap<>();
  private final LazyValue<TypeCatalog> typeCatalog = new LazyValue<>(() -> TypeCatalog.getDefault(getExtensions()));

  ClasspathBasedDslContext(ClassLoader classLoader) {
    this.classLoader = classLoader;
    findExtensionsInClasspath();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ExtensionModel> getExtension(String name) {
    if (!resolvedModels.containsKey(name) && extensionsByName.containsKey(name)) {
      resolvedModels.put(name, loadExtension(extensionsByName.get(name)));
    }

    return ofNullable(resolvedModels.get(name));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ExtensionModel> getExtensionForType(String typeId) {
    return getTypeCatalog().getDeclaringExtension(typeId).flatMap(this::getExtension);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<ExtensionModel> getExtensions() {
    return resolvedModels.size() != extensionsByName.size()
        ? extensionsByName.values().stream().map(MuleExtensionUtils::loadExtension).collect(toSet())
        : copyOf(resolvedModels.values());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TypeCatalog getTypeCatalog() {
    return typeCatalog.get();
  }

  private void findExtensionsInClasspath() {
    final Collection<URL> mulePluginsUrls = forClassLoader(classLoader).stream()
        .filter(url -> url.getPath().contains(MULE_PLUGIN_CLASSIFIER))
        .collect(toList());

    getExtensionTypes(mulePluginsUrls)
        .forEach(type -> extensionsByName.put(getExtensionInfo(type).getName(), type));
  }

  private Set<Class<?>> getExtensionTypes(Collection<URL> urls) {
    try {
      Reflections reflections = new Reflections(new ConfigurationBuilder()
          .setUrls(urls)
          .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner()));

      Set<Class<?>> classes = new LinkedHashSet<>(reflections.getTypesAnnotatedWith(Extension.class));
      classes.addAll(reflections.getTypesAnnotatedWith(org.mule.sdk.api.annotation.Extension.class));

      return classes;
    } catch (Exception e) {
      return emptySet();
    }
  }
}
