/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.sender;

import static org.mule.extension.email.internal.EmailProtocol.SMTP;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.SMTP_PORT;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.CONNECTION;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

/**
 * A {@link ConnectionProvider} that returns instances of smtp based {@link SenderConnection}s.
 *
 * @since 4.0
 */
@Alias("smtp")
@DisplayName("SMTP Connection")
public class SMTPProvider extends AbstractSenderProvider {

  /**
   * The port number of the mail server. '25' by default.
   */
  @Parameter
  @Optional(defaultValue = SMTP_PORT)
  @Placement(group = CONNECTION, order = 2)
  private String port;

  /**
   * {@inheritDoc}
   */
  @Override
  public SenderConnection connect() throws ConnectionException {
    return new SenderConnection(SMTP, settings.getUser(), settings.getPassword(), settings.getHost(), port,
                                getConnectionTimeout(), getReadTimeout(), getWriteTimeout(), getProperties());
  }
}
