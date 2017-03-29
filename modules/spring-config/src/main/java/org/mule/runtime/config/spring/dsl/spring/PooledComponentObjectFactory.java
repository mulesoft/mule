/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.spring;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.core.api.component.Component;
import org.mule.runtime.core.api.component.LifecycleAdapterFactory;
import org.mule.runtime.core.api.interceptor.Interceptor;
import org.mule.runtime.core.api.model.EntryPointResolver;
import org.mule.runtime.core.api.model.EntryPointResolverSet;
import org.mule.runtime.core.component.AbstractJavaComponent;
import org.mule.runtime.core.component.PooledJavaComponent;
import org.mule.runtime.core.api.model.resolvers.DefaultEntryPointResolverSet;
import org.mule.runtime.core.object.PrototypeObjectFactory;
import org.mule.runtime.dsl.api.component.AbstractAnnotatedObjectFactory;
import org.mule.runtime.dsl.api.component.ObjectFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link ObjectFactory} for {@link PooledJavaComponent} objects.
 *
 * @since 4.0
 */
public class PooledComponentObjectFactory extends AbstractAnnotatedObjectFactory<Component> {

  protected Class clazz;
  protected org.mule.runtime.core.api.object.ObjectFactory objectFactory = new PrototypeObjectFactory();
  protected EntryPointResolverSet entryPointResolverSet;
  protected EntryPointResolver entryPointResolver;
  protected PoolingProfile poolingProfile = new PoolingProfile();
  protected LifecycleAdapterFactory lifecycleAdapterFactory;
  protected List<Interceptor> interceptors = new ArrayList<>();

  @Override
  public Component doGetObject() throws Exception {
    if (clazz != null) {
      objectFactory = new PrototypeObjectFactory(clazz);
    }
    if (entryPointResolver != null) {
      entryPointResolverSet = new DefaultEntryPointResolverSet();
      entryPointResolverSet.addEntryPointResolver(entryPointResolver);
    }
    AbstractJavaComponent pooledJavaComponent = createComponent();
    pooledJavaComponent.setInterceptors(interceptors);
    pooledJavaComponent.setAnnotations(getAnnotations());
    return pooledJavaComponent;
  }

  protected AbstractJavaComponent createComponent() {
    PooledJavaComponent pooledJavaComponent;
    if (objectFactory != null) {
      pooledJavaComponent = new PooledJavaComponent(objectFactory, poolingProfile, entryPointResolverSet);
      pooledJavaComponent.setLifecycleAdapterFactory(lifecycleAdapterFactory);
    } else {
      pooledJavaComponent = new PooledJavaComponent();
    }
    return pooledJavaComponent;
  }

  public void setInterceptors(List<Interceptor> interceptors) {
    this.interceptors = interceptors;
  }

  public void setClazz(Class clazz) {
    this.clazz = clazz;
  }

  public void setObjectFactory(org.mule.runtime.core.api.object.ObjectFactory objectFactory) {
    this.objectFactory = objectFactory;
  }

  public void setEntryPointResolverSet(EntryPointResolverSet entryPointResolverSet) {
    this.entryPointResolverSet = entryPointResolverSet;
  }

  public void setEntryPointResolver(EntryPointResolver entryPointResolver) {
    this.entryPointResolver = entryPointResolver;
  }

  public void setPoolingProfile(PoolingProfile poolingProfile) {
    this.poolingProfile = poolingProfile;
  }

  public void setLifecycleAdapterFactory(LifecycleAdapterFactory lifecycleAdapterFactory) {
    this.lifecycleAdapterFactory = lifecycleAdapterFactory;
  }
}
