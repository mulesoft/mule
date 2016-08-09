/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.metadata;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.valueOf;
import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;

import org.mule.extension.socket.api.ImmutableSocketAttributes;
import org.mule.extension.socket.api.SocketOperations;
import org.mule.extension.socket.api.config.RequesterConfig;
import org.mule.extension.socket.api.connection.RequesterConnection;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.MetadataAttributesResolver;
import org.mule.runtime.api.metadata.resolving.MetadataKeysResolver;
import org.mule.runtime.api.metadata.resolving.MetadataOutputResolver;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The output metadata only depends on whether
 * {@link SocketOperations#send(RequesterConnection, RequesterConfig, Object, String, String, MuleMessage)} should await a
 * response or not. If no response is needed, the operation metadata should behave like a void operation.
 */
public class SocketMetadataResolver
    implements MetadataOutputResolver<String>, MetadataAttributesResolver<String>, MetadataKeysResolver {

  private final ClassTypeLoader typeLoader =
      ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(getClass().getClassLoader());

  @Override
  public MetadataType getOutputMetadata(MetadataContext metadataContext, String key)
      throws MetadataResolvingException, ConnectionException {
    return valueOf(key) ? BaseTypeBuilder.create(MetadataFormat.JAVA).binaryType().build()
        : BaseTypeBuilder.create(MetadataFormat.JAVA).anyType().build();
  }

  @Override
  public MetadataType getAttributesMetadata(MetadataContext context, String key)
      throws MetadataResolvingException, ConnectionException {
    return valueOf(key) ? typeLoader.load(ImmutableSocketAttributes.class)
        : BaseTypeBuilder.create(MetadataFormat.JAVA).anyType().build();

  }

  @Override
  public Set<MetadataKey> getMetadataKeys(MetadataContext metadataContext)
      throws MetadataResolvingException, ConnectionException {
    return Stream.of(TRUE, FALSE).map(b -> newKey(b.toString()).build()).collect(Collectors.toSet());
  }
}
