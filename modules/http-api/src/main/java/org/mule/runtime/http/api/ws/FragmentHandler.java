/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.ws;

import org.mule.api.annotation.Experimental;
import org.mule.api.annotation.NoImplement;

import java.io.IOException;
import java.io.InputStream;

/**
 * Allows to channel the information of a multi fragment Web Socket message into an {@link InputStream}.
 * <p>
 * Clients <b>MUST</b> always invoke either {@link #complete()} or {@link #abort()} once the multi fragment
 * message is over.
 *
 * @since 4.1.5 as experimental
 */
@Experimental
@NoImplement
public interface FragmentHandler {

  /**
   * Makes the given {@code data} available at the end of the {@link #getInputStream()}
   *
   * @param data a fragment's payload
   * @return {@code true} if the information could be appended to the stream
   * @throws IOException
   */
  boolean write(byte[] data) throws IOException;

  /**
   * @return The stream on which the fragment data is being consolidated
   */
  InputStream getInputStream();

  /**
   * Clients <b>MUST</b> invoke this method once the last fragment of a message has been received.
   */
  void complete();

  /**
   * Clients <b>MUST</b> invoke this method if any exceptional situation will cause the multi fragment message
   * to never complete, meaning that the last fragment will never arrive.
   */
  void abort();
}
