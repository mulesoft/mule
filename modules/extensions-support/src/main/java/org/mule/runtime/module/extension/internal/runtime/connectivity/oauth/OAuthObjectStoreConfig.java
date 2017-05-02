/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

/**
 * Groups the sum of all the parameters that a user configured in order to specify
 * in which store should the runtime store the {@link ResourceOwnerOAuthContext} instances
 * obtained through an extension
 *
 * @since 4.0
 */
public final class OAuthObjectStoreConfig {

  private final String objectStoreName;

  public OAuthObjectStoreConfig(String objectStoreName) {
    this.objectStoreName = objectStoreName;
  }

  public String getObjectStoreName() {
    return objectStoreName;
  }
}
