/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TIME_SUPPLIER;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.internal.metadata.MuleMetadataManager;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.module.extension.internal.AbstractInterceptableContractTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.size.SmallTest;
import org.mule.tck.util.TestTimeSupplier;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.verification.VerificationMode;

@SmallTest
@RunWith(Parameterized.class)
public class LifecycleAwareConfigurationInstanceTestCase
    extends AbstractInterceptableContractTestCase<LifecycleAwareConfigurationInstance> {

  private static final String NAME = "name";

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
                         new Object[][] {
                             {"With provider",
                                 mock(ConnectionProvider.class,
                                      withSettings().extraInterfaces(Lifecycle.class, MuleContextAware.class))},
                             {"Without provider", null}});
  }

  @Mock
  private RuntimeConfigurationModel configurationModel;

  @Mock
  private Lifecycle value;

  @Mock
  private ConnectionManager connectionManager;

  private String name;
  private Optional<ConnectionProvider> connectionProvider;

  public LifecycleAwareConfigurationInstanceTestCase(String name, ConnectionProvider connectionProvider) {
    this.name = name;
    this.connectionProvider = Optional.ofNullable(connectionProvider);
  }

  private TestTimeSupplier timeSupplier = new TestTimeSupplier(System.currentTimeMillis());

  @Before
  @Override
  public void before() throws Exception {
    MockitoAnnotations.initMocks(this);
    muleContext.getRegistry().registerObject(OBJECT_CONNECTION_MANAGER, connectionManager);
    muleContext.getRegistry().registerObject(OBJECT_TIME_SUPPLIER, timeSupplier);
    super.before();
  }

  @Override
  protected LifecycleAwareConfigurationInstance createInterceptable() {
    if (connectionProvider.isPresent()) {
      reset(connectionProvider.get());
    }
    return new LifecycleAwareConfigurationInstance(NAME, configurationModel, value, getInterceptors(), connectionProvider);
  }

  @Test
  public void valueInjected() throws Exception {
    interceptable.initialise();
    verify(injector).inject(value);
    if (connectionProvider.isPresent()) {
      verify(injector).inject(connectionProvider.get());
    } else {
      verify(injector, never()).inject(any(ConnectionProvider.class));
    }
  }

  @Test
  public void connectionBinded() throws Exception {
    interceptable.initialise();
    assertBinded();
  }

  private void assertBinded() throws Exception {
    if (connectionProvider.isPresent()) {
      verify(connectionManager, times(1)).bind(value, connectionProvider.get());
    } else {
      verify(connectionManager, never()).bind(same(value), anyObject());
    }
  }

  private VerificationMode getBindingVerificationMode() {
    return connectionProvider.map(p -> times(1)).orElse(never());
  }

  @Test
  public void connectionReBindedAfterStopStart() throws Exception {
    connectionBinded();
    interceptable.stop();
    verify(connectionManager, getBindingVerificationMode()).unbind(value);

    reset(connectionManager);
    interceptable.start();
    assertBinded();
  }

  @Test
  public void valueInitialised() throws Exception {
    interceptable.initialise();
    verify((Initialisable) value).initialise();
    if (connectionProvider.isPresent()) {
      verify((Initialisable) connectionProvider.get()).initialise();
    }
  }

  @Test
  public void valueStarted() throws Exception {
    interceptable.start();
    verify((Startable) value).start();
    if (connectionProvider.isPresent()) {
      verify((Startable) connectionProvider.get()).start();
    }
  }

  @Test
  public void valueStopped() throws Exception {
    interceptable.stop();
    verify((Stoppable) value).stop();
    if (connectionProvider.isPresent()) {
      verify((Stoppable) connectionProvider.get()).stop();
    }
  }

  @Test
  public void connectionUnbinded() throws Exception {
    interceptable.stop();
    if (connectionProvider.isPresent()) {
      verify(connectionManager).unbind(value);
    } else {
      verify(connectionManager, never()).unbind(anyObject());
    }
  }

  @Test
  public void valueDisposed() throws Exception {
    interceptable.dispose();
    verify((Disposable) value).dispose();
    if (connectionProvider.isPresent()) {
      verify((Disposable) connectionProvider.get()).dispose();
    }
  }

  @Test
  public void disposeMetadataCacheWhenConfigIsDisposed() throws Exception {
    MuleMetadataManager muleMetadataManager = muleContext.getRegistry().lookupObject(MuleMetadataManager.class);
    muleMetadataManager.getMetadataCache(NAME);
    interceptable.stop();
    new PollingProber(1000, 100).check(new JUnitLambdaProbe(() -> muleMetadataManager.getMetadataCaches().entrySet().isEmpty()));
  }

  @Test
  public void getName() {
    assertThat(interceptable.getName(), is(NAME));
  }

  @Test
  public void getModel() {
    assertThat(interceptable.getModel(), is(sameInstance(configurationModel)));
  }

  @Test
  public void getValue() {
    assertThat(interceptable.getValue(), is(sameInstance(value)));
  }

  @Test(expected = IllegalStateException.class)
  public void getStatsBeforeInit() {
    interceptable.getStatistics();
  }

  @Test
  public void getStatistics() throws Exception {
    interceptable.initialise();
    assertThat(interceptable.getStatistics(), is(notNullValue()));
  }
}
