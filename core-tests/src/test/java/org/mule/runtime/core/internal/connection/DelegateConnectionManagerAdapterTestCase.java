/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_CONNECTIONS_DEPLOYMENT_PROPERTY;
import static org.mule.test.allure.AllureConstants.ConnectionManagementFeature.CONNECTION_MANAGEMENT;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.LazyConnectionsStory.LAZY_CONNECTIONS;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import static org.junit.Assert.assertThrows;

import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@SmallTest
@Feature(CONNECTION_MANAGEMENT)
@Story(LAZY_CONNECTIONS)
public class DelegateConnectionManagerAdapterTestCase extends AbstractMuleContextTestCase {

  private DelegateConnectionManagerAdapter managerAdapter;

  @Before
  public void setUp() throws InitialisationException {
    muleContext.getDeploymentProperties().setProperty(MULE_LAZY_CONNECTIONS_DEPLOYMENT_PROPERTY, "true");
    managerAdapter = new DelegateConnectionManagerAdapter(muleContext);
    managerAdapter.initialise();
  }

  @Test
  public void proxyIsCreatedWithTheCorrectInterfaces() throws ConnectionException {
    SomeConfig config = new SomeConfig();
    managerAdapter.bind(config, new SomeConnectionProvider());
    ConnectionHandler<Object> connection = managerAdapter.getConnection(config);

    assertThat(connection, instanceOf(ConnectionHandler.class));
    assertThat(connection, instanceOf(ConnectionHandlerAdapter.class));
  }

  @Test
  @Issue("W-14885106")
  public void proxyDoesntFailReleasingAfterConnectionException() throws ConnectionException {
    SomeConfig config = new SomeConfig();
    managerAdapter.bind(config, new FailingConnectionProvider());
    ConnectionHandler<Object> connection = managerAdapter.getConnection(config);

    assertThrows("Expected", ConnectionException.class, () -> connection.getConnection());

    // Just verify these calls do not throw an exception
    connection.release();
    connection.invalidate();
  }

  private static class SomeConfig {

  }

  private static class SomeConnectionProvider implements CachedConnectionProvider {

    @Override
    public Object connect() throws ConnectionException {
      return null;
    }

    @Override
    public void disconnect(Object o) {

    }

    @Override
    public ConnectionValidationResult validate(Object o) {
      return null;
    }
  }

  private static class FailingConnectionProvider implements CachedConnectionProvider {

    @Override
    public Object connect() throws ConnectionException {
      throw new ConnectionException("Expected");
    }

    @Override
    public void disconnect(Object o) {

    }

    @Override
    public ConnectionValidationResult validate(Object o) {
      return null;
    }
  }

}
