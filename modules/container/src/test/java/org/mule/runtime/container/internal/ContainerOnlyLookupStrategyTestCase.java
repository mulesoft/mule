/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
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

    assertThat(classLoaders, contains(containerClassLoader));
  }

  @Test(expected = IllegalArgumentException.class)
  public void validatesContainerClassLoader() throws Exception {
    new ContainerOnlyLookupStrategy(null);
  }
}
