/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.object;

import org.mule.runtime.core.api.object.AbstractObjectFactory;

import java.util.Map;

/**
 * Creates a new instance of the object on each call.
 * 
 * @deprecated this will be removed in next major version
 */
@Deprecated
public final class PrototypeObjectFactory extends AbstractObjectFactory {

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
