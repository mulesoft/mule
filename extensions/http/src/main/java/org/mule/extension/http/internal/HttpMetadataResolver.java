/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal;

import static org.mule.extension.http.api.HttpMetadataKey.ANY;
import static org.mule.extension.http.api.HttpMetadataKey.FORM;
import static org.mule.extension.http.api.HttpMetadataKey.MULTIPART;
import static org.mule.extension.http.api.HttpMetadataKey.STREAM;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.UnionTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.MetadataKeysResolver;
import org.mule.runtime.api.metadata.resolving.MetadataOutputResolver;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.module.http.internal.ParameterMap;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Handles all metadata related operations for HTTP requests.
 *
 * @since 4.0
 */
public class HttpMetadataResolver implements MetadataKeysResolver, MetadataOutputResolver<String> {

  private static final ClassTypeLoader TYPE_LOADER = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  private Map<String, MetadataType> types = new HashMap<>();
  private Set<MetadataKey> keys = new HashSet<>();

  public HttpMetadataResolver() {
    // Create all MetadataTypes and store them by key using a friendly name
    MetadataType streamType = TYPE_LOADER.load(InputStream.class);
    types.put(STREAM.toString(), streamType);
    MetadataType multiPartType = TYPE_LOADER.load(MultiPartPayload.class);
    types.put(MULTIPART.toString(), multiPartType);
    MetadataType formPart = TYPE_LOADER.load(ParameterMap.class);
    types.put(FORM.toString(), formPart);
    //Create default union type
    UnionTypeBuilder builder = new BaseTypeBuilder<>(JAVA).unionType();
    types.values().forEach(builder::of);
    types.put(ANY.toString(), builder.build());
    // Create MetadataKeys
    types.keySet().forEach(aKey -> keys.add(newKey(aKey).build()));
  }

  @Override
  public String getCategoryName() {
    return "HttpCategory";
  }

  @Override
  public Set<MetadataKey> getMetadataKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
    return keys;
  }

  @Override
  public MetadataType getOutputMetadata(MetadataContext context, String key)
      throws MetadataResolvingException, ConnectionException {
    return types.get(key);
  }

}
