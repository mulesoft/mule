/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.withRefreshToken;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.core.api.util.func.CheckedSupplier;

/**
 * Util class for resolving metadata.
 *
 * @since 4.4.0
 */
public final class MetadataResolverUtils {

  /**
   * Gets the value provided by a {@link CheckedSupplier} taking into account that an OAuth access token refresh may be needed.
   *
   * @param metadataContext  metadata context used by the supplier to provide a value
   * @param metadataSupplier supplier that provides the value
   * @param <T>              the type of value the supplier provider
   * @throws Exception
   */
  public static <T> T resolveWithOAuthRefresh(MetadataContext metadataContext, CheckedSupplier<T> metadataSupplier)
      throws Exception {
    return withRefreshToken(getConnectionProvider(metadataContext), metadataSupplier);
  }

  private static ConnectionProvider getConnectionProvider(MetadataContext metadataContext) {
    return metadataContext instanceof ConnectionProviderAwareMetadataContext
        ? ((ConnectionProviderAwareMetadataContext) metadataContext).getConnectionProvider()
            .orElse(null)
        : null;
  }


}
