/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.mailbox.pop3;

import static org.mule.extension.email.internal.EmailProtocol.POP3S;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
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
 * A {@link ConnectionProvider} that returns instances of pop3s (secured) based {@link MailboxConnection}s.
 * <p>
 * The returned connection is secured by TLS.
 *
 * @since 4.0
 */
@Alias("pop3s")
@DisplayName("POP3S Connection")
public class POP3SProvider extends AbstractMailboxConnectionProvider<MailboxConnection> implements Initialisable {

  @ParameterGroup(ParameterGroup.CONNECTION)
  private POP3SConnectionSettings connectionSettings;

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
    return new MailboxConnection(POP3S,
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
