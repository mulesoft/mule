/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.internal;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.container.internal.MuleClassLoaderLookupPolicy.invalidLookupPolicyOverrideError;
import static org.mule.runtime.module.artifact.api.classloader.ChildFirstLookupStrategy.CHILD_FIRST;
import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import static org.mule.runtime.module.artifact.api.classloader.ParentOnlyLookupStrategy.PARENT_ONLY;

import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class MuleClassLoaderLookupPolicyProviderTestCase extends AbstractMuleTestCase {

  private static final String FOO_PACKAGE = "org.foo";
  private static final String FOO_PACKAGE_PREFIX = FOO_PACKAGE + ".";
  private static final String FOO_CLASS = FOO_PACKAGE_PREFIX + "Object";
  private static final String SYSTEM_PACKAGE = "java";
  private static final String JAVA_PACKAGE = SYSTEM_PACKAGE + ".lang";
  private static final String JAVA_PACKAGE_PREFIX = JAVA_PACKAGE + ".";

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void extendingCustomLookupStrategyForSystemPackage() throws Exception {
    final String overrideClassName = Object.class.getName();
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(invalidLookupPolicyOverrideError(overrideClassName, CHILD_FIRST));

    new MuleClassLoaderLookupPolicy(emptyMap(), singleton(SYSTEM_PACKAGE))
        .extend(singletonMap(overrideClassName, CHILD_FIRST));
  }

  @Test
  public void returnsConfiguredLookupStrategy() throws Exception {
    MuleClassLoaderLookupPolicy lookupPolicy =
        new MuleClassLoaderLookupPolicy(singletonMap(JAVA_PACKAGE, CHILD_FIRST),
                                        emptySet());

    LookupStrategy lookupStrategy = lookupPolicy.getClassLookupStrategy(Object.class.getName());
    assertThat(lookupStrategy, sameInstance(CHILD_FIRST));

    lookupPolicy =
        new MuleClassLoaderLookupPolicy(singletonMap(JAVA_PACKAGE_PREFIX, CHILD_FIRST), emptySet());

    lookupStrategy = lookupPolicy.getClassLookupStrategy(Object.class.getName());
    assertThat(lookupStrategy, sameInstance(CHILD_FIRST));
  }

  @Test
  public void usesParentOnlyForSystemPackage() throws Exception {
    ClassLoaderLookupPolicy lookupPolicy = new MuleClassLoaderLookupPolicy(emptyMap(), singleton(SYSTEM_PACKAGE));

    assertThat(lookupPolicy.getClassLookupStrategy(Object.class.getName()), sameInstance(PARENT_ONLY));

    lookupPolicy = new MuleClassLoaderLookupPolicy(emptyMap(), singleton(JAVA_PACKAGE));
    assertThat(lookupPolicy.getClassLookupStrategy(Object.class.getName()), sameInstance(PARENT_ONLY));
  }

  @Test
  public void usesChildFirstForNoConfiguredPackage() throws Exception {
    ClassLoaderLookupPolicy lookupPolicy = new MuleClassLoaderLookupPolicy(emptyMap(), emptySet());

    assertThat(lookupPolicy.getClassLookupStrategy(FOO_CLASS), sameInstance(CHILD_FIRST));
  }

  @Test
  public void extendsPolicy() throws Exception {
    ClassLoaderLookupPolicy lookupPolicy = new MuleClassLoaderLookupPolicy(emptyMap(), emptySet());

    final ClassLoaderLookupPolicy extendedPolicy =
        lookupPolicy.extend(singletonMap(FOO_PACKAGE, PARENT_FIRST));

    assertThat(extendedPolicy.getClassLookupStrategy(FOO_CLASS), sameInstance(PARENT_FIRST));
  }

  @Test
  public void maintainsOriginalLookupStrategy() throws Exception {
    ClassLoaderLookupPolicy lookupPolicy =
        new MuleClassLoaderLookupPolicy(singletonMap(FOO_PACKAGE,
                                                     new ContainerOnlyLookupStrategy(getClass().getClassLoader())),
                                        emptySet());

    final ClassLoaderLookupPolicy extendedPolicy =
        lookupPolicy.extend(singletonMap(FOO_PACKAGE, PARENT_FIRST));

    assertThat(extendedPolicy.getClassLookupStrategy(FOO_CLASS), instanceOf(ContainerOnlyLookupStrategy.class));
    assertThat(extendedPolicy.getClassLookupStrategy(FOO_CLASS).getClassLoaders(getClass().getClassLoader()),
               contains(getClass().getClassLoader()));
  }

  @Test
  public void normalizesLookupStrategies() throws Exception {
    ClassLoaderLookupPolicy lookupPolicy =
        new MuleClassLoaderLookupPolicy(singletonMap(FOO_PACKAGE,
                                                     new ContainerOnlyLookupStrategy(getClass().getClassLoader())),
                                        emptySet());

    final ClassLoaderLookupPolicy extendedPolicy =
        lookupPolicy.extend(singletonMap(FOO_PACKAGE_PREFIX, PARENT_FIRST));

    assertThat(extendedPolicy.getClassLookupStrategy(FOO_CLASS), instanceOf(ContainerOnlyLookupStrategy.class));
    assertThat(extendedPolicy.getClassLookupStrategy(FOO_CLASS).getClassLoaders(getClass().getClassLoader()),
               contains(getClass().getClassLoader()));
  }

  @Test
  public void cannotExtendPolicyWithSystemPackage() throws Exception {
    ClassLoaderLookupPolicy lookupPolicy = new MuleClassLoaderLookupPolicy(emptyMap(), singleton(SYSTEM_PACKAGE));

    final String overrideClassName = Object.class.getName();
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(invalidLookupPolicyOverrideError(overrideClassName, PARENT_FIRST));

    lookupPolicy.extend(singletonMap(overrideClassName, PARENT_FIRST));
  }
}
