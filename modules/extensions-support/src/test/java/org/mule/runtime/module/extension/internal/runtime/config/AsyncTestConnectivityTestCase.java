/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static java.lang.System.currentTimeMillis;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONFIGURATION_PROPERTIES;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TIME_SUPPLIER;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.retry.RetryNotifier;
import org.mule.runtime.core.api.retry.async.AsynchronousRetryTemplate;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationState;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.size.SmallTest;
import org.mule.tck.util.TestTimeSupplier;

import java.util.Optional;

import io.qameta.allure.Description;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mock;

@SmallTest
public class AsyncTestConnectivityTestCase extends AbstractMuleContextTestCase {

  protected static final int RECONNECTION_MAX_ATTEMPTS = 5;
  private static final int RECONNECTION_FREQ = 100;
  private static final String NAME = "name";
  private static final int TEST_TIMEOUT = 2000;
  private static final int TEST_POLL_DELAY = 10;

  public AsyncTestConnectivityTestCase() {}

  @Mock
  private ConfigurationModel configurationModel;

  @Mock
  private ConfigurationState configurationState;

  @Mock
  private ConfigurationProperties configurationProperties;

  protected Lifecycle value = mock(Lifecycle.class, withSettings().extraInterfaces(Component.class));
  protected AsyncConnectionManagerAdapter connectionManager;
  protected RetryPolicyTemplate retryPolicyTemplate;
  protected LifecycleAwareConfigurationInstance configurationInstance;
  private TestTimeSupplier timeSupplier = new TestTimeSupplier(currentTimeMillis());
  private Optional<ConnectionProvider> connectionProvider =
      of(mock(ConnectionProvider.class, withSettings().extraInterfaces(Lifecycle.class, MuleContextAware.class)));

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    initMocks(this);
    super.doSetUpBeforeMuleContextCreation();
  }

  @Override
  protected void doSetUp() throws Exception {
    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject(OBJECT_TIME_SUPPLIER, timeSupplier);
    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject(OBJECT_CONFIGURATION_PROPERTIES,
                                                                         configurationProperties);

    doReturn(empty()).when(configurationProperties).resolveBooleanProperty(MULE_LAZY_INIT_DEPLOYMENT_PROPERTY);

    retryPolicyTemplate = createRetryTemplate();
    retryPolicyTemplate.setNotifier(mock(RetryNotifier.class));
    connectionManager = spy(new AsyncConnectionManagerAdapter(retryPolicyTemplate));
    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject(OBJECT_CONNECTION_MANAGER, connectionManager);
    configurationInstance = createConfigurationInstance();

    super.doSetUp();
  }

  protected RetryPolicyTemplate createRetryTemplate() {
    return new AsynchronousRetryTemplate(new SimpleRetryPolicyTemplate(RECONNECTION_FREQ, RECONNECTION_MAX_ATTEMPTS));
  }

  @After
  public void after() {
    configurationInstance.dispose();
  }

  protected LifecycleAwareConfigurationInstance createConfigurationInstance() {
    return new LifecycleAwareConfigurationInstance(NAME,
                                                   configurationModel,
                                                   value,
                                                   configurationState,
                                                   connectionProvider);
  }


  @Test
  @Description("Checks that the test connectivity test is not interrupted")
  public void testConnectivityIsNotInterruptedWhenAsyncRetryTemplate() throws Exception {
    configurationInstance.initialise();
    configurationInstance.start();
    new PollingProber(TEST_TIMEOUT, TEST_POLL_DELAY).check(new JUnitLambdaProbe(() -> {
      verify(connectionManager).testConnectivity(configurationInstance);
      assertThat(connectionManager.wasInterruptedBeforeTestingConnectivity(), is(false));
      return true;
    }));
  }

  /**
   * This class is used to verify that the testConnectivity is performed before the thread pool used for connectivity testing
   * purposes is interrupted.
   */
  private static class AsyncConnectionManagerAdapter implements ConnectionManagerAdapter {

    private RetryPolicyTemplate retryPolicyTemplate;

    public AsyncConnectionManagerAdapter(RetryPolicyTemplate retryPolicyTemplate) {
      this.retryPolicyTemplate = retryPolicyTemplate;
    }

    private boolean interrupted = false;

    @Override
    public <C> void bind(Object config, ConnectionProvider<C> connectionProvider) {}

    @Override
    public boolean hasBinding(Object config) {
      return false;
    }

    @Override
    public void unbind(Object config) {}

    @Override
    public <C> ConnectionHandler<C> getConnection(Object config) throws ConnectionException {
      return null;
    }

    @Override
    public <C> ConnectionValidationResult testConnectivity(ConnectionProvider<C> connectionProvider) {
      if (Thread.currentThread().isInterrupted()) {
        interrupted = true;
      }

      return success();
    }

    @Override
    public <C> ConnectionValidationResult testConnectivity(C connection, ConnectionHandler<C> connectionHandler) {
      return success();
    }

    @Override
    public ConnectionValidationResult testConnectivity(ConfigurationInstance configurationInstance)
        throws IllegalArgumentException {
      return success();
    }

    @Override
    public void initialise() throws InitialisationException {}

    @Override
    public void start() throws MuleException {}

    @Override
    public void stop() throws MuleException {}

    @Override
    public void dispose() {}

    @Override
    public <C> RetryPolicyTemplate getRetryTemplateFor(ConnectionProvider<C> connectionProvider) {
      return retryPolicyTemplate;
    }

    @Override
    public <C> ReconnectionConfig getReconnectionConfigFor(ConnectionProvider<C> connectionProvider) {
      return null;
    }

    @Override
    public PoolingProfile getDefaultPoolingProfile() {
      return null;
    }

    public boolean wasInterruptedBeforeTestingConnectivity() {
      return interrupted;
    }

  }
}
