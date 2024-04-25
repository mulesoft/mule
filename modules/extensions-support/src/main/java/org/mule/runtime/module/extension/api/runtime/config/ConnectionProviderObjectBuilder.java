/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.runtime.config;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSetResult;

/**
 * Produces instances of {@link ConnectionProviderModel}
 *
 * @since 4.8
 */
public interface ConnectionProviderObjectBuilder<C> {

  /**
   * Returns a new instance of the {@link ConnectionProvider} with its fields populated.
   *
   * @param result a {@link ResolverSetResult} to inject the fields of the {@link ConnectionProvider}.
   * @return a new instance
   * @throws {@link MuleException}
   */
  public Pair<ConnectionProvider<C>, ResolverSetResult> build(ResolverSetResult result) throws MuleException;
}
