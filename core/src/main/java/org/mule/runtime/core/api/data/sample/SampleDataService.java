/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.data.sample;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.sdk.api.data.sample.SampleDataException;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Provides the capability of obtaining sample data for any component in a Mule app which supports providing sample data.
 *
 * @since 4.4.0
 */
@NoImplement
public interface SampleDataService {

  /**
   * Key under which the {@link SampleDataService} can be found in the {@link org.mule.runtime.api.artifact.Registry}
   */
  String SAMPLE_DATA_SERVICE_KEY = "_muleSampleDataService";

  /**
   * Returns a sample output {@link Message} for the component at the given {@link Location}.
   *
   * @param location The {@link Location} of the target component
   * @return a sample output {@link Message}
   */
  Message getSampleData(Location location) throws SampleDataException;

  Message getSampleData(String extensionName,
                        String componentName,
                        Map<String, Object> parameters,
                        Supplier<Optional<ConfigurationInstance>> configurationInstanceSupplier)
      throws SampleDataException;
}
