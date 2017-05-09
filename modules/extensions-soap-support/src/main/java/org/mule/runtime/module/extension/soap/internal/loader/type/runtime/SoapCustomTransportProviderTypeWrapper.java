/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.loader.type.runtime;

import static org.mule.runtime.extension.api.util.NameUtils.hyphenize;
import org.mule.runtime.extension.api.soap.SoapTransportProvider;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;

/**
 * {@link TypeWrapper} implementation for classes that implements the {@link SoapTransportProvider} interface.
 *
 * @since 4.0
 */
public class SoapCustomTransportProviderTypeWrapper extends TypeWrapper {

  private static final String TRANSPORT_PROVIDER = "-transport-provider";

  SoapCustomTransportProviderTypeWrapper(Class<?> clazz) {
    super(clazz);
  }

  @Override
  public String getAlias() {
    String hyphenized = hyphenize(super.getAlias());
    return hyphenized.replace("-soap" + TRANSPORT_PROVIDER, "").replace(TRANSPORT_PROVIDER, "").concat(TRANSPORT_PROVIDER);
  }
}
