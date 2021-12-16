/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.module.extension.internal.loader.base.delegate.MuleExtensionAnnotationParser.getExtensionInfo;
import static org.mule.runtime.module.extension.internal.loader.base.delegate.MuleExtensionAnnotationParser.mapReduceSingleAnnotation;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getApiMethods;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.Category;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.module.extension.api.loader.java.type.ConfigurationElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.FunctionElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationElement;
import org.mule.runtime.module.extension.api.loader.java.type.ParameterizableTypeElement;
import org.mule.runtime.module.extension.internal.loader.java.info.ExtensionInfo;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * {@link ConfigurationWrapper} specification for classes that are considered as Extensions
 *
 * @since 4.0
 */
public class ExtensionTypeWrapper<T> extends ComponentWrapper implements ExtensionElement, ParameterizableTypeElement {

  private LazyValue<ExtensionInfo> extensionInfo;

  public ExtensionTypeWrapper(Class<T> aClass, ClassTypeLoader typeLoader) {
    super(aClass, newCachedClassTypeLoader(typeLoader));
    extensionInfo = new LazyValue<>(() -> getExtensionInfo(aClass));
  }

  private static ClassTypeLoader newCachedClassTypeLoader(ClassTypeLoader classTypeLoader) {
    return new CachedClassTypeLoader(classTypeLoader);
  }

  /**
   * {@inheritDoc}
   */
  public List<ConfigurationElement> getConfigurations() {
    return mapReduceSingleAnnotation(
                                     this,
                                     Configurations.class,
                                     org.mule.sdk.api.annotation.Configurations.class,
                                     value -> value.getClassArrayValue(Configurations::value),
                                     value -> value.getClassArrayValue(org.mule.sdk.api.annotation.Configurations::value))
                                         .map(types -> types.stream()
                                             .map(type -> (ConfigurationElement) new ConfigurationWrapper(type.getDeclaringClass()
                                                 .get(), typeLoader))
                                             .collect(toList()))
                                         .orElse(emptyList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<OperationElement> getOperations() {
    return getOperationClassStream()
        .flatMap(clazz -> getApiMethods(clazz).stream())
        .map(clazz -> (OperationElement) new OperationWrapper(clazz, typeLoader))
        .collect(toList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<FunctionElement> getFunctions() {
    return getExpressionFunctionClassStream()
        .flatMap(clazz -> getApiMethods(clazz).stream())
        .map(clazz -> (FunctionElement) new FunctionWrapper(clazz, typeLoader))
        .collect(toList());
  }

  @Override
  public Category getCategory() {
    return extensionInfo.get().getCategory();
  }

  @Override
  public String getVendor() {
    return extensionInfo.get().getVendor();
  }

  @Override
  public String getName() {
    return extensionInfo.get().getName();
  }

  private static class CachedClassTypeLoader implements ClassTypeLoader {

    private ClassTypeLoader classTypeLoader;

    private Map<Type, MetadataType> typeMetadataTypeMap = new WeakHashMap<>();
    private Map<String, Optional<MetadataType>> typeIdentifierMetadataTypeMap = new WeakHashMap<>();

    public CachedClassTypeLoader(ClassTypeLoader classTypeLoader) {
      requireNonNull(classTypeLoader, "classTypeLoader cannot be null");

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
