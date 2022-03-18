/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.tooling.api;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.runtime.module.extension.tooling.internal.TestToolingExtensionDeclarer.PARTY_MODE_PARAM_NAME;
import static org.mule.runtime.module.extension.tooling.internal.TestToolingExtensionDeclarer.PASSWORD_PARAM_NAME;
import static org.mule.runtime.module.extension.tooling.internal.TestToolingExtensionDeclarer.PORT_PARAM_NAME;
import static org.mule.runtime.module.extension.tooling.internal.TestToolingExtensionDeclarer.USERNAME_PARAM_NAME;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.core.internal.lock.MuleLockFactory;
import org.mule.runtime.extension.api.runtime.connectivity.ConnectionProviderFactory;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.ExtensionModelFactory;
import org.mule.runtime.tooling.api.connectivity.ToolingActivityExecutor;
import org.mule.runtime.tooling.internal.DefaultToolingActivityExecutor;
import org.mule.runtime.module.extension.tooling.internal.TestToolingExtensionDeclarer;
import org.mule.runtime.module.extension.tooling.internal.extension.ComplexParameterGroup;
import org.mule.runtime.module.extension.tooling.internal.extension.TestConnectionProvider;
import org.mule.runtime.tooling.internal.service.scheduler.ToolingSchedulerService;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TestConnectivityTestCase extends AbstractMuleTestCase {

  public static final String HOST = "localhost";
  public static final String PASSWORD = "dietMule";
  public static final String USER = "dietuser";
  public static final String PORT = "8080"; // intentionally a String, to force value transformation
  public static final boolean PARTY_MODE = true;

  private ExtensionModel extensionModel;
  private ConnectionProviderModel connectionProviderModel;

  private ClassLoader classLoader = getClass().getClassLoader();
  private TestConnectionProvider connectionProvider = new TestConnectionProvider();
  private ToolingActivityExecutor executor = new DefaultToolingActivityExecutor();

  @Before
  public void setup() throws Exception {
    TestToolingExtensionDeclarer declarer = new TestToolingExtensionDeclarer();
    declarer.setConnectionProviderFactory(new ConnectionProviderFactory() {

      @Override
      public ConnectionProvider newInstance() {
        return connectionProvider;
      }

      @Override
      public Class<? extends ConnectionProvider> getObjectType() {
        return connectionProvider.getClass();
      }
    });

    extensionModel = new ExtensionModelFactory().create(
                                                        new DefaultExtensionLoadingContext(declarer.getExtensionDeclarer(),
                                                                                           classLoader,
                                                                                           DslResolvingContext
                                                                                               .getDefault(emptySet())));

    connectionProviderModel = extensionModel.getConfigurationModels().get(0).getConnectionProviders().get(0);
  }

  @Test
  public void testConnectivity() throws Exception {
    Map<String, Object> params = new HashMap<>();
    params.put(USERNAME_PARAM_NAME, USER);
    params.put(PASSWORD_PARAM_NAME, PASSWORD);
    params.put(PORT_PARAM_NAME, PORT);
    params.put(PARTY_MODE_PARAM_NAME, PARTY_MODE);

    long now = System.currentTimeMillis();
    ConnectionValidationResult result = executor.testConnectivity(extensionModel, connectionProviderModel, classLoader, params);
    System.out.println("Diet Connectivity test took " + (System.currentTimeMillis() - now) + " ms");

    if (!result.isValid()) {
      if (result.getException() != null) {
        throw result.getException();
      }

      fail("Connectivity testing failed");
    }

    assertThat(result.getException(), is(nullValue()));
    assertThat(result.getErrorType().isPresent(), is(false));

    assertThat(connectionProvider.getInitialise(), is(1));
    assertThat(connectionProvider.getStart(), is(1));
    assertThat(connectionProvider.getStop(), is(1));
    assertThat(connectionProvider.getDispose(), is(1));

    assertThat(connectionProvider.getUsername(), equalTo(USER));
    assertThat(connectionProvider.getPassword(), equalTo(PASSWORD));
    assertThat(connectionProvider.getHost(), equalTo(HOST));
    assertThat(connectionProvider.getPort(), equalTo(Integer.parseInt(PORT)));

    assertThat(connectionProvider.getLockFactory(), is(instanceOf(MuleLockFactory.class)));
    assertThat(connectionProvider.getSchedulerService(), is(instanceOf(ToolingSchedulerService.class)));
    assertThat(connectionProvider.getEncoding(), is(not(isEmptyString())));
    assertThat(connectionProvider.getConfigName(), is(not(isEmptyString())));

    assertThat(connectionProvider.getComplexParameterGroup(), is(notNullValue()));
    ComplexParameterGroup complexParameterGroup = connectionProvider.getComplexParameterGroup();
    assertThat(complexParameterGroup.isPartyMode(), is(PARTY_MODE));
    assertThat(complexParameterGroup.getGreetings(), is(notNullValue()));
    assertThat(complexParameterGroup.getAnnoyingPojo(), is(not(nullValue())));

    assertThat(connectionProvider.getConnection().isConnected(), is(false));
  }
}
