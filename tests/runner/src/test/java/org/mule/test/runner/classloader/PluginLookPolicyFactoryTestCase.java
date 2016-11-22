/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.classloader;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.CHILD_ONLY;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.PARENT_FIRST;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy;
import org.mule.test.runner.api.PluginUrlClassification;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class PluginLookPolicyFactoryTestCase {

  private static final String BAR_PLUGIN_ID = "org.foo:bar:1.0-SNAPSHOT";

  private static final String FOO_PLUGIN_ID = "org.bar:foo:1.0-SNAPSHOT";
  private static final String FOO_PACKAGE = "org.bar.foo";

  private PluginLookPolicyFactory factory;
  private PluginUrlClassification fooPluginClassification;

  @Before
  public void setUp() {
    factory = new PluginLookPolicyFactory();
    fooPluginClassification = new PluginUrlClassification(FOO_PLUGIN_ID, Collections.<URL>emptyList(),
                                                          Collections.<Class>emptyList(), Collections.<String>emptyList(),
                                                          newHashSet(FOO_PACKAGE), Collections.<String>emptySet());
  }

  @Test
  public void lookupPoliciesForPluginThatDeclaresDependency() {
    PluginUrlClassification barPluginClassification =
        new PluginUrlClassification(BAR_PLUGIN_ID, Collections.<URL>emptyList(), Collections.<Class>emptyList(),
                                    newArrayList(FOO_PLUGIN_ID));
    List<PluginUrlClassification> pluginClassifications = newArrayList(barPluginClassification, fooPluginClassification);
    ClassLoaderLookupPolicy parentLookupPolicies = getParentClassLoaderLookupPolicy();

    ClassLoaderLookupPolicy pluginPolicy =
        factory.createLookupPolicy(barPluginClassification, pluginClassifications, parentLookupPolicies);
    assertThat(pluginPolicy.getLookupStrategy(FOO_PACKAGE), is(PARENT_FIRST));
  }

  @Test
  public void lookupPoliciesForPluginThatDoesNotDeclareDependency() {
    PluginUrlClassification barPluginClassification =
        new PluginUrlClassification(BAR_PLUGIN_ID, Collections.<URL>emptyList(), Collections.<Class>emptyList(),
                                    Collections.<String>emptyList());
    List<PluginUrlClassification> pluginClassifications = newArrayList(barPluginClassification, fooPluginClassification);
    ClassLoaderLookupPolicy parentLookupPolicies = getParentClassLoaderLookupPolicy();

    ClassLoaderLookupPolicy pluginPolicy =
        factory.createLookupPolicy(barPluginClassification, pluginClassifications, parentLookupPolicies);
    assertThat(pluginPolicy.getLookupStrategy(FOO_PACKAGE), is(CHILD_ONLY));
  }

  private ClassLoaderLookupPolicy getParentClassLoaderLookupPolicy() {
    ArgumentCaptor<Map> argumentCaptor = forClass(Map.class);
    ClassLoaderLookupPolicy parentLookupPolicies = mock(ClassLoaderLookupPolicy.class);
    when(parentLookupPolicies.extend(argumentCaptor.capture()))
        .thenAnswer(invocation -> getClassLoaderLookupPolicyByPackage(argumentCaptor.getValue()));
    return parentLookupPolicies;
  }

  private ClassLoaderLookupPolicy getClassLoaderLookupPolicyByPackage(Map<String, ClassLoaderLookupStrategy> delegate) {
    return new ClassLoaderLookupPolicy() {

      @Override
      public ClassLoaderLookupStrategy getLookupStrategy(String className) {
        return delegate.get(className);
      }

      @Override
      public ClassLoaderLookupPolicy extend(Map<String, ClassLoaderLookupStrategy> lookupStrategies) {
        throw new UnsupportedOperationException("Cannot be extended");
      }

    };
  }

}
