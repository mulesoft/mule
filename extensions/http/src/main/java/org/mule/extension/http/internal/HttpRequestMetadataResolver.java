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

import org.mule.extension.http.api.HttpMetadataKey;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.UnionTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.core.model.ParameterMap;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles all metadata related operations for HTTP requests.
 *
 * @since 4.0
 */
public class HttpRequestMetadataResolver implements OutputTypeResolver<HttpMetadataKey> {

  private static final ClassTypeLoader TYPE_LOADER = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  private Map<HttpMetadataKey, MetadataType> types = new HashMap<>();

  public HttpRequestMetadataResolver() {
    // Create all MetadataTypes and store them by key using a friendly name
    MetadataType streamType = TYPE_LOADER.load(InputStream.class);
    types.put(STREAM, streamType);
    MetadataType multiPartType = TYPE_LOADER.load(MultiPartPayload.class);
    types.put(MULTIPART, multiPartType);
    MetadataType formPart = TYPE_LOADER.load(ParameterMap.class);
    types.put(FORM, formPart);
    //Create default union type
    UnionTypeBuilder builder = new BaseTypeBuilder(JAVA).unionType();
    types.values().forEach(builder::of);
    types.put(ANY, builder.build());
  }

  @Override
  public String getCategoryName() {
    return "HttpCategory";
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, HttpMetadataKey key)
      throws MetadataResolvingException, ConnectionException {
    return types.get(key);
  }
}
