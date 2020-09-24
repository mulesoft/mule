/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.classloading;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.StringContains.containsString;
import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.tck.util.TestConnectivityUtils.disableAutomaticTestConnectivity;
import static org.mule.test.classloading.CLKeysResolver.GET_METADATA;
import static org.mule.test.classloading.CLNoneConnectionProvider.CONNECT;
import static org.mule.test.classloading.CLNoneConnectionProvider.DISCONNECT;
import static org.mule.test.classloading.CLPoolingConnectionProvider.ON_BORROW;
import static org.mule.test.classloading.CLPoolingConnectionProvider.ON_RETURN;
import static org.mule.test.classloading.internal.AllOptionalParameterGroup.ALL_OPTIONAL_PARAMETER_GROUP;

import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.classloading.api.ClassLoadingHelper;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.hamcrest.Matcher;
import org.hamcrest.core.StringContains;
import org.junit.Rule;
import org.junit.Test;

public class ClassLoadingOnConnectionsTestCase extends AbstractExtensionFunctionalTestCase {

  @Inject
  private MetadataService metadataManager;

  @Rule
  public SystemProperty disableTestConnectivity = disableAutomaticTestConnectivity();

  @Override
  protected void doTearDown() throws Exception {
    ClassLoadingHelper.createdClassLoaders.clear();
  }

  @Override
  protected String getConfigFile() {
    return "classloading/classloading-extension-config.xml";
  }

  @Test
  public void noneConnectionProvider() throws Exception {
    flowRunner("none-operation").run();
    verifyUsedClassLoaders(ALL_OPTIONAL_PARAMETER_GROUP, CONNECT, DISCONNECT);
  }

  @Test
  public void cachedConnectionProvider() throws Exception {
    flowRunner("cached-operation").run();
    verifyUsedClassLoaders(ALL_OPTIONAL_PARAMETER_GROUP, CONNECT);
  }

  @Test
  public void poolingConnectionProvider() throws Exception {
    flowRunner("pooling-operation").run();
    verifyUsedClassLoaders(ALL_OPTIONAL_PARAMETER_GROUP, CONNECT, ON_BORROW, ON_RETURN);
  }

  @Test
  public void allOptionalParameterGroup() throws Exception {
    verifyUsedClassLoaders(ALL_OPTIONAL_PARAMETER_GROUP);
  }

  @Test
  public void operationWithMetadataResolver() throws Exception {
    metadataManager.getMetadataKeys(builder().globalName("none").build());
    verifyUsedClassLoaders(GET_METADATA);
  }

  void verifyUsedClassLoaders(String... phasesToExecute) {
    Map<String, ClassLoader> createdClassLoaders = ClassLoadingHelper.createdClassLoaders;
    List<ClassLoader> collect = createdClassLoaders.values().stream().distinct().collect(toList());
    collect.forEach(this::assertExtensionClassLoader);
    Set<String> executedPhases = createdClassLoaders.keySet();
    assertThat(executedPhases, is(hasItems(stream(phasesToExecute).map(StringContains::containsString).toArray(Matcher[]::new))));
  }

  private void assertExtensionClassLoader(ClassLoader classLoader) {
    assertThat(classLoader.toString(),
               allOf(containsString("classloading-extension"),
                     anyOf(containsString(".TestRegionClassLoader[Region] @"),
                           containsString("MuleArtifactClassLoader"))));
  }
}
