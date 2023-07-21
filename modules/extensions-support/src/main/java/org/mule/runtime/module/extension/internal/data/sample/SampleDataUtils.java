/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.data.sample;

import static java.lang.String.format;
import static org.mule.runtime.core.api.util.ClassUtils.instantiateClass;

import org.mule.sdk.api.data.sample.SampleDataProvider;

/**
 * Utility class for {@link SampleDataProvider} related objects
 *
 * @since 4.4.0
 */
public class SampleDataUtils {

  /**
   * Creates an instance of the given {@link SampleDataProvider} class and retrieves its id.
   *
   * @param providerClass a class that implements the {@link SampleDataProvider} interface.
   * @return The id of the data provider
   * @since 4.4.0
   */
  public static String getSampleDataProviderId(Class<? extends SampleDataProvider> providerClass) {
    SampleDataProvider provider;
    try {
      provider = instantiateClass(providerClass);
    } catch (Exception e) {
      throw new IllegalStateException(format("There was an error creating an instance of %s to retrieve the Id of the provider",
                                             providerClass.getName()),
                                      e);
    }
    return provider.getId();
  }
}
