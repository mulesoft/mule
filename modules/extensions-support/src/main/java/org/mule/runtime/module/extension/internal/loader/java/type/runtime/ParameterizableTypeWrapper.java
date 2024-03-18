/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
