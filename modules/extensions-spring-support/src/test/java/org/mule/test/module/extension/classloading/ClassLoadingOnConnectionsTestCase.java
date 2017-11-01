/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.classloading;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.StringContains.containsString;
import static org.mule.tck.util.TestConnectivityUtils.disableAutomaticTestConnectivity;
import static org.mule.test.classloading.CLNoneConnectionProvider.CONNECT;
import static org.mule.test.classloading.CLNoneConnectionProvider.DISCONNECT;
import static org.mule.test.classloading.CLPoolingConnectionProvider.ON_BORROW;
import static org.mule.test.classloading.CLPoolingConnectionProvider.ON_RETURN;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.classloading.api.ClassLoadingHelper;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hamcrest.Matcher;
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ClassLoadingOnConnectionsTestCase extends AbstractExtensionFunctionalTestCase {

  @Rule
  public SystemProperty disableTestConnectivity = disableAutomaticTestConnectivity();

  @Before
  public void setUp() {
    ClassLoadingHelper.createdClassLoaders.clear();
  }

  @Override
  protected String getConfigFile() {
    return "classloading/classloading-extension-config.xml";
  }

  @Test
  public void noneConnectionProvider() throws Exception {
    flowRunner("none-operation").run();
    verifyUsedClassLoaders(CONNECT, DISCONNECT);
  }

  @Test
  public void cachedConnectionProvider() throws Exception {
    flowRunner("cached-operation").run();
    verifyUsedClassLoaders(CONNECT);
  }

  @Test
  public void poolingConnectionProvider() throws Exception {
    flowRunner("pooling-operation").run();
    verifyUsedClassLoaders(CONNECT, ON_BORROW, ON_RETURN);
  }

  void verifyUsedClassLoaders(String... phasesToExecute) {
    Map<String, ClassLoader> createdClassLoaders = ClassLoadingHelper.createdClassLoaders;
    List<ClassLoader> collect = createdClassLoaders.values().stream().distinct().collect(toList());
    assertThat(collect.size(), is(1));
    ClassLoader classLoader = collect.get(0);
    assertThat(classLoader.toString(), containsString("classloading-extension"));
    Set<String> executedPhases = createdClassLoaders.keySet();
    assertThat(executedPhases, is(hasItems(stream(phasesToExecute).map(StringContains::containsString).toArray(Matcher[]::new))));
  }
}
