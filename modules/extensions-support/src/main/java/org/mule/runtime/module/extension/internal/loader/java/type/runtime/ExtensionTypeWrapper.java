/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getApiMethods;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.Category;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.ExpressionFunctions;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.module.extension.api.loader.java.type.ConfigurationElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.FunctionElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationElement;
import org.mule.runtime.module.extension.api.loader.java.type.ParameterizableTypeElement;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * {@link ConfigurationWrapper} specification for classes that are considered as Extensions
 *
 * @since 4.0
 */
public class ExtensionTypeWrapper<T> extends ComponentWrapper implements ExtensionElement, ParameterizableTypeElement {

  private LazyValue<Extension> extensionAnnotation = new LazyValue<>(() -> getAnnotation(Extension.class).get());

  public ExtensionTypeWrapper(Class<T> aClass, ClassTypeLoader typeLoader) {
    super(aClass, newCachedClassTypeLoader(typeLoader));
  }

  private static ClassTypeLoader newCachedClassTypeLoader(ClassTypeLoader classTypeLoader) {
    return new CachedClassTypeLoader(classTypeLoader);
  }

  /**
   * {@inheritDoc}
   */
  public List<ConfigurationElement> getConfigurations() {
    final Optional<Configurations> optionalConfigurations = this.getAnnotation(Configurations.class);
    if (optionalConfigurations.isPresent()) {
      final Configurations configurations = optionalConfigurations.get();
      return Stream.of(configurations.value()).map((Class<?> aClass) -> new ConfigurationWrapper(aClass, typeLoader))
          .collect(toList());
    }
    return emptyList();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<OperationElement> getOperations() {
    return getAnnotation(Operations.class)
        .map(classes -> Stream.of(classes.value())
            .flatMap(clazz -> getApiMethods(clazz).stream())
            .map(clazz -> (OperationElement) new OperationWrapper(clazz, typeLoader))
            .collect(toList()))
        .orElse(emptyList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<FunctionElement> getFunctions() {
    return getAnnotation(ExpressionFunctions.class)
        .map(classes -> Stream.of(classes.value())
            .flatMap(clazz -> getApiMethods(clazz).stream())
            .map(clazz -> (FunctionElement) new FunctionWrapper(clazz, typeLoader))
            .collect(toList()))
        .orElse(emptyList());
  }

  @Override
  public Category getCategory() {
    return extensionAnnotation.get().category();
  }

  @Override
  public String getVendor() {
    return extensionAnnotation.get().vendor();
  }

  @Override
  public String getName() {
    return extensionAnnotation.get().name();
  }

  private static class CachedClassTypeLoader implements ClassTypeLoader {

    private ClassTypeLoader classTypeLoader;

    private Map<Type, MetadataType> typeMetadataTypeMap = new HashMap<>();
    private Map<String, Optional<MetadataType>> typeIdentifierMetadataTypeMap = new HashMap<>();

    public CachedClassTypeLoader(ClassTypeLoader classTypeLoader) {
      Objects.requireNonNull(classTypeLoader, "classTypeLoader cannot be null");

      this.classTypeLoader = classTypeLoader;
    }

    @Override
    public MetadataType load(Type type) {
      return typeMetadataTypeMap.computeIfAbsent(type, k -> classTypeLoader.load(type));
    }

    @Override
    public ClassLoader getClassLoader() {
      return classTypeLoader.getClassLoader();
    }

    @Override
    public Optional<MetadataType> load(String typeIdentifier) {
      return typeIdentifierMetadataTypeMap.computeIfAbsent(typeIdentifier, k -> classTypeLoader.load(k));
    }

  }

}
