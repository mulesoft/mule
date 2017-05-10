/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.loader.type.runtime;

import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;

/**
 * {@link TypeWrapper} Base implementation for classes that are annotated with the SoapTransportProviders annotation.
 *
 * @since 4.0
 */
abstract class SoapComponentWrapper extends TypeWrapper {

  SoapComponentWrapper(Class<?> aClass) {
    super(aClass);
  }
}
