/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.mediatype;

import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.nio.charset.Charset;
import java.util.Optional;

import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.api.metadata.MediaTypeUtils.parseCharset;

/**
 * Holds the logic to resolve which is the {@link MediaType} a {@link Result} payload should have.
 * 
 * @since 4.2
 */
public class PayloadMediaTypeResolver {

  private Charset defaultEncoding;
  private MediaType defaultMediaType;
  private Optional<String> encoding;
  private Optional<String> mimeType;

  /**
   * Creates a new instance
   * 
   * @param defaultEncoding     the default encoding used by the system
   * @param defaultMediaType    the default {@link MediaType} to use in case one is not specified
   * @param encoding            {@link Optional} encoding to be used if present
   * @param mimeType            {@link Optional} mimeType to be used if present
   */
  public PayloadMediaTypeResolver(Charset defaultEncoding, MediaType defaultMediaType, Optional<String> encoding,
                                  Optional<String> mimeType) {
    this.defaultEncoding = defaultEncoding;
    this.defaultMediaType = defaultMediaType;
    this.encoding = encoding;
    this.mimeType = mimeType;
  }

  /**
   * 
   * @param result  {@link Result} whose payload {@link MediaType} has to be resolved
   * @return        {@link Result} with the payload {@link MediaType} resolved
   */
  public Result resolve(Result result) {
    return new MediaTypeDecoratedResult(result, resolveMediaType(result));
  }

  private MediaType resolveMediaType(Result result) {
    Charset existingEncoding = defaultEncoding;
    MediaType mediaType = (MediaType) result.getMediaType().orElse(defaultMediaType);
    if (result.getMediaType().isPresent() && mediaType.getCharset().isPresent()) {
      existingEncoding = mediaType.getCharset().get();
    }
    if (mimeType.isPresent()) {
      mediaType = MediaType.parse(mimeType.get());
    }
    if (encoding.isPresent()) {
      mediaType = mediaType.withCharset(parseCharset(encoding.get()));
    } else {
      mediaType = mediaType.withCharset(existingEncoding);
    }
    return mediaType;
  }
}
