/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import org.mule.oauth.client.api.state.ResourceOwnerOAuthContext;

/**
 * Groups the sum of all the parameters that a user configured in order to specify in which store should the runtime store the
 * {@link ResourceOwnerOAuthContext} instances obtained through an extension
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
