/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api;

import static org.mule.extension.email.api.EmailMetadataKey.ANY;
import static org.mule.extension.email.api.EmailMetadataKey.MULTIPART;
import static org.mule.extension.email.api.EmailMetadataKey.STRING;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.UnionTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles all metadata related to ENAIL list operations.
 *
 * @since 4.0
 */
public class EmailMetadataResolver implements OutputTypeResolver<EmailMetadataKey> {

  private static final ClassTypeLoader TYPE_LOADER = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  private Map<EmailMetadataKey, MetadataType> types = new HashMap<>();

  public EmailMetadataResolver() {
    // Create all MetadataTypes and store them by key using a friendly name
    MetadataType multiPartType = TYPE_LOADER.load(MultiPartPayload.class);
    types.put(MULTIPART, multiPartType);
    MetadataType stringType = TYPE_LOADER.load(String.class);
    types.put(STRING, stringType);

    // Create default union type
    UnionTypeBuilder builder = new BaseTypeBuilder(JAVA).unionType();
    types.values().forEach(builder::of);
    types.put(ANY, builder.build());
  }

  @Override
  public String getCategoryName() {
    return "EmailCategory";
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, EmailMetadataKey key)
      throws MetadataResolvingException, ConnectionException {
    return types.get(key);
  }

}
