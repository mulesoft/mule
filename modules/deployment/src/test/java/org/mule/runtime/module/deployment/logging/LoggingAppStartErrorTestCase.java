/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.logging;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.internal.config.RuntimeLockFactoryUtil.getRuntimeLockFactory;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.DEPLOYMENT_FAILED;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import static com.github.valfirst.slf4jtest.TestLoggerFactory.getTestLogger;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.registry.DefaultRegistry;
import org.mule.runtime.deployment.model.api.DeploymentStartException;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.application.ApplicationStatus;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.artifact.extension.ExtensionModelLoaderRepository;
import org.mule.runtime.module.artifact.activation.internal.classloader.MuleApplicationClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultMuleApplication;
import org.mule.runtime.module.deployment.internal.AbstractApplicationDeploymentTestCase;
import org.mule.runtime.module.deployment.internal.DefaultArtifactDeployer;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import com.github.valfirst.slf4jtest.TestLogger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class LoggingAppStartErrorTestCase extends AbstractApplicationDeploymentTestCase {

  private static final int PROBER_TIMEOUT = 1000;
  private static final int PROBER_INTERVAL = 100;

  private DefaultMuleApplication application;
  private ArtifactContext mockArtifactContext;

  private final File appLocation = new File("fakeLocation");

  TestLogger loggerDefaultMuleApplication = getTestLogger(DefaultMuleApplication.class);
  TestLogger loggerDefaultArtifactDeployer = getTestLogger(DefaultArtifactDeployer.class);

  @Parameterized.Parameters(name = "Parallel: {0}")
  public static List<Boolean> params() {
    return asList(false);
  }

  public LoggingAppStartErrorTestCase(boolean parallelDeployment) {
    super(parallelDeployment);
  }

  @Before
  public void doSetUp() throws Exception {
    MuleApplicationClassLoader parentArtifactClassLoader = mock(MuleApplicationClassLoader.class);
    mockArtifactContext = mock(ArtifactContext.class);
    MuleContextWithRegistry muleContext = mock(MuleContextWithRegistry.class);
    when(mockArtifactContext.getMuleContext()).thenReturn(muleContext);

    DefaultRegistry defaultRegistryMock = mock(DefaultRegistry.class);
    when(mockArtifactContext.getRegistry()).thenReturn(defaultRegistryMock);

    NotificationListenerRegistry notificationListenerRegistryMock = mock(NotificationListenerRegistry.class);
    when(defaultRegistryMock.lookupByType(any())).thenReturn(Optional.of(notificationListenerRegistryMock));

    ApplicationDescriptor applicationDescriptorMock = mock(ApplicationDescriptor.class, RETURNS_DEEP_STUBS.get());
    when(applicationDescriptorMock.getClassLoaderModel())
        .thenReturn(new ClassLoaderModel.ClassLoaderModelBuilder().containing(new URL("file:/target/classes")).build());
    application = new DefaultMuleApplication(applicationDescriptorMock, parentArtifactClassLoader, emptyList(),
        null, mock(ServiceRepository.class),
        mock(ExtensionModelLoaderRepository.class),
        appLocation, null, null,
        getRuntimeLockFactory(),
        mock(MemoryManagementService.class),
        mock(ArtifactConfigurationProcessor.class));

    Method setArtifactContext = application.getClass().getDeclaredMethod("setArtifactContext", ArtifactContext.class);
    setArtifactContext.setAccessible(true);
    setArtifactContext.invoke(application, mockArtifactContext);
  }

  @Test
  public void whenAppThrowsLifecycleExceptionWhileStartingTheErrorShouldBeLogged() throws Exception {
    MuleContext mockedMuleContext = mock(MuleContext.class);
    when(mockArtifactContext.getMuleContext()).thenReturn(mockedMuleContext);
    String expectedLogMessage = "Could not start HTTP server for 'lisConfig' on port 27664: Address already in use";

    MessageSource mockMessageSource = mock(MessageSource.class, withSettings().extraInterfaces(Startable.class, Stoppable.class));
    doThrow(new LifecycleException(createStaticMessage(expectedLogMessage), mockMessageSource)).when(mockedMuleContext).start();

    try {
      application.start();
      fail("Was expecting start to fail");
    } catch (DeploymentStartException e) {
      assertStatus(DEPLOYMENT_FAILED);
      assertThat(loggerDefaultMuleApplication.getAllLoggingEvents().size(), is(1));
      assertThat(loggerDefaultMuleApplication.getAllLoggingEvents().get(0).getMessage(),
          containsString(expectedLogMessage));
    }
  }

  private void assertStatus(final ApplicationStatus status) {
    PollingProber prober = new PollingProber(PROBER_TIMEOUT, PROBER_INTERVAL);
    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        assertThat(application.getStatus(), is(status));
        return true;
      }

      @Override
      public String describeFailure() {
        return format("Application remained at status %s instead of moving to %s", application.getStatus().name(),
            status.name());
      }
    });

  }

  @Test
  public void whenAppFailsWhileStartingTheErrorLogShouldBeCreatedWithTheAppClassloader() throws Exception {
    String expectedLogMessageDefaultMuleApplication = "Failing processor error";
    String expectedLogMessageDefaultArtifactDeployer = "Failed to deploy artifact";
    addPackedAppFromBuilder(dummyErrorAppOnStartDescriptorFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, dummyErrorAppOnStartDescriptorFileBuilder.getId());

    assertThat(loggerDefaultArtifactDeployer.getAllLoggingEvents().size(), is(1));
    assertThat(loggerDefaultArtifactDeployer.getAllLoggingEvents().get(0).getMessage(),
        containsString(expectedLogMessageDefaultArtifactDeployer));

    assertThat(loggerDefaultMuleApplication.getAllLoggingEvents().size(), is(1));
    assertThat(loggerDefaultMuleApplication.getAllLoggingEvents().get(0).getMessage(),
        containsString(expectedLogMessageDefaultMuleApplication));
    assertThat(((MuleArtifactClassLoader) loggerDefaultMuleApplication.getAllLoggingEvents().get(0).getThreadContextClassLoader())
            .getArtifactId(),
        containsString("dummy-error-app-start"));
  }
}
