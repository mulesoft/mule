/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.metadata.MediaType;

/**
 * Indicates the output media type of the annotated component and whether that is strict or not.
 *
 * @since 4.0
 */
public class MediaTypeModelProperty implements ModelProperty {

  private final MediaType mediaType;
  private final boolean strict;

  public MediaTypeModelProperty(String mimeType, boolean strict) {
    mediaType = MediaType.parse(mimeType);
    this.strict = strict;
  }

  public MediaType getMediaType() {
    return mediaType;
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
