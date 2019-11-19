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

/**
 * Holds the logic to resolve which is the {@link MediaType} a {@link Result} payload should have.
 *
 * @since 4.2
 */
public class PayloadMediaTypeResolver {

  private Charset defaultEncoding;
  private MediaType defaultMediaType;
  private Charset encoding;
  private MediaType mimeType;

  private MediaType resolvedMimeType;

  /**
   * Creates a new instance
   *
   * @param defaultEncoding     the default encoding used by the system
   * @param defaultMediaType    the default {@link MediaType} to use in case one is not specified
   * @param encoding            encoding to be used
   * @param mimeType            mimeType to be used
   */
  public PayloadMediaTypeResolver(Charset defaultEncoding,
                                  MediaType defaultMediaType,
                                  Charset encoding,
                                  MediaType mimeType) {
    this.defaultEncoding = defaultEncoding;
    this.defaultMediaType = defaultMediaType;
    this.encoding = encoding;
    this.mimeType = mimeType;

    if (mimeType != null && encoding != null) {
      resolvedMimeType = mimeType.withCharset(encoding);
    }
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
    if (resolvedMimeType != null) {
      return resolvedMimeType;
    }


    MediaType mediaType = mimeType;
    if (mediaType == null) {
      mediaType = (MediaType) result.getMediaType().orElse(defaultMediaType);
    }

    Charset existingEncoding;
    if (result.getMediaType().isPresent() && mediaType.getCharset().isPresent()) {
      existingEncoding = mediaType.getCharset().get();
    } else {
      existingEncoding = defaultEncoding;
    }

    return mimeType.withCharset(existingEncoding);
  }
}
