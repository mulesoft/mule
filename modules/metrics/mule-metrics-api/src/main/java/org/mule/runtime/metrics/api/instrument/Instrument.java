/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.api.instrument;

/**
 * A component that is used for reporting measurements.
 */
public interface Instrument {

  /**
   * @return the name of the instrument.
   */
  String getName();

  /**
   * @return the description of the instrument.
   */
  String getDescription();
}
