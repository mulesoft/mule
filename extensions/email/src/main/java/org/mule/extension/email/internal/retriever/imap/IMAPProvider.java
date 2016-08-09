/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.retriever.imap;

import static org.mule.extension.email.internal.EmailProtocol.IMAP;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.IMAP_PORT;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.CONNECTION;

import org.mule.extension.email.internal.retriever.AbstractRetrieverProvider;
import org.mule.extension.email.internal.retriever.RetrieverConnection;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

/**
 * A {@link ConnectionProvider} that returns instances of imap based {@link RetrieverConnection}s.
 *
 * @since 4.0
 */
@Alias("imap")
@DisplayName("IMAP Connection")
public class IMAPProvider extends AbstractRetrieverProvider<RetrieverConnection> {

  /**
   * The port number of the mail server. '143' by default
   */
  @Parameter
  @Optional(defaultValue = IMAP_PORT)
  @Placement(group = CONNECTION, order = 2)
  private String port;

  /**
   * {@inheritDoc}
   */
  @Override
  public RetrieverConnection connect() throws ConnectionException {
    return new RetrieverConnection(IMAP, settings.getUser(), settings.getPassword(), settings.getHost(), port,
                                   getConnectionTimeout(), getReadTimeout(), getWriteTimeout(), getProperties());
  }
}
