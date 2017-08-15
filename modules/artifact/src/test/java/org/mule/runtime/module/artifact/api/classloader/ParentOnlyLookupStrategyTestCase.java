/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.classloader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.module.artifact.api.classloader.ParentOnlyLookupStrategy.PARENT_ONLY;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.List;

import org.junit.Test;

@SmallTest
public class ParentOnlyLookupStrategyTestCase extends AbstractMuleTestCase {

  @Test
  public void returnsParentOnly() throws Exception {
    List<ClassLoader> classLoaders = PARENT_ONLY.getClassLoaders(getClass().getClassLoader());

    assertThat(classLoaders, contains(getClass().getClassLoader().getParent()));
  }

  @Test
  public void returnsEmptyListWhenParentIsNull() throws Exception {
    ClassLoader classLoader = mock(ClassLoader.class);

    List<ClassLoader> classLoaders = PARENT_ONLY.getClassLoaders(classLoader);

    assertThat(classLoaders, is(empty()));
  }
}
