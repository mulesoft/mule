/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.query;

import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

public class NativeQueryOutputResolver implements OutputTypeResolver<String> {

  public static final String NATIVE_QUERY = "SELECT FIELDS: field-id FROM TYPE: Circle DO WHERE field-diameter < 18";

  @Override
  public String getCategoryName() {
    return "MetadataExtensionEntityResolver";
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, String key)
      throws MetadataResolvingException, ConnectionException {

    if (!key.equals(NATIVE_QUERY)) {
      throw new IllegalArgumentException("Native Query Key was not the expected one");
    }

    final ObjectTypeBuilder objectType = context.getTypeBuilder().objectType();
    objectType.addField().key("id").value().numberType();
    return objectType.build();
  }
}
