/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.object;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.object.AbstractObjectFactory;

import java.util.Map;

/**
 * Creates an instance of the object once and then always returns the same instance.
 * 
 * @deprecated this will be removed in next major version
 */
@Deprecated
public final class SingletonObjectFactory extends AbstractObjectFactory {

  private Object instance;

  /**
   * For Spring only
   */
  public SingletonObjectFactory() {
    super();
  }

  public SingletonObjectFactory(String objectClassName) {
    super(objectClassName);
  }

  public SingletonObjectFactory(String objectClassName, Map properties) {
    super(objectClassName, properties);
  }

  public SingletonObjectFactory(Class objectClass) {
    super(objectClass);
  }

  public SingletonObjectFactory(Class<?> objectClass, Map properties) {
    super(objectClass, properties);
  }

  /**
   * Create the singleton based on a previously created object.
   */
  public SingletonObjectFactory(Object instance) {
    super(instance.getClass());
    this.instance = instance;
  }

  @Override
  public void dispose() {
    instance = null;
    super.dispose();
  }

  /**
   * Always returns the same instance of the object.
   * 
   * @param muleContext
   */
  @Override
  public Object getInstance(MuleContext muleContext) throws Exception {
    if (instance == null) {
      try {
        instance = super.getInstance(muleContext);
      } catch (Exception e) {
        throw new InitialisationException(e, this);
      }
    }
    return instance;
  }

  @Override
  public Class<?> getObjectClass() {
    if (instance != null) {
      return instance.getClass();
    } else {
      return super.getObjectClass();
    }
  }

  @Override
  public boolean isSingleton() {
    return true;
  }
}
