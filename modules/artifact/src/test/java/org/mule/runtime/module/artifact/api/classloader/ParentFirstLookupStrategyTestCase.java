/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.classloader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.List;

import org.junit.Test;

@SmallTest
public class ParentFirstLookupStrategyTestCase extends AbstractMuleTestCase {

  @Test
  public void returnsParentAndChild() throws Exception {
    List<ClassLoader> classLoaders = PARENT_FIRST.getClassLoaders(getClass().getClassLoader());

    assertThat(classLoaders, contains(getClass().getClassLoader().getParent(), getClass().getClassLoader()));
  }

  @Test
  public void returnsChildOnlyIfParentIfNull() throws Exception {
    ClassLoader classLoader = mock(ClassLoader.class);

    List<ClassLoader> classLoaders = PARENT_FIRST.getClassLoaders(classLoader);

    assertThat(classLoaders, contains(classLoader));
  }
}
