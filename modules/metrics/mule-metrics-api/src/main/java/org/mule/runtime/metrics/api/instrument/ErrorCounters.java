/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.api.instrument;

import org.mule.runtime.api.message.Error;

import java.util.function.Consumer;

public interface ErrorCounters extends Instrument {

  void add(Error value);

  void add(Throwable value);

  void onNewError(Consumer<LongCounter> newErrorCounterConsumer);

}
