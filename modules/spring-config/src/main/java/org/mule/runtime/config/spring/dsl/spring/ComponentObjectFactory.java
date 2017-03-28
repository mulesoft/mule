/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.spring;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.component.Component;
import org.mule.runtime.core.api.component.LifecycleAdapterFactory;
import org.mule.runtime.core.api.interceptor.Interceptor;
import org.mule.runtime.core.api.model.EntryPointResolver;
import org.mule.runtime.core.api.model.EntryPointResolverSet;
import org.mule.runtime.core.component.AbstractJavaComponent;
import org.mule.runtime.core.component.DefaultJavaComponent;
import org.mule.runtime.core.api.model.resolvers.DefaultEntryPointResolverSet;
import org.mule.runtime.core.api.model.resolvers.LegacyEntryPointResolverSet;
import org.mule.runtime.core.object.PrototypeObjectFactory;
import org.mule.runtime.core.object.SingletonObjectFactory;
import org.mule.runtime.dsl.api.component.AbstractAnnotatedObjectFactory;
import org.mule.runtime.dsl.api.component.ObjectFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link ObjectFactory} for {@link DefaultJavaComponent} objects.
 *
 * @since 4.0
 */
public class ComponentObjectFactory extends AbstractAnnotatedObjectFactory<Component> {

  protected Class clazz;
  protected org.mule.runtime.core.api.object.ObjectFactory objectFactory;
  protected EntryPointResolverSet entryPointResolverSet = new LegacyEntryPointResolverSet();
  protected EntryPointResolver entryPointResolver;
  protected LifecycleAdapterFactory lifecycleAdapterFactory;
  protected boolean usePrototypeObjectFactory = true;
  protected String staticData;
  protected List<Interceptor> interceptors = new ArrayList<>();

  @Override
  public Component doGetObject() throws Exception {
    if (clazz != null && objectFactory != null) {
      throw new MuleRuntimeException(createStaticMessage("Only one of class attribute or object factory is allowed in a component"));
    }
    Map<Object, Object> properties = new HashMap<>();
    if (staticData != null) {
      properties.put("data", staticData);
    }
    if (clazz != null && usePrototypeObjectFactory) {
      objectFactory = new PrototypeObjectFactory(clazz, properties);
    } else if (clazz != null) {
      objectFactory = new SingletonObjectFactory(clazz, properties);
    }
    if (entryPointResolver != null) {
      entryPointResolverSet = new DefaultEntryPointResolverSet();
      entryPointResolverSet.addEntryPointResolver(entryPointResolver);
    }
    AbstractJavaComponent component = createComponent();
    if (lifecycleAdapterFactory != null) {
      component.setLifecycleAdapterFactory(lifecycleAdapterFactory);
    }
    component.setInterceptors(interceptors);
    component.setAnnotations(this.getAnnotations());
    return component;
  }

  protected AbstractJavaComponent createComponent() {
    DefaultJavaComponent component;
    if (this.objectFactory != null) {
      component = new DefaultJavaComponent(this.objectFactory, this.entryPointResolverSet);
    } else {
      component = new DefaultJavaComponent();
    }
    return component;
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

  public void setLifecycleAdapterFactory(LifecycleAdapterFactory lifecycleAdapterFactory) {
    this.lifecycleAdapterFactory = lifecycleAdapterFactory;
  }

  public void setUsePrototypeObjectFactory(boolean usePrototypeObjectFactory) {
    this.usePrototypeObjectFactory = usePrototypeObjectFactory;
  }

  public void setStaticData(String staticData) {
    this.staticData = staticData;
  }
}
