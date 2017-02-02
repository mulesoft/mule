/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.catalog.loader;

import org.mule.runtime.extension.internal.loader.catalog.builder.TypesCatalogBuilder;

/**
 * Holds the context of the current catalog being discovered.
 * TODO(fernandezlautaro: MULE-11501 this class must be moved to a separate module
 *
 * @since 4.0
 */
public class TypesCatalogLoaderContext {

  private final TypesCatalogBuilder typesCatalogBuilder;

  public TypesCatalogLoaderContext(TypesCatalogBuilder typesCatalogBuilder) {
    this.typesCatalogBuilder = typesCatalogBuilder;
  }

  public TypesCatalogBuilder getTypesCatalogBuilder() {
    return typesCatalogBuilder;
  }
}
