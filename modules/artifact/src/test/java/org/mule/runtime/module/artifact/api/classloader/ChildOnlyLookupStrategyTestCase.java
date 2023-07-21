/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.classloader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mule.runtime.module.artifact.api.classloader.ChildOnlyLookupStrategy.CHILD_ONLY;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.List;

import org.junit.Test;

@SmallTest
public class ChildOnlyLookupStrategyTestCase extends AbstractMuleTestCase {

  @Test
  public void returnsChildOnly() throws Exception {
    List<ClassLoader> classLoaders = CHILD_ONLY.getClassLoaders(getClass().getClassLoader());

    assertThat(classLoaders, contains(getClass().getClassLoader()));
  }
}
