/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.log4j2;

import static java.util.Optional.empty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_LOG_CONTEXT_DISPOSE_DELAY_MILLIS;
import static org.mule.runtime.module.launcher.log4j2.LoggerContextReaperThreadFactory.THREAD_NAME;
import static org.mule.tck.MuleTestUtils.getRunningThreadByName;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ShutdownListener;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.reboot.api.MuleContainerBootstrapUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.size.SmallTest;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ArtifactAwareContextSelectorTestCase extends AbstractMuleTestCase {

  private static final File CONFIG_LOCATION = new File("my/local/log4j2.xml");
  private static final int PROBER_TIMEOUT = 5000;
  private static final int PROBER_FREQ = 500;

  @Rule
  public SystemProperty disposeDelay = new SystemProperty(MULE_LOG_CONTEXT_DISPOSE_DELAY_MILLIS, "200");

  private ArtifactAwareContextSelector selector;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private RegionClassLoader regionClassLoader;

  @Mock
  private ArtifactDescriptor artifactDescriptor;

  @Before
  public void before() throws Exception {
    selector = new ArtifactAwareContextSelector();

    when(artifactDescriptor.getDeploymentProperties()).thenReturn(empty());
    when(regionClassLoader.getArtifactId()).thenReturn(getClass().getName());
    when(regionClassLoader.findLocalResource("log4j2.xml")).thenReturn(CONFIG_LOCATION.toURI().toURL());
    when(regionClassLoader.getArtifactDescriptor()).thenReturn(artifactDescriptor);
  }

  @Test
  public void classLoaderToContext() {
    MuleLoggerContext context = (MuleLoggerContext) selector.getContext(EMPTY, regionClassLoader, true);
    assertThat(context, is(sameInstance(selector.getContext(EMPTY, regionClassLoader, true))));

    regionClassLoader = mock(RegionClassLoader.class, RETURNS_DEEP_STUBS);
    when(regionClassLoader.getArtifactId()).thenReturn(getClass().getName());
    when(regionClassLoader.getArtifactDescriptor()).thenReturn(artifactDescriptor);
    assertThat(context, not(sameInstance(selector.getContext(EMPTY, regionClassLoader, true))));
  }

  @Test
  public void shutdownListener() {
    MuleLoggerContext context = getContext();

    ArgumentCaptor<ShutdownListener> captor = ArgumentCaptor.forClass(ShutdownListener.class);
    verify(regionClassLoader).addShutdownListener(captor.capture());
    ShutdownListener listener = captor.getValue();
    assertThat(listener, notNullValue());

    assertThat(context, is(selector.getContext(EMPTY, regionClassLoader, true)));
    listener.execute();

    assertStopped(context);
  }

  @Test
  public void dispose() throws Exception {
    MuleLoggerContext context = getContext();

    selector.dispose();
    assertStopped(context);
  }

  @Test
  public void returnsMuleLoggerContext() {
    LoggerContext ctx = selector.getContext("", regionClassLoader, true);
    assertThat(ctx, instanceOf(MuleLoggerContext.class));
    assertConfigurationLocation(ctx);
  }

  @Test
  public void defaultToConfWhenNoConfigFound() {
    when(regionClassLoader.findLocalResource(anyString())).thenReturn(null);
    File expected = new File(MuleContainerBootstrapUtils.getMuleHome(), "conf");
    expected = new File(expected, "log4j2.xml");
    LoggerContext ctx = selector.getContext("", regionClassLoader, true);
    assertThat(ctx.getConfigLocation(), equalTo(expected.toURI()));
  }

  @Test
  public void usesLoggerContextReaperThread() {
    assertReaperThreadNotRunning();

    MuleLoggerContext context = getContext();
    selector.removeContext(context);

    Thread thread = getReaperThread();
    assertThat(thread, is(notNullValue()));
  }

  @Test
  public void returnsMuleLoggerContextForArtifactClassLoaderChild() {
    ClassLoader childClassLoader = new URLClassLoader(new URL[0], regionClassLoader);
    LoggerContext parentCtx = selector.getContext("", regionClassLoader, true);
    LoggerContext childCtx = selector.getContext("", childClassLoader, true);
    assertThat(childCtx, instanceOf(MuleLoggerContext.class));
    assertThat(childCtx, sameInstance(parentCtx));
  }

  @Test
  public void returnsMuleLoggerContextForInternalArtifactClassLoader() {
    ArtifactClassLoader serviceClassLoader =
        new MuleArtifactClassLoader("test", new ApplicationDescriptor("test"), new URL[0], this.getClass().getClassLoader(),
                                    mock(ClassLoaderLookupPolicy.class));
    LoggerContext systemContext = selector.getContext("", this.getClass().getClassLoader(), true);
    LoggerContext serviceCtx = selector.getContext("", serviceClassLoader.getClassLoader(), true);
    assertThat(serviceCtx, instanceOf(MuleLoggerContext.class));
    assertThat(serviceCtx, sameInstance(systemContext));
  }

  private void assertReaperThreadNotRunning() {
    PollingProber prober = new PollingProber(PROBER_TIMEOUT, PROBER_FREQ);
    prober.check(new Probe() {

      @Override
      public boolean isSatisfied() {
        return getReaperThread() == null;
      }

      @Override
      public String describeFailure() {
        return "Reaper thread exists from previous test and did not died";
      }
    });
  }

  private Thread getReaperThread() {
    return getRunningThreadByName(THREAD_NAME);
  }

  private MuleLoggerContext getContext() {
    return (MuleLoggerContext) selector.getContext("", regionClassLoader, true);
  }

  private void assertConfigurationLocation(LoggerContext ctx) {
    assertThat(ctx.getConfigLocation(), equalTo(CONFIG_LOCATION.toURI()));
  }

  private void assertStopped(final MuleLoggerContext context) {
    final Reference<Boolean> contextWasAccessibleDuringShutdown = new Reference<>(true);
    PollingProber pollingProber = new PollingProber(1000, 10);
    pollingProber.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        if (context.getState().equals(LifeCycle.State.STOPPED)) {
          return true;
        } else {
          LoggerContext currentContext = getContext();
          if (currentContext != null && currentContext != context) {
            contextWasAccessibleDuringShutdown.set(false);
          }
          return false;
        }
      }

      @Override
      public String describeFailure() {
        return "context was not stopped";
      }
    });

    assertThat(context, not(getContext()));
    assertThat(contextWasAccessibleDuringShutdown.get(), is(true));
  }
}
