/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import java.net.Proxy;
import java.util.Objects;

public class VertexWrapper {

  final Object wrappedObject;

  public VertexWrapper(Object o) {
    Objects.requireNonNull(o, "bean should not be null");
    this.wrappedObject = o;
  }

  public Object getWrappedObject() {
    return wrappedObject;
  }

  @Override
  public boolean equals(Object obj) {
    if (wrappedObject.equals(obj)) {
      return true;
    } else if (obj instanceof VertexWrapper) {
      return this.getWrappedObject().hashCode() == ((VertexWrapper) obj).getWrappedObject().hashCode();
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    // todo: proxy has the same hashcode with the wrapped one? check!
    return wrappedObject.hashCode();
  }
}
