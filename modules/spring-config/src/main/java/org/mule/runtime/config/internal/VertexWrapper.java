/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import java.util.Objects;

public class VertexWrapper {

  Object originalObject;

  public VertexWrapper(Object o) {
    Objects.requireNonNull(o, "bean should not be null");
    this.originalObject = o;
  }

  public Object getOriginalObject() {
    return originalObject;
  }

  @Override // command + n : auto generate shortcut (getter,setter, etc)
  public boolean equals(Object obj) {
    if (originalObject.equals(obj))
      return true;
    else if (obj instanceof VertexWrapper) {
      return this.getOriginalObject().hashCode() == ((VertexWrapper) obj).getOriginalObject().hashCode();
    } else
      return false;
  }
}
