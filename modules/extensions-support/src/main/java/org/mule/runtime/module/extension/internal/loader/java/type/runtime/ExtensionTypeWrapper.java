/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.getExtensionInfo;
import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.mapReduceSingleAnnotation;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getApiMethods;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.meta.Category;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.module.extension.api.loader.java.type.ConfigurationElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.FunctionElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationElement;
import org.mule.runtime.module.extension.api.loader.java.type.ParameterizableTypeElement;
import org.mule.runtime.module.extension.internal.loader.java.info.ExtensionInfo;

import java.util.List;

/**
 * {@link ConfigurationWrapper} specification for classes that are considered as Extensions
 *
 * @since 4.0
 */
public class ExtensionTypeWrapper<T> extends ComponentWrapper implements ExtensionElement, ParameterizableTypeElement {

  private final LazyValue<ExtensionInfo> extensionInfo;

  public ExtensionTypeWrapper(Class<T> aClass, ClassTypeLoader typeLoader) {
    super(aClass, typeLoader);
    extensionInfo = new LazyValue<>(() -> getExtensionInfo(aClass));
  }

  /**
   * {@inheritDoc}
   */
  @Override
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

}
