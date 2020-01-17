/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ocs;

import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsState;
import org.mule.runtime.oauth.api.PlatformManagedOAuthDancer;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

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
