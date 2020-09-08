/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.data.sample;

import static java.lang.String.format;
import static org.mule.runtime.core.internal.util.LocationUtils.deleteLastPartFromLocation;
import static org.mule.runtime.core.internal.util.LocationUtils.isConnection;
import static org.mule.sdk.api.data.sample.SampleDataException.NOT_SUPPORTED;

import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.data.sample.SampleDataService;
import org.mule.runtime.extension.api.data.sample.ComponentSampleDataProvider;
import org.mule.sdk.api.data.sample.SampleDataException;

import javax.inject.Inject;

public class MuleSampleDataService implements SampleDataService {

  @Inject
  private ConfigurationComponentLocator componentLocator;

  @Override
  public Message getSampleData(Location location) throws SampleDataException {
    boolean isConnection = isConnection(location);

    Location realLocation = isConnection
        ? deleteLastPartFromLocation(location)
        : location;

    Object component = findComponent(realLocation);

    if (component instanceof ComponentSampleDataProvider) {
      return ((ComponentSampleDataProvider) component).getSampleData();
    }

    throw new SampleDataException(format("The found element in the Location [%s] is not capable of provide Values",
                                         location),
                                  NOT_SUPPORTED);
  }

  private Object findComponent(Location location) throws SampleDataException {
    return componentLocator.find(location)
        .orElseThrow(() -> new SampleDataException(format("Invalid location [%s]. No element found in the given location.",
                                                          location),
                                                   SampleDataException.INVALID_LOCATION));
  }
}
