/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.config.bootstrap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

@SmallTest
public class PropertiesBootstrapServiceDiscovererTestCase extends AbstractMuleTestCase {

  @Test
  public void discoversServiceOnDefaultClassLoader() throws Exception {
    final ClassLoader classLoader = mock(ClassLoader.class);

    final PropertiesBootstrapServiceDiscoverer propertiesBootstrapServiceDiscoverer =
        new PropertiesBootstrapServiceDiscoverer(classLoader);

    final List<BootstrapService> services = propertiesBootstrapServiceDiscoverer.discover();

    assertThat(services.size(), is(1));
  }

  @Test
  public void discoversMultipleServices() throws Exception {
    final List<Properties> properties = new ArrayList<>();
    properties.add(new Properties());
    properties.add(new Properties());

    doCustomClassLoaderDiscoverTest(properties);
  }

  @Test
  public void discoversNoServices() throws Exception {
    doCustomClassLoaderDiscoverTest(new ArrayList<>());
  }

  private void doCustomClassLoaderDiscoverTest(List<Properties> properties) throws BootstrapException {
    final ClassLoader classLoader = mock(ClassLoader.class);
    final RegistryBootstrapDiscoverer registryBootstrapDiscoverer = mock(RegistryBootstrapDiscoverer.class);
    when(registryBootstrapDiscoverer.discover()).thenReturn(properties);
    final PropertiesBootstrapServiceDiscoverer propertiesBootstrapServiceDiscoverer =
        new PropertiesBootstrapServiceDiscoverer(classLoader, registryBootstrapDiscoverer);

    final List<BootstrapService> services = propertiesBootstrapServiceDiscoverer.discover();

    assertThat(services.size(), is(properties.size()));
    for (int i = 0; i < services.size(); i++) {
      assertThat(services.get(i).getProperties(), is(properties.get(i)));
    }
  }
}
