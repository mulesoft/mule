/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.HasOutputModel;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;

import java.util.Optional;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.metadata.MediaType.ANY;

public class MediaTypeUtils {

  public static MediaType getDefaultMediaType(ComponentModel componentModel) {
    Optional<MediaTypeModelProperty> mediaTypeModelProperty = componentModel.getModelProperty(MediaTypeModelProperty.class);
    if (mediaTypeModelProperty.isPresent() && mediaTypeModelProperty.get().getMediaType().isPresent()) {
      return mediaTypeModelProperty.get().getMediaType().get();
    }
    if (componentModel instanceof HasOutputModel) {
      MetadataType output = ((HasOutputModel) componentModel).getOutput().getType();
      return JAVA.equals(output.getMetadataFormat()) && output instanceof ObjectType
          ? MediaType.APPLICATION_JAVA
          : getMediaTypeFromMetadataType(output);
    }
    return ANY;
  }

  private static MediaType getMediaTypeFromMetadataType(MetadataType output) {
    if (output.getMetadataFormat() != null && output.getMetadataFormat().getValidMimeTypes().size() == 1) {
      return MediaType.parse(output.getMetadataFormat().getValidMimeTypes().iterator().next());
    } else {
      return ANY;
    }
  }

}
