/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import org.mule.runtime.api.ioc.ObjectProvider;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.util.Map;
import java.util.Optional;

/**
 * {@link ObjectProvider} implementation of the mule artifact so other DI plugins can access the artifact object.
 * 
 * @since 4.0
 */
public class MuleArtifactObjectProvider extends AbstractAnnotatedObject implements ObjectProvider {

  private final MuleArtifactContext muleArtifactContext;

  MuleArtifactObjectProvider(MuleArtifactContext muleArtifactContext) {
    this.muleArtifactContext = muleArtifactContext;
  }

  @Override
  public Optional<Object> getObject(String name) {
    try {
      Object bean = muleArtifactContext.getBean(name);
      return of(bean);
    } catch (NoSuchBeanDefinitionException e) {
      return empty();
    }
  }

  @Override
  public Optional<Object> getObjectByType(Class<?> objectType) {
    try {
      Object bean = muleArtifactContext.getBean(objectType);
      return of(bean);
    } catch (NoSuchBeanDefinitionException e) {
      return empty();
    }
  }

  @Override
  public Optional<Boolean> isObjectSingleton(String name) {
    try {
      return of(muleArtifactContext.isSingleton(name));
    } catch (NoSuchBeanDefinitionException e) {
      return empty();
    }
  }

  @Override
  public boolean containsObject(String name) {
    return muleArtifactContext.containsBean(name);
  }

  @Override
  public <T> Map<String, T> getObjectsByType(Class<T> type) {
    return muleArtifactContext.getBeansOfType(type);
  }
}
