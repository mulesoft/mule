/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.loader;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.soap.internal.loader.type.runtime.SoapExtensionTypeWrapper;

/**
 * Method Factory Pattern implementation that creates {@link ExtensionElement} for a Soap Extension from a {@link Class} that
 * uses the Soap Extensions API.
 *
 * @since 4.0
 */
class SoapExtensionTypeFactory {

  /**
   * Creates a {@link ExtensionElement} from a given {@link Class} that will help to introspect an extension.
   *
   * @param extensionType SOAP {@link Extension} annotated {@link Class}
   * @return an {@link ExtensionElement} wrapping the extension {@link Class} structure
   */

  static SoapExtensionTypeWrapper getSoapExtensionType(Class<?> extensionType) {
    return new SoapExtensionTypeWrapper<>(extensionType);
  }
}
