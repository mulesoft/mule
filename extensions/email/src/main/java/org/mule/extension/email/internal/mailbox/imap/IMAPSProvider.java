/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.mailbox.imap;

import static org.mule.extension.email.internal.EmailProtocol.IMAPS;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.extension.api.annotation.param.ParameterGroup.CONNECTION;
import org.mule.extension.email.internal.mailbox.AbstractMailboxConnectionProvider;
import org.mule.extension.email.internal.mailbox.MailboxConnection;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

/**
 * A {@link ConnectionProvider} that returns instances of imaps (secure) based {@link MailboxConnection}s.
 * <p>
 * The returned connection is secured by TLS.
 *
 * @since 4.0
 */
@Alias("imaps")
@DisplayName("IMAPS Connection")
public class IMAPSProvider extends AbstractMailboxConnectionProvider<MailboxConnection> implements Initialisable {

  @ParameterGroup(CONNECTION)
  private IMAPSConnectionSettings connectionSettings;

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(connectionSettings.getTlsContextFactory());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MailboxConnection connect() throws ConnectionException {
    return new MailboxConnection(IMAPS,
                                 connectionSettings.getUser(),
                                 connectionSettings.getPassword(),
                                 connectionSettings.getHost(),
                                 connectionSettings.getPort(),
                                 getConnectionTimeout(),
                                 getReadTimeout(),
                                 getWriteTimeout(),
                                 getProperties(),
                                 connectionSettings.getTlsContextFactory());
  }
}
