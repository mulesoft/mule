/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.data.sample;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.data.sample.SampleDataService;
import org.mule.runtime.api.message.Message;

public class MuleSampleDataService implements SampleDataService {

  @Override
  public Message getSampleData(Location location) {
    return null;
  }
}
