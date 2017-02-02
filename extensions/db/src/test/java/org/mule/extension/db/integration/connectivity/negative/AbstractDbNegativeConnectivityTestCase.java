/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.connectivity.negative;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.mule.extension.db.integration.DbArtifactClassLoaderRunnerConfig;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.util.TestConnectivityUtils;

public abstract class AbstractDbNegativeConnectivityTestCase extends MuleArtifactFunctionalTestCase
    implements DbArtifactClassLoaderRunnerConfig {

  static final Matcher<Exception> IS_CONNECTION_EXCEPTION = is(instanceOf(ConnectionException.class));

  TestConnectivityUtils utils;

  @Override
  protected String getConfigFile() {
    return "integration/config/db-negative-connectivity-testing-config.xml";
  }

  @Rule
  public SystemProperty rule = TestConnectivityUtils.disableAutomaticTestConnectivity();

  @Before
  public void createUtils() {
    utils = new TestConnectivityUtils(muleContext);
  }

}
