/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.extension;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.extension.api.metadata.ComponentMetadataConfigurer;
import org.mule.runtime.extension.api.metadata.ComponentMetadataConfigurerFactory;

public final class ComponentConfigurerTestUtils {

  private ComponentConfigurerTestUtils() {

  }

  public static ComponentMetadataConfigurerFactory createMockedFactory() {
    return new ComponentMetadataConfigurerFactory() {

      @Override
      public ComponentMetadataConfigurer create() {
        return mock(ComponentMetadataConfigurer.class, RETURNS_DEEP_STUBS);
      }
    };
  }

}
