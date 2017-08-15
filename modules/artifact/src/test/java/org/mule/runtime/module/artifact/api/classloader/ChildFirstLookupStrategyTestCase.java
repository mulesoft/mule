/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.classloader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.module.artifact.api.classloader.ChildFirstLookupStrategy.CHILD_FIRST;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.List;

import org.junit.Test;

@SmallTest
public class ChildFirstLookupStrategyTestCase extends AbstractMuleTestCase {

  @Test
  public void returnsChildAndParent() throws Exception {
    List<ClassLoader> classLoaders = CHILD_FIRST.getClassLoaders(getClass().getClassLoader());

    assertThat(classLoaders, contains(getClass().getClassLoader(), getClass().getClassLoader().getParent()));
  }

  @Test
  public void returnsChildOnlyIfParentIfNull() throws Exception {
    ClassLoader classLoader = mock(ClassLoader.class);

    List<ClassLoader> classLoaders = CHILD_FIRST.getClassLoaders(classLoader);

    assertThat(classLoaders, contains(classLoader));
  }
}
