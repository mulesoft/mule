/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ocs;

import org.mule.oauth.client.api.state.ResourceOwnerOAuthContext;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsState;
import org.mule.runtime.oauth.api.PlatformManagedOAuthDancer;

import java.util.function.Consumer;

/**
 * Implementation of {@link AbstractPlatformOAuthStateAdapter} that implements {@link ClientCredentialsState}
 *
 * @since 4.3.0
 */
public class PlatformClientCredentialsOAuthStateAdapter extends AbstractPlatformOAuthStateAdapter
    implements ClientCredentialsState {

  public PlatformClientCredentialsOAuthStateAdapter(PlatformManagedOAuthDancer dancer,
                                                    Consumer<ResourceOwnerOAuthContext> onUpdate) {
    super(dancer, onUpdate);
  }
}
