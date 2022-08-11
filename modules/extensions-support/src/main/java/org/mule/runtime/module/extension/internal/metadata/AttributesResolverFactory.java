/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.metadata;

import static java.lang.String.format;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.sdk.api.metadata.NullMetadataResolver;

/**
 * Factory of {@link AttributesTypeResolver}
 */
public class AttributesResolverFactory {

  public static AttributesTypeResolver from(Object resolver) {
    checkArgument(resolver != null, "Cannot create null resolver");

    if (resolver instanceof AttributesTypeResolver) {
      return (AttributesTypeResolver) resolver;
    } else if (resolver instanceof NullMetadataResolver) {
      return new org.mule.runtime.extension.api.metadata.NullMetadataResolver();
    } else if (resolver instanceof org.mule.sdk.api.metadata.resolving.AttributesTypeResolver) {
      return new MuleAttributesTypeResolverAdapter((org.mule.sdk.api.metadata.resolving.AttributesTypeResolver) resolver);
    } else {
      throw new IllegalArgumentException(format("Resolver of class '%s' is neither a '%s' nor a '%s'",
                                                resolver.getClass().getName(),
                                                OutputTypeResolver.class.getName(),
                                                org.mule.sdk.api.metadata.resolving.OutputTypeResolver.class.getName()));
    }
  }
}
