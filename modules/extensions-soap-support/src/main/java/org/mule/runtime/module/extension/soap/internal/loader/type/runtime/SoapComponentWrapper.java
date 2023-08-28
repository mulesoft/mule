/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.soap.internal.loader.type.runtime;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;

/**
 * {@link TypeWrapper} Base implementation for classes that are annotated with the SoapTransportProviders annotation.
 *
 * @since 4.0
 */
abstract class SoapComponentWrapper extends TypeWrapper {

  SoapComponentWrapper(Class<?> aClass, ClassTypeLoader typeLoader) {
    super(aClass, typeLoader);
  }
}
