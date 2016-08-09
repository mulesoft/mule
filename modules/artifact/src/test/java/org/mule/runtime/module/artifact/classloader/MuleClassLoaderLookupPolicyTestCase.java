/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.classloader;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.CHILD_FIRST;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.PARENT_FIRST;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.PARENT_ONLY;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Collections;

import org.junit.Test;

@SmallTest
public class MuleClassLoaderLookupPolicyTestCase extends AbstractMuleTestCase {

  @Test(expected = IllegalArgumentException.class)
  public void extendingCustomLookupStrategyForSystemPackage() throws Exception {
    new MuleClassLoaderLookupPolicy(Collections.emptyMap(), singleton("java"))
        .extend(Collections.singletonMap(Object.class.getName(), CHILD_FIRST));
  }

  @Test
  public void returnsConfiguredLookupStrategy() throws Exception {
    MuleClassLoaderLookupPolicy lookupPolicy =
        new MuleClassLoaderLookupPolicy(Collections.singletonMap("java.lang", CHILD_FIRST), emptySet());

    ClassLoaderLookupStrategy lookupStrategy = lookupPolicy.getLookupStrategy(Object.class.getName());
    assertThat(lookupStrategy, is(CHILD_FIRST));

    lookupPolicy = new MuleClassLoaderLookupPolicy(Collections.singletonMap("java.lang.", CHILD_FIRST), emptySet());

    lookupStrategy = lookupPolicy.getLookupStrategy(Object.class.getName());
    assertThat(lookupStrategy, is(CHILD_FIRST));
  }

  @Test
  public void usesParentOnlyForSystemPackage() throws Exception {
    ClassLoaderLookupPolicy lookupPolicy = new MuleClassLoaderLookupPolicy(emptyMap(), singleton("java"));

    assertThat(lookupPolicy.getLookupStrategy(Object.class.getName()), is(PARENT_ONLY));

    lookupPolicy = new MuleClassLoaderLookupPolicy(emptyMap(), singleton("java.lang"));
    assertThat(lookupPolicy.getLookupStrategy(Object.class.getName()), is(PARENT_ONLY));
  }

  @Test
  public void usesChildFirstForNoConfiguredPackage() throws Exception {
    ClassLoaderLookupPolicy lookupPolicy = new MuleClassLoaderLookupPolicy(emptyMap(), emptySet());

    assertThat(lookupPolicy.getLookupStrategy("org.foo.Object"), is(CHILD_FIRST));
  }

  @Test
  public void extendsPolicy() throws Exception {
    ClassLoaderLookupPolicy lookupPolicy = new MuleClassLoaderLookupPolicy(emptyMap(), emptySet());

    final ClassLoaderLookupPolicy extendedPolicy = lookupPolicy.extend(singletonMap("org.foo", PARENT_FIRST));

    assertThat(extendedPolicy.getLookupStrategy("org.foo.Object"), is(PARENT_FIRST));
  }

  @Test
  public void maintainsOriginalLookupStrategy() throws Exception {
    ClassLoaderLookupPolicy lookupPolicy = new MuleClassLoaderLookupPolicy(singletonMap("org.foo", PARENT_ONLY), emptySet());

    final ClassLoaderLookupPolicy extendedPolicy = lookupPolicy.extend(singletonMap("org.foo", PARENT_FIRST));

    assertThat(extendedPolicy.getLookupStrategy("org.foo.Object"), is(PARENT_ONLY));
  }

  @Test
  public void normalizesLookupStrategies() throws Exception {
    ClassLoaderLookupPolicy lookupPolicy = new MuleClassLoaderLookupPolicy(singletonMap("org.foo", PARENT_ONLY), emptySet());

    final ClassLoaderLookupPolicy extendedPolicy = lookupPolicy.extend(singletonMap("org.foo.", PARENT_FIRST));

    assertThat(extendedPolicy.getLookupStrategy("org.foo.Object"), is(PARENT_ONLY));
  }

  @Test(expected = IllegalArgumentException.class)
  public void cannotExtendPolicyWithSystemPackage() throws Exception {
    ClassLoaderLookupPolicy lookupPolicy = new MuleClassLoaderLookupPolicy(emptyMap(), singleton("java"));

    lookupPolicy.extend(singletonMap(Object.class.getName(), PARENT_FIRST));
  }
}
