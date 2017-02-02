/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.mailbox;

import org.mule.extension.email.internal.AbstractEmailConnection;
import org.mule.extension.email.internal.AbstractEmailConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;

/**
 * Generic contract for all email retriever {@link ConfigurationProvider}s.
 *
 * @since 4.0
 */
// TODO: Change generic signature for a more specific one. MULE-9874
public abstract class AbstractMailboxConnectionProvider<C extends AbstractEmailConnection>
    extends AbstractEmailConnectionProvider<C> implements PoolingConnectionProvider<C> {

  /**
   * {@inheritDoc}
   */
  @Override
  public void disconnect(C connection) {
    connection.disconnect();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectionValidationResult validate(C connection) {
    return connection.validate();
  }

  @Override
  public void onBorrow(C connection) {
    if (connection instanceof MailboxConnection) {
      ((MailboxConnection) connection).closeFolder(false);
    }
  }
}
