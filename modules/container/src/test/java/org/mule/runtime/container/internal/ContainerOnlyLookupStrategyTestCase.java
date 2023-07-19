/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.container.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.mock;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.List;

import org.junit.Test;

@SmallTest
public class ContainerOnlyLookupStrategyTestCase extends AbstractMuleTestCase {

  @Test
  public void returnsContainerClassLoader() throws Exception {
    ClassLoader containerClassLoader = getClass().getClassLoader();
    ContainerOnlyLookupStrategy lookupStrategy = new ContainerOnlyLookupStrategy(containerClassLoader);

    List<ClassLoader> classLoaders = lookupStrategy.getClassLoaders(mock(ClassLoader.class));

    assertThat(classLoaders, hasItem(containerClassLoader));
  }

  @Test(expected = IllegalArgumentException.class)
  public void validatesContainerClassLoader() throws Exception {
    new ContainerOnlyLookupStrategy(null);
  }
}
