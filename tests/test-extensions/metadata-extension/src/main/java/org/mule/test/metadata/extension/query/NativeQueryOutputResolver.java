/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.query;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.MetadataOutputResolver;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.test.metadata.extension.model.animals.Bear;

public class NativeQueryOutputResolver implements MetadataOutputResolver<String> {

  public static final String NATIVE_QUERY = "SELECT FIELDS: field-id FROM TYPE: Circle DO WHERE field-diameter < 18";
  private static final ClassTypeLoader LOADER = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  @Override
  public MetadataType getOutputMetadata(MetadataContext context, String key)
      throws MetadataResolvingException, ConnectionException {

    if (!key.equals(NATIVE_QUERY)) {
      throw new IllegalArgumentException("Native Query Key was not the expected one");
    }

    return LOADER.load(Bear.class);
  }
}
