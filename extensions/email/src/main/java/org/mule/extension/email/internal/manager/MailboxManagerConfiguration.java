/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.manager;

import org.mule.extension.email.api.EmailAttributes;

import javax.mail.Message;

/**
 * Generic contract for configurations that contains operations for retrieving emails.
 *
 * @since 4.0
 */
public interface MailboxManagerConfiguration {

  /**
   * @return a boolean value that indicates whether the retrieved emails should be opened and read or not.
   */
  boolean isEagerlyFetchContent();

  /**
   * Resolves the {@link EmailAttributes} from a given message for this configuration.
   *
   * @param message the {@link Message} that we want to parse.
   * @return an {@link EmailAttributes} instance from the parsed {@code message}.
   */
  <T extends EmailAttributes> T parseAttributesFromMessage(Message message);
}
