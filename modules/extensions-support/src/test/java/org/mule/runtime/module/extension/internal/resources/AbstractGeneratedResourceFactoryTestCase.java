/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static java.util.Arrays.stream;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ServiceLoader;

import org.junit.Test;

/**
 * Base class for testing implementations of {@link GeneratedResourceFactory}.
 * <p>
 * This class is needed because it's not enough to ensure that the resource factory works fine itself, it's also needed to make
 * sure that it is SPI discoverable, otherwise its product will not be available.
 * <p>
 * Implementations of this class are to implement the {@link #getResourceFactoryTypes()} and return the types that are to be
 * tested, and the {@link #spiDiscovery()} test will verify that all of them can be found through a {@link ServiceLoader}
 *
 * @since 4.0
 */
public abstract class AbstractGeneratedResourceFactoryTestCase extends AbstractMuleTestCase {

  @Test
  public void spiDiscovery() throws Exception {
    ServiceLoader<GeneratedResourceFactory> services = ServiceLoader.load(GeneratedResourceFactory.class);
    assertThat(stream(getResourceFactoryTypes()).allMatch(factoryClass -> {
      for (GeneratedResourceFactory factory : services) {
        if (factoryClass.isAssignableFrom(factory.getClass())) {
          return true;
        }
      }

      return false;
    }), is(true));

  }

  /**
   * @return the {@link GeneratedResourceFactory} types that this tests verifies
   */
  protected abstract Class<? extends GeneratedResourceFactory>[] getResourceFactoryTypes();
}
