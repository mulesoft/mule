/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.internal;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.container.api.MuleModule;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class CompositeModuleDiscovererTestCase extends AbstractMuleTestCase {

  @Test
  public void delegatesToComposedDiscovers() throws Exception {
    final ModuleDiscoverer discoverer1 = mock(ModuleDiscoverer.class);
    MuleModule module1 = new MuleModule("module1", emptySet(), emptySet(), emptySet(), emptySet(), emptyList());
    final List<MuleModule> modules1 = new ArrayList<>();
    modules1.add(module1);
    when(discoverer1.discover()).thenReturn(modules1);
    MuleModule module2 = new MuleModule("module1", emptySet(), emptySet(), emptySet(), emptySet(), emptyList());
    final List<MuleModule> modules2 = new ArrayList<>();
    modules2.add(module2);
    final ModuleDiscoverer discoverer2 = mock(ModuleDiscoverer.class);
    when(discoverer2.discover()).thenReturn(modules2);

    final CompositeModuleDiscoverer composite = new CompositeModuleDiscoverer(discoverer1, discoverer2);
    final List<MuleModule> discovered = composite.discover();

    assertThat(discovered.size(), equalTo(2));
    assertThat(discovered, contains(module1, module2));
  }
}
