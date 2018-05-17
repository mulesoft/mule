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
 * @since 4.2
 */
public class MediaTypeResolver {

  private Charset defaultEncoding;
  private MediaType defaultMediaType;
  private Optional<String> encoding;
  private Optional<String> mimeType;

  public MediaTypeResolver(Charset defaultEncoding, MediaType defaultMediaType, Optional<String> encoding,
                           Optional<String> mimeType) {
    this.defaultEncoding = defaultEncoding;
    this.defaultMediaType = defaultMediaType;
    this.encoding = encoding;
    this.mimeType = mimeType;
  }

  public Result resolve(Result result) {
    return Result.builder()
        .attributesMediaType((MediaType) result.getAttributesMediaType().orElse(null))
        .attributes(result.getAttributes().orElse(null))
        .output(result.getOutput())
        .mediaType(resolveMediaType(result))
        .build();
  }

  private MediaType resolveMediaType(Result value) {
    Charset existingEncoding = defaultEncoding;
    MediaType mediaType = defaultMediaType;
    final Optional<MediaType> optionalMediaType = ((Result) value).getMediaType();
    if (optionalMediaType.isPresent()) {
      mediaType = optionalMediaType.get();
      if (mediaType.getCharset().isPresent()) {
        existingEncoding = mediaType.getCharset().get();
      }
    }


    if (mediaType == null) {
      mediaType = ANY;
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
