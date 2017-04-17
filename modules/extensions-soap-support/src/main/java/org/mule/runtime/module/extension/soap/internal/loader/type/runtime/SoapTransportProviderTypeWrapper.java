/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.loader.type.runtime;

import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;

/**
 * {@link TypeWrapper} implementation for classes that implements the Soap Transport interface.
 *
 * @since 4.0
 */
class SoapTransportProviderTypeWrapper extends TypeWrapper {

  SoapTransportProviderTypeWrapper(Class<?> clazz) {
    super(clazz);
  }
}
