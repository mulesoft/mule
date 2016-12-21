/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.metadata;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;

import org.mule.extension.socket.api.ImmutableSocketAttributes;
import org.mule.extension.socket.api.SocketOperations;
import org.mule.extension.socket.api.config.RequesterConfig;
import org.mule.extension.socket.api.connection.RequesterConnection;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;

/**
 * The output metadata only depends on whether
 * {@link SocketOperations#send(RequesterConnection, RequesterConfig, Object, String, boolean, Message)} should await a
 * response or not. If no response is needed, the operation metadata should behave like a void operation.
 */
public class SocketMetadataResolver
    implements OutputTypeResolver<Boolean>, AttributesTypeResolver<Boolean> {

  private final ClassTypeLoader typeLoader =
      ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(getClass().getClassLoader());

  @Override
  public String getCategoryName() {
    return "SocketCategory";
  }

  @Override
  public String getResolverName() {
    return "SocketMetadataResolver";
  }

  @Override
  public MetadataType getOutputType(MetadataContext metadataContext, Boolean key)
      throws MetadataResolvingException, ConnectionException {
    return key ? BaseTypeBuilder.create(JAVA).binaryType().build()
        : BaseTypeBuilder.create(JAVA).anyType().build();
  }

  @Override
  public MetadataType getAttributesType(MetadataContext context, Boolean key)
      throws MetadataResolvingException, ConnectionException {
    return key ? typeLoader.load(ImmutableSocketAttributes.class)
        : BaseTypeBuilder.create(JAVA).anyType().build();
  }
}
