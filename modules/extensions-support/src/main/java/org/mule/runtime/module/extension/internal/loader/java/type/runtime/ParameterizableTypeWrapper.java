/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.module.extension.api.loader.java.type.ParameterizableTypeElement;

/**
 * Wrapper for any kind of class that can define parameters
 *
 * @since 4.0
 */
public class ParameterizableTypeWrapper extends TypeWrapper implements ParameterizableTypeElement {

  public ParameterizableTypeWrapper(Class aClass, ClassTypeLoader typeLoader) {
    super(aClass, typeLoader);
  }
}
