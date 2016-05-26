/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.socket.internal.metadata;

import static java.lang.Boolean.*;
import static java.util.Arrays.asList;
import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.JavaTypeLoader;
import org.mule.module.socket.api.SocketOperations;
import org.mule.module.socket.api.SocketsExtension;
import org.mule.module.socket.api.connection.RequesterConnection;
import org.mule.module.socket.api.source.ImmutableSocketAttributes;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.MetadataKeysResolver;
import org.mule.runtime.api.metadata.resolving.MetadataOutputResolver;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * The output metadata only depends on whether {@link SocketOperations#send(RequesterConnection, Object, String, String, MuleMessage)}
 * should await a response or not. If no response is needed, the operation metadata should behave like a void operation.
 */
public class SocketMetadataResolver implements MetadataOutputResolver<String>, MetadataKeysResolver
{

    private final JavaTypeLoader javaTypeLoader = new JavaTypeLoader(SocketsExtension.class.getClassLoader());

    @Override
    public MetadataType getOutputMetadata(MetadataContext metadataContext, String key) throws MetadataResolvingException, ConnectionException
    {
        return valueOf(key) ?
               BaseTypeBuilder.create(MetadataFormat.JAVA).arrayType().of().build() :
               BaseTypeBuilder.create(MetadataFormat.JAVA).anyType().build();
    }

    @Override
    public MetadataType getAttributesMetadata(MetadataContext context, String key) throws MetadataResolvingException, ConnectionException
    {
        return valueOf(key) ?
               javaTypeLoader.load(ImmutableSocketAttributes.class) :
               BaseTypeBuilder.create(MetadataFormat.JAVA).anyType().build();
    }

    @Override
    public Set<MetadataKey> getMetadataKeys(MetadataContext metadataContext) throws MetadataResolvingException, ConnectionException
    {
        return asList(TRUE, FALSE).stream()
                .map(b -> newKey(b.toString()).build())
                .collect(Collectors.toSet());
    }
}
