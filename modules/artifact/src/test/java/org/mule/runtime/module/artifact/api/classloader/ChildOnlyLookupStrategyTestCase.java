/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
