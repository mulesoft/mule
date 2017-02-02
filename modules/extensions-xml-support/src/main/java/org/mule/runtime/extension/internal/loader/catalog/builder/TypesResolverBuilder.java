/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.catalog.builder;

import org.mule.runtime.extension.internal.loader.catalog.model.resolver.SingleTypeResolver;
import org.mule.runtime.extension.internal.loader.catalog.model.resolver.TypeResolver;

import java.net.URI;

/**
 * Builder for {@link TypeResolver}.
 * TODO(fernandezlautaro: MULE-11501 this class must be moved to a separate module
 *
 * @since 4.0
 */
public class TypesResolverBuilder {

  private final URI baseUri;
  private String name;
  private String location;

  public TypesResolverBuilder(URI baseUri) {
    this.baseUri = baseUri;
  }

  public void name(String name) {
    this.name = name;
  }

  public void location(String location) {
    this.location = location;
  }

  public TypeResolver build() throws Exception {
    URI schemaURI = baseUri != null ? baseUri.resolve(location) : new URI(location);
    return new SingleTypeResolver(name, schemaURI.toURL());
  }

}
