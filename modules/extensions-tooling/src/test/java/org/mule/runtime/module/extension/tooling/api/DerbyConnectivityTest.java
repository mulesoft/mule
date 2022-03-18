/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.tooling.api;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.extension.db.internal.DbConnector;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.module.extension.internal.util.MuleExtensionUtils;
import org.mule.runtime.tooling.api.connectivity.ToolingActivityExecutor;
import org.mule.runtime.tooling.internal.DefaultToolingActivityExecutor;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class DerbyConnectivityTest extends ExtensionFunctionalTestCase {

  private ToolingActivityExecutor executor = new DefaultToolingActivityExecutor();

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {DbConnector.class};
  }

  @Override
  protected String getConfigFile() {
    return "empty-config.xml";
  }

  @Test
  public void testConnectivity() throws Exception {
    ExtensionModel dbExtensionModel = muleContext.getExtensionManager().getExtension("Database").get();
    ConnectionProviderModel derbyConnectionProviderModel =
        dbExtensionModel.getConfigurationModels().get(0).getConnectionProviderModel("derby").get();

    ClassLoader dbClassLoader = MuleExtensionUtils.getClassLoader(dbExtensionModel);

    Map<String, Object> params = new HashMap<>();
    params.put("database", "target/muleEmbeddedDB");
    params.put("create", "true");


    long now = System.currentTimeMillis();
    ConnectionValidationResult result = executor.testConnectivity(dbExtensionModel,
                                                                  derbyConnectionProviderModel,
                                                                  dbClassLoader,
                                                                  params);
    System.out.println("Diet Connectivity test took " + (System.currentTimeMillis() - now) + " ms");

    assertThat(result.isValid(), is(true));
    assertThat(result.getException(), is(nullValue()));
  }

}
