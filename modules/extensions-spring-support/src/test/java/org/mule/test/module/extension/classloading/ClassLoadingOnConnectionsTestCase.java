/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.classloading;

import static org.mule.tck.util.TestConnectivityUtils.disableAutomaticTestConnectivity;
import static org.mule.test.classloading.CLNoneConnectionProvider.CONNECT;
import static org.mule.test.classloading.CLNoneConnectionProvider.DISCONNECT;
import static org.mule.test.classloading.CLPoolingConnectionProvider.ON_BORROW;
import static org.mule.test.classloading.CLPoolingConnectionProvider.ON_RETURN;
import static org.mule.test.classloading.api.ClassLoadingHelper.verifyUsedClassLoaders;
import static org.mule.test.classloading.internal.AllOptionalParameterGroup.ALL_OPTIONAL_PARAMETER_GROUP;

import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.classloading.api.ClassLoadingHelper;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.Rule;
import org.junit.Test;

public class ClassLoadingOnConnectionsTestCase extends AbstractExtensionFunctionalTestCase {

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
}
