/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.utils;

import org.mule.runtime.api.metadata.resolving.NamedTypeResolver;
import org.mule.runtime.api.metadata.resolving.StaticResolver;
import org.mule.runtime.module.extension.internal.metadata.MuleMetadataTypeResolverAdapter;
import org.mule.sdk.api.metadata.NullMetadataResolver;

public class JavaMetadataTypeResolverUtils {

  public static boolean isNullResolver(Class<?> typeResolverClass) {
    return NullMetadataResolver.class.isAssignableFrom(typeResolverClass) ||
        org.mule.runtime.extension.api.metadata.NullMetadataResolver.class.isAssignableFrom(typeResolverClass);
  }

  public static Class<?> getEnclosingClass(NamedTypeResolver namedTypeResolver) {
    if (namedTypeResolver instanceof MuleMetadataTypeResolverAdapter) {
      return ((MuleMetadataTypeResolverAdapter) namedTypeResolver).getDelegateResolverClass();
    } else {
      return namedTypeResolver.getClass();
    }
  }

  public static boolean isStaticResolver(Class<?> resolverClass) {
    return StaticResolver.class.isAssignableFrom(resolverClass)
        || org.mule.sdk.api.metadata.resolving.StaticResolver.class.isAssignableFrom(resolverClass);
  }

}
