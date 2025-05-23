/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.message;

import org.mule.runtime.core.api.event.CoreEvent;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The OutputHandler is a strategy class that is used to defer the writing of the message payload until there is a stream
 * available to write it to.
 */
public interface OutputHandler {

  /**
   * Write the event payload to the stream. Depending on the underlying transport, attachements and message properties may be
   * written to the stream here too.
   *
   * @param event the current event
   * @param out   the output stream to write to
   * @throws IOException in case of error
   */
  void write(CoreEvent event, OutputStream out) throws IOException;
}
