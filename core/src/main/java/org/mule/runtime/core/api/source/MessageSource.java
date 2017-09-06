/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.source;

import static org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy.WAIT;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.processor.Processor;

/**
 * Implemented by objects that receives or generates messages which are then processed by a {@link Processor}.
 *
 * @since 3.0
 */
public interface MessageSource extends Component {

  /**
   * Set the {@link Processor} listener on a message source which will be invoked when a message is received or generated.
   */
  void setListener(Processor listener);

  /**
   * The {@link BackPressureStrategy} to be used by {@link org.mule.runtime.core.api.construct.Pipeline} implements to handle
   * overload.
   *
   * @return the {@link BackPressureStrategy} to use.
   */
  default BackPressureStrategy getBackPressureStrategy() {
    return WAIT;
  }

  /**
   * Strategy used by a {@link org.mule.runtime.core.api.construct.Pipeline} to respond to and overload situation where the
   * {@link org.mule.runtime.core.api.construct.Pipeline} is unable to process a new {@link org.mule.runtime.api.event.Event} due
   * to, for example, all available threads being busy.
   */
  enum BackPressureStrategy {

    /**
     * On back-pressure fail by returning an {@code OVERLOAD} {@link org.mule.runtime.api.message.Error} to the
     * {@link MessageSource}.
     */
    FAIL,

    /**
     * On back-pressure block the current thread and wait until the {@link org.mule.runtime.core.api.InternalEvent} can be
     * accepted.
     */
    WAIT,

    /**
     * On back-pressure drop the {@link org.mule.runtime.core.api.InternalEvent} by immediately completing
     * {@link org.mule.runtime.core.api.InternalEventContext} with no result without performing any processing.
     */
    DROP
  }

}
