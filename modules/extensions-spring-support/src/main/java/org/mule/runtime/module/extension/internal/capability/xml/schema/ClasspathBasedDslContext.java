/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema;

import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.util.annotation.AnnotationUtils.getAnnotation;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.loadExtension;
import static org.reflections.util.ClasspathHelper.forClassLoader;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslResolvingContext;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
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

  private void findExtensionsInClasspath() {

    Set<Class<?>> annotated = getExtensionTypes(forClassLoader(classLoader));

    annotated.forEach(type -> getAnnotation(type, Extension.class)
        .ifPresent(extension -> extensionsByName.put(extension.name(), type)));
  }

  private Set<Class<?>> getExtensionTypes(Collection<URL> urls) {
    try {
      return new Reflections(new ConfigurationBuilder()
          .setUrls(urls)
          .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner()))
              .getTypesAnnotatedWith(Extension.class);
    } catch (Exception e) {
      return emptySet();
    }
  }
}
