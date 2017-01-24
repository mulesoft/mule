/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.query;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import org.mule.metadata.api.builder.ArrayTypeBuilder;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

public class NativeQueryOutputResolver implements OutputTypeResolver<String> {

  public static final String NATIVE_QUERY = "SELECT FIELDS: field-id FROM TYPE: Circle DO WHERE field-diameter < 18";
  public static final MetadataType CIRCLE_TYPE;
  static {
    final ArrayTypeBuilder arrayType = BaseTypeBuilder.create(JAVA).arrayType();
    final ObjectTypeBuilder objectType = arrayType.of().objectType();
    objectType.addField().key("id").value().numberType();
    CIRCLE_TYPE = arrayType.build();
  }

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

    return CIRCLE_TYPE;
  }
}
