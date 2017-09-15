/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.object;

import org.mule.runtime.core.api.object.AbstractObjectFactory;

import java.util.Map;

/**
 * Creates a new instance of the object on each call.
 */
public class PrototypeObjectFactory extends AbstractObjectFactory {

  /** For Spring only */
  public PrototypeObjectFactory() {
    super();
  }

  public PrototypeObjectFactory(String objectClassName) {
    super(objectClassName);
  }

  public PrototypeObjectFactory(String objectClassName, Map properties) {
    super(objectClassName, properties);
  }

  public PrototypeObjectFactory(Class<?> objectClass) {
    super(objectClass);
  }

  public PrototypeObjectFactory(Class<?> objectClass, Map properties) {
    super(objectClass, properties);
  }

}
