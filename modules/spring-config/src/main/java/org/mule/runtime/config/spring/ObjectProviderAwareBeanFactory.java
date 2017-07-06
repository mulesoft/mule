/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.ioc.ObjectProvider;
import org.mule.runtime.core.api.util.func.CheckedSupplier;

import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * {@link org.springframework.beans.factory.ListableBeanFactory} implementation that will resolve beans using a list of
 * {@link ObjectProvider}s if it was not able to find a bean on itself.
 * 
 * @since 4.0
 */
public class ObjectProviderAwareBeanFactory extends DefaultListableBeanFactory {

  private List<ObjectProvider> objectProviders;

  public ObjectProviderAwareBeanFactory(BeanFactory parentBeanFactory) {
    super(parentBeanFactory);
  }

  public void setObjectProviders(List<ObjectProvider> objectProviders) {
    this.objectProviders = objectProviders;
  }

  @Override
  public <T> T getBean(Class<T> requiredType) throws BeansException {
    boolean doNotFail = false;
    try {
      T bean = super.getBean(requiredType);
      if (bean == null) {
        doNotFail = true;
        throw new NoSuchBeanDefinitionException(requiredType);
      }
      return bean;
    } catch (NoSuchBeanDefinitionException e) {
      Optional<Object> objectFound =
          objectProviders.stream().map(objectProvider -> objectProvider.getObjectByType(requiredType))
              .filter(valueOptional -> valueOptional.isPresent())
              .map(valueOptional -> valueOptional.get())
              .findFirst();
      return doNotFail ? null : (T) objectFound.orElseThrow(() -> e);
    }
  }

  @Override
  public Object getBean(String name) throws BeansException {
    boolean doNotFail = false;
    try {
      Object bean = super.getBean(name);
      if (bean == null) {
        doNotFail = true;
        throw new NoSuchBeanDefinitionException(name);
      }
      return bean;
    } catch (NoSuchBeanDefinitionException e) {
      Optional<Object> objectFound =
          objectProviders.stream().map(objectProvider -> objectProvider.getObject(name))
              .filter(valueOptional -> valueOptional.isPresent())
              .map(valueOptional -> valueOptional.get())
              .findFirst();
      return doNotFail ? null : objectFound.orElseThrow(() -> e);
    }
  }

  @Override
  public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
    return doWithFallbackInObjectProvider(() -> super.isSingleton(name), objectProvider -> {
      try {
        return objectProvider.isObjectSingleton(name);
      } catch (NoSuchBeanDefinitionException e) {
        return empty();
      }
    }).orElse(false);
  }

  @Override
  public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
    ImmutableMap.Builder<String, T> builder = ImmutableMap.builder();
    for (ObjectProvider objectProvider : objectProviders) {
      builder.putAll(objectProvider.getObjectsByType(type));
    }
    builder.putAll(super.getBeansOfType(type));
    return builder.build();
  }


  private <T> Optional<T> doWithFallbackInObjectProvider(CheckedSupplier<T> thisSupplier,
                                                         Function<ObjectProvider, Optional<T>> fallbackFunction)
      throws BeansException {
    try {
      return of(thisSupplier.get());
    } catch (Exception e) {
      try {
        Optional<Optional<T>> firstValue = objectProviders.stream().map(objectProvider -> fallbackFunction.apply(objectProvider))
            .filter(Optional::isPresent).findFirst();
        return firstValue.orElseThrow(() -> e);
      } catch (BeansException beansException) {
        throw beansException;
      } catch (Exception e2) {
        throw new MuleRuntimeException(e2);
      }
    }
  }
}
