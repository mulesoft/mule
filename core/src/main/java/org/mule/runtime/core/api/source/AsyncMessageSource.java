/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.source;

import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.processor.AsyncProcessor;

/**
 * Implemented by sources that receive or generate messages which are to be processed by an {@link AsyncProcessor}. This source
 * interface extends {@link MessageSource} for compatibility and it is up to implementations to determine if they use the
 * injected {@link Processor}, {@link AsyncProcessor} or both.
 *
 * TODO MULE-11250 Replace/migrate uses of MessageSource with Async version or improve co-existence of blocking/async sources
 *
 * @since 4.0
 */
public interface AsyncMessageSource extends MessageSource {

  /**
   * Set the {@link AsyncProcessor} listener on a message source which will be invoked when a message is received or generated.
   *
   * @param listener the listener.
   */
  void setAsyncListener(AsyncProcessor listener);

}
