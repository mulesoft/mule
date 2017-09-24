/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

import org.mule.runtime.core.api.event.CoreEvent;

/**
 * <p>
 * Processes {@link CoreEvent}'s intercepting another listener {@link Processor}. It is the InterceptingMessageProcessor's
 * responsibility to invoke the next {@link Processor}.
 * </p>
 * Although not normal, it is valid for the <i>listener</i> MessageProcessor to be <i>null</i> and implementations should handle
 * this case.
 * 
 * @since 3.0
 */
public interface InterceptingMessageProcessor extends Processor {

  /**
   * Set the MessageProcessor listener that will be invoked when a message is received or generated.
   */
  void setListener(Processor listener);

}
