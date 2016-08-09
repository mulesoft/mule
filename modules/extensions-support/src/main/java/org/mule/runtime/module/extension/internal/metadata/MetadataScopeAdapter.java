/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import org.mule.runtime.api.metadata.resolving.MetadataAttributesResolver;
import org.mule.runtime.api.metadata.resolving.MetadataContentResolver;
import org.mule.runtime.api.metadata.resolving.MetadataKeysResolver;
import org.mule.runtime.api.metadata.resolving.MetadataOutputResolver;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.introspection.ComponentModel;
import org.mule.runtime.extension.api.introspection.metadata.NullMetadataResolver;

/**
 * Adapter implementation which expands the {@link MetadataScope} to a more descriptive of the developer's metadata declaration
 * for a {@link ComponentModel component}
 *
 * @since 4.0
 */
public final class MetadataScopeAdapter {

  private Class<? extends MetadataKeysResolver> keysResolver = NullMetadataResolver.class;
  private Class<? extends MetadataContentResolver> contentResolver = NullMetadataResolver.class;
  private Class<? extends MetadataOutputResolver> outputResolver = NullMetadataResolver.class;
  private Class<? extends MetadataAttributesResolver> attributesResolver = NullMetadataResolver.class;
  private boolean customScope = false;

  public MetadataScopeAdapter(MetadataScope scope) {
    if (scope != null) {
      this.customScope = true;
      this.keysResolver = scope.keysResolver();
      this.contentResolver = scope.contentResolver();
      this.outputResolver = scope.outputResolver();
      this.attributesResolver = scope.attributesResolver();
    }
  }

  public boolean isCustomScope() {
    return customScope;
  }

  public boolean hasKeysResolver() {
    return !keysResolver.equals(NullMetadataResolver.class);
  }

  public boolean hasContentResolver() {
    return !contentResolver.equals(NullMetadataResolver.class);
  }

  public boolean hasOutputResolver() {
    return !outputResolver.equals(NullMetadataResolver.class);
  }

  public boolean hasAttributesResolver() {
    return !attributesResolver.equals(NullMetadataResolver.class);
  }

  public Class<? extends MetadataKeysResolver> getKeysResolver() {
    return keysResolver;
  }

  public Class<? extends MetadataContentResolver> getContentResolver() {
    return contentResolver;
  }

  public Class<? extends MetadataOutputResolver> getOutputResolver() {
    return outputResolver;
  }

  public Class<? extends MetadataAttributesResolver> getAttributesResolver() {
    return attributesResolver;
  }

}
