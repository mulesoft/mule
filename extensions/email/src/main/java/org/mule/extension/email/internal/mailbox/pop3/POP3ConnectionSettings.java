/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.mailbox.pop3;

import static org.mule.extension.email.internal.util.EmailConnectorConstants.POP3_PORT;
import org.mule.extension.email.internal.EmailConnectionSettings;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

/**
 * Groups POP3 connection parameters
 *
 * @since 4.0
 */
public final class POP3ConnectionSettings extends EmailConnectionSettings {

  /**
   * The port number of the mail server. '110' by default.
   */
  @Parameter
  @Optional(defaultValue = POP3_PORT)
  @Placement(order = 2)
  private String port;

  @Override
  public String getPort() {
    return port;
  }
}
