/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;


import static java.lang.reflect.Proxy.isProxyClass;
import static java.util.Objects.requireNonNull;

/**
 * BeanWrapper helps the comparison/equality check among non-proxy vs. proxy or proxy vs. proxy when building a dependency graph.
 */
public class BeanWrapper {

  private final Object wrappedObject;
  private final String name;
  private final int cachedHashCode;

  public BeanWrapper(String name, Object o) {
    requireNonNull(o, "bean `" + name + "` must not be null");
    this.name = name;
    this.wrappedObject = o;
    this.cachedHashCode = calculateHashCode();
  }

  public Object getWrappedObject() {
    return wrappedObject;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object obj) {
    // same object
    if (this == obj || wrappedObject.equals(obj)) {
      return true;
    }
    if (!(obj instanceof BeanWrapper)) {
      return false;
    }
    // proxy vs. proxy
    if (isProxyClass(this.wrappedObject.getClass()) && isProxyClass(((BeanWrapper) obj).wrappedObject.getClass())) {
      return (this.wrappedObject == ((BeanWrapper) obj).wrappedObject)
          || (this.wrappedObject.hashCode() == ((BeanWrapper) obj).wrappedObject.hashCode());
    }
    // non-proxy vs. proxy
    if (isProxyClass(((BeanWrapper) obj).wrappedObject.getClass())) {
      return ((BeanWrapper) obj).wrappedObject.equals(this.wrappedObject);
    }
    // other cases
    return this.wrappedObject.equals(((BeanWrapper) obj).wrappedObject);
  }

  @Override
  public int hashCode() {
    return cachedHashCode;
  }

  private int calculateHashCode() {
    return wrappedObject.hashCode();
  }


}
