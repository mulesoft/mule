/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.metadata.MediaType;

import java.util.Optional;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;

/**
 * Indicates the output media type of the annotated component and whether that is strict or not.
 *
 * @since 4.0
 */
public class MediaTypeModelProperty implements ModelProperty {

  private final MediaType mediaType;
  private final boolean strict;

  public MediaTypeModelProperty(String mimeType, boolean strict) {
    mediaType = isBlank(mimeType) ? null : MediaType.parse(mimeType);
    this.strict = strict;
  }

  public Optional<MediaType> getMediaType() {
    return ofNullable(mediaType);
  }

  public boolean isStrict() {
    return strict;
  }

  @Override
  public String getName() {
    return "mediaType";
  }

  @Override
  public boolean isPublic() {
    return false;
  }
}
