/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api.message;

import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.soap.message.MessageDispatcher;

/**
 * Contract for objects that enables the use of a custom transport executing operations of other plugins by
 * using the {@link ExtensionsClient} to send soap the messages.
 *
 * @since 4.0
 */
public interface CustomTransportConfiguration {

  /**
   * Builds a new {@link MessageDispatcher} using the {@link ExtensionsClient}.
   *
   * @param client the extensions client.
   * @return a new {@link MessageDispatcher}.
   */
  MessageDispatcher buildDispatcher(ExtensionsClient client);
}
