/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.registry;

import static java.lang.Thread.currentThread;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.mule.runtime.core.internal.registry.TestDiscoverableObject;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Collection;

import org.junit.Test;

@SmallTest
public class SpiServiceRegistryTestCase extends AbstractMuleTestCase {

  private ServiceRegistry serviceRegistry = new SpiServiceRegistry();

  @Test
  public void lookupProviders() throws Exception {
    Collection<TestDiscoverableObject> discoveredObjects =
        serviceRegistry.lookupProviders(TestDiscoverableObject.class, currentThread().getContextClassLoader());
    assertProvidersFound(discoveredObjects);
  }

  @Test
  public void lookupProvidersWithCustomClassLoader() throws Exception {
    ClassLoader classLoader = mock(ClassLoader.class, RETURNS_DEEP_STUBS);
    serviceRegistry.lookupProviders(TestDiscoverableObject.class, classLoader);

    verify(classLoader).getResources("META-INF/services/" + TestDiscoverableObject.class.getName());
  }

  private void assertProvidersFound(Collection<TestDiscoverableObject> discoveredObjects) {
    assertThat(discoveredObjects, hasSize(1));
    assertThat(discoveredObjects.iterator().next(), instanceOf(TestDiscoverableObject.class));
  }

}
