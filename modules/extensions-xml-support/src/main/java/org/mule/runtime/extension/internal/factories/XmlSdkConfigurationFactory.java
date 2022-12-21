/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.factories;

import static org.mule.runtime.core.internal.util.MultiParentClassLoaderUtils.multiParentClassLoaderFor;

import static java.lang.Thread.currentThread;

import static net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default.CHILD_FIRST;
import static net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.api.runtime.config.ConfigurationFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Supplier;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;

public class XmlSdkConfigurationFactory implements ConfigurationFactory {

  private final Supplier<Class<?>> configClass;

  private Object lastBuilt;

  public XmlSdkConfigurationFactory(List<ParameterDeclaration> configParamDeclarations) {
    this.configClass = new LazyValue<>(() -> createConfigBeanClass(configParamDeclarations));
  }

  @Override
  public Object newInstance() {
    Object instance;
    try {
      instance = this.configClass.get().getDeclaredConstructor().newInstance();
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
        | NoSuchMethodException | SecurityException e) {
      throw new MuleRuntimeException(e);
    }
    this.lastBuilt = instance;
    return instance;
  }

  @Override
  public Class<?> getObjectType() {
    return configClass.get();
  }

  public Object getLastBuilt() {
    final Object lastBuilt = this.lastBuilt;
    this.lastBuilt = null;
    return lastBuilt;
  }

  private Class<? extends XmlSdkConnectionProviderWrapper> createConfigBeanClass(List<ParameterDeclaration> paramDeclarations) {
    DynamicType.Builder connectionProviderWrapperClassBuilder = new ByteBuddy()
        .subclass(Object.class, DEFAULT_CONSTRUCTOR);

    for (ParameterDeclaration parameterDeclaration : paramDeclarations) {
      connectionProviderWrapperClassBuilder =
          connectionProviderWrapperClassBuilder.defineProperty(parameterDeclaration.getName(), String.class);
    }

    return connectionProviderWrapperClassBuilder.make().load(multiParentClassLoaderFor(currentThread().getContextClassLoader()),
                                                             CHILD_FIRST)
        .getLoaded();
  }

}
