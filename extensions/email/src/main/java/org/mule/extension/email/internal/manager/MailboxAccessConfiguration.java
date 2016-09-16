/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.manager;

import org.mule.extension.email.api.attributes.BaseEmailAttributes;

import javax.mail.Message;

/**
 * Generic contract for configurations that contains operations for retrieving and managing emails in a mailbox.
 *
 * @since 4.0
 */
public interface MailboxAccessConfiguration {

  /**
   * @return a boolean value that indicates whether the retrieved emails should be opened and read or not.
   */
  boolean isEagerlyFetchContent();

  /**
   * Resolves the {@link BaseEmailAttributes} from a given message for this configuration.
   *
   * @param message the {@link Message} that we want to parse.
   * @return an {@link BaseEmailAttributes} instance from the parsed {@code message}.
   */
  <T extends BaseEmailAttributes> T parseAttributesFromMessage(Message message);
}
