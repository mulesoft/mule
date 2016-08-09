/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.component;

import org.mule.runtime.core.api.model.EntryPointResolverSet;
import org.mule.runtime.core.api.object.ObjectFactory;
import org.mule.runtime.core.component.PooledJavaComponent;
import org.mule.runtime.core.object.PrototypeObjectFactory;
import org.mule.runtime.core.object.SingletonObjectFactory;

/**
 * <code>JavaComponent</code> is a Java {@link Component} implementation used to invoke Java component implementations. A
 * <code>JavaComponent</code> uses an {@link ObjectFactory} to specify the object instance's source and allows for singleton and
 * prototype implementations to be used along with other custom {@link ObjectFactory} that allow component instances to be
 * obtained from containers such as Spring.
 */
public interface JavaComponent extends Component {

  /**
   * A {@link JavaComponent} can have a custom entry-point resolver for its own object. By default this is null. When set this
   * resolver will override the resolver on the model
   * 
   * @return Null is a resolver set has not been set otherwise the resolver to use on this service
   */
  EntryPointResolverSet getEntryPointResolverSet();

  /**
   * A {@link JavaComponent} can have a custom entry-point resolver for its own object. By default this is null. When set this
   * resolver will override the resolver on the model
   */
  void setEntryPointResolverSet(EntryPointResolverSet entryPointResolverSet);

  /**
   * The object factory used to obtain the component object instance. Mule core provides two implementations:
   * {@link SingletonObjectFactory} and {@link PrototypeObjectFactory}.<br/>
   * The spring-config module provides an {@link ObjectFactory} implementation that delegates to spring. There is no
   * PooledObjectFactory, the {@link PooledJavaComponent} should be used for pooling.
   * 
   * @param objectFactory
   */
  void setObjectFactory(ObjectFactory objectFactory);

  ObjectFactory getObjectFactory();

  Class<?> getObjectType();

  /**
   * Returns the factory used create life-cycle adaptors that are used to wrap component instance.
   */
  LifecycleAdapterFactory getLifecycleAdapterFactory();

  /**
   * Sets the factory used create life-cycle adaptors that are used to wrap component instance.
   * 
   * @param lifecycleAdaptor
   */
  void setLifecycleAdapterFactory(LifecycleAdapterFactory lifecycleAdaptor);
}
