/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.classloader;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.runner.api.PluginUrlClassification;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

@SmallTest
public class PluginLookPolicyFactoryTestCase extends AbstractMuleTestCase {

  private static final String BAR_PLUGIN_ID = "org.foo:bar:1.0-SNAPSHOT";
  private static final String FOO_PLUGIN_ID = "org.bar:foo:1.0-SNAPSHOT";
  private static final String FOO_PACKAGE = "org.bar.foo";

  private PluginLookPolicyFactory factory;
  private PluginUrlClassification fooPluginClassification;

  @Before
  public void setUp() {
    factory = new PluginLookPolicyFactory();
    fooPluginClassification =
        new PluginUrlClassification(FOO_PLUGIN_ID, emptyList(), emptyList(), emptyList(), newHashSet(FOO_PACKAGE), emptySet(),
                                    emptySet(), emptySet());
  }

  @Test
  public void lookupPoliciesForPluginThatDeclaresDependency() {
    PluginUrlClassification barPluginClassification =
        new PluginUrlClassification(BAR_PLUGIN_ID, emptyList(), emptyList(), newArrayList(FOO_PLUGIN_ID));
    List<PluginUrlClassification> pluginClassifications = newArrayList(barPluginClassification, fooPluginClassification);
    ClassLoaderLookupPolicy parentLookupPolicies = getParentClassLoaderLookupPolicy();

    ClassLoaderLookupPolicy pluginPolicy =
        factory.createLookupPolicy(barPluginClassification, pluginClassifications, parentLookupPolicies, null);
    assertThat(pluginPolicy.getClassLookupStrategy(FOO_PACKAGE), sameInstance(PARENT_FIRST));
  }

  @Test
  public void lookupPoliciesForPluginThatDoesNotDeclareDependency() {
    PluginUrlClassification barPluginClassification =
        new PluginUrlClassification(BAR_PLUGIN_ID, emptyList(), emptyList(), emptyList());
    List<PluginUrlClassification> pluginClassifications = newArrayList(barPluginClassification, fooPluginClassification);
    ClassLoaderLookupPolicy parentLookupPolicies = getParentClassLoaderLookupPolicy();

    ClassLoaderLookupPolicy pluginPolicy =
        factory.createLookupPolicy(barPluginClassification, pluginClassifications, parentLookupPolicies, null);
    assertThat(pluginPolicy.getClassLookupStrategy(FOO_PACKAGE), is(nullValue()));
  }

  private ClassLoaderLookupPolicy getParentClassLoaderLookupPolicy() {
    ArgumentCaptor<Map> argumentCaptor = forClass(Map.class);
    ClassLoaderLookupPolicy parentLookupPolicies = mock(ClassLoaderLookupPolicy.class);
    when(parentLookupPolicies.extend(argumentCaptor.capture()))
        .thenAnswer(invocation -> getClassLoaderLookupPolicyByPackage(argumentCaptor.getValue()));
    return parentLookupPolicies;
  }

  private ClassLoaderLookupPolicy getClassLoaderLookupPolicyByPackage(Map<String, LookupStrategy> delegate) {
    return new ClassLoaderLookupPolicy() {

      @Override
      public LookupStrategy getClassLookupStrategy(String className) {
        return delegate.get(className);
      }

      @Override
      public LookupStrategy getPackageLookupStrategy(String packageName) {
        return null;
      }

      @Override
      public ClassLoaderLookupPolicy extend(Map<String, LookupStrategy> lookupStrategies) {
        throw new UnsupportedOperationException("Cannot be extended");
      }
    };
  }

}
