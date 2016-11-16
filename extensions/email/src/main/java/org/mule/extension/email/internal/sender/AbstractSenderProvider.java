/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.sender;

import org.mule.extension.email.internal.AbstractEmailConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;

/**
 * Abstract implementation of a {@link PoolingConnectionProvider} for email sender connection providers.
 *
 * @since 4.0
 */
public abstract class AbstractSenderProvider extends AbstractEmailConnectionProvider<SenderConnection>
    implements PoolingConnectionProvider<SenderConnection> {

  /**
   * {@inheritDoc}
   */
  @Override
  public void disconnect(SenderConnection connection) {
    connection.disconnect();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectionValidationResult validate(SenderConnection connection) {
    return connection.validate();
  }
}
