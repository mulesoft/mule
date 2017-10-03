/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.context.notification.MuleContextNotification.CONTEXT_INITIALISED;

import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.api.context.notification.MuleContextNotification;
import org.mule.runtime.core.internal.registry.DefaultRegistry;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.application.ApplicationStatus;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.internal.application.MuleApplicationClassLoader;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderRepository;
import org.mule.runtime.module.service.api.manager.ServiceRepository;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;

import org.junit.Test;

import java.io.File;

import javax.inject.Inject;

/**
 * This tests verifies that the {@link DefaultMuleApplication} status is set correctly depending on its
 * {@link org.mule.runtime.core.api.MuleContext}'s lifecycle phase
 */
public class DefaultMuleApplicationStatusTestCase extends AbstractMuleContextTestCase {

  private static final int PROBER_TIMEOUT = 1000;
  private static final int PROBER_INTERVAL = 100;

  @Inject
  private NotificationDispatcher notificationDispatcher;

  private DefaultMuleApplication application;
  private File appLocation = new File("fakeLocation");

  @Override
  protected void doSetUp() throws Exception {
    MuleApplicationClassLoader parentArtifactClassLoader = mock(MuleApplicationClassLoader.class);
    ArtifactContext mockArtifactContext = mock(ArtifactContext.class);
    when(mockArtifactContext.getMuleContext()).thenReturn(muleContext);
    when(mockArtifactContext.getRegistry()).thenReturn(new DefaultRegistry(muleContext));
    application = new DefaultMuleApplication(null, parentArtifactClassLoader, emptyList(),
                                             null, mock(ServiceRepository.class), mock(ExtensionModelLoaderRepository.class),
                                             appLocation, null, null);
    application.setArtifactContext(mockArtifactContext);

    muleContext.getInjector().inject(this);
  }

  @Test
  public void initialState() {
    assertStatus(ApplicationStatus.CREATED);
  }

  @Test
  public void initialised() {
    // the context was initialised before we gave it to the application, so we need
    // to fire the notification again since the listener wasn't there
    notificationDispatcher.dispatch(new MuleContextNotification(muleContext, CONTEXT_INITIALISED));
    assertStatus(ApplicationStatus.INITIALISED);
  }

  @Test
  public void started() throws Exception {
    muleContext.start();
    assertStatus(ApplicationStatus.STARTED);
  }

  @Test
  public void stopped() throws Exception {
    muleContext.start();
    muleContext.stop();
    assertStatus(ApplicationStatus.STOPPED);
  }

  @Test
  public void destroyed() {
    muleContext.dispose();
    assertStatus(ApplicationStatus.DESTROYED);
  }

  @Test
  public void nullDeploymentClassLoaderAfterDispose() {
    ApplicationDescriptor descriptor = mock(ApplicationDescriptor.class);
    when(descriptor.getConfigResources()).thenReturn(emptyList());

    DefaultMuleApplication application =
        new DefaultMuleApplication(descriptor, mock(MuleApplicationClassLoader.class), emptyList(), null,
                                   null, null, appLocation, null, null);
    application.install();
    assertThat(application.deploymentClassLoader, is(notNullValue()));
    application.dispose();
    assertThat(application.deploymentClassLoader, is(nullValue()));
  }

  @Test
  public void deploymentFailedOnInit() {
    try {
      application.init();
      fail("Was expecting init to fail");
    } catch (Exception e) {
      assertStatus(ApplicationStatus.DEPLOYMENT_FAILED);
    }
  }

  @Test
  public void deploymentFailedOnStart() throws Exception {
    try {
      application.start();
      fail("Was expecting start to fail");
    } catch (Exception e) {
      muleContext.stop();
      muleContext.dispose();
      assertStatus(ApplicationStatus.DEPLOYMENT_FAILED);
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
        return String.format("Application remained at status %s instead of moving to %s", application.getStatus().name(),
                             status.name());
      }
    });

  }
}
