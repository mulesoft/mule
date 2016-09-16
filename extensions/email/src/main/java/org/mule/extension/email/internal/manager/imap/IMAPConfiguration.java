/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.manager.imap;

import org.mule.extension.email.api.attributes.BaseEmailAttributes;
import org.mule.extension.email.api.attributes.ImapEmailAttributes;
import org.mule.extension.email.internal.manager.CommonEmailOperations;
import org.mule.extension.email.internal.manager.MailboxAccessConfiguration;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.connector.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

import javax.mail.Message;

/**
 * Configuration for operations that are performed through the IMAP (Internet Message Access Protocol) protocol.
 *
 * @since 4.0
 */
@Operations({IMAPOperations.class, CommonEmailOperations.class})
@ConnectionProviders({IMAPProvider.class, IMAPSProvider.class})
@Configuration(name = "imap")
@DisplayName("IMAP")
public class IMAPConfiguration implements MailboxAccessConfiguration {

  /**
   * Indicates whether the retrieved emails should be opened and read. The default value is "true".
   */
  @Parameter
  @Optional(defaultValue = "true")
  private boolean eagerlyFetchContent;

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEagerlyFetchContent() {
    return eagerlyFetchContent;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends BaseEmailAttributes> T parseAttributesFromMessage(Message message) {
    return (T) new ImapEmailAttributes(message);
  }
}
