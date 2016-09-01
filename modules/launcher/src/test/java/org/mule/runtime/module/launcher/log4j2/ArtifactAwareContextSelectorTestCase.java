/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.log4j2;

import static org.apache.commons.lang.StringUtils.EMPTY;
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
import org.mule.runtime.core.util.ValueHolder;
import org.mule.runtime.module.artifact.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.classloader.ShutdownListener;
import org.mule.runtime.module.reboot.MuleContainerBootstrapUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.size.SmallTest;

import java.io.File;

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
  private RegionClassLoader classLoader;

  @Before
  public void before() throws Exception {
    selector = new ArtifactAwareContextSelector();
    when(classLoader.getArtifactName()).thenReturn(getClass().getName());
    when(classLoader.findLocalResource("log4j2.xml")).thenReturn(CONFIG_LOCATION.toURI().toURL());
  }

  @Test
  public void classLoaderToContext() {
    MuleLoggerContext context = (MuleLoggerContext) selector.getContext(EMPTY, classLoader, true);
    assertThat(context, is(sameInstance(selector.getContext(EMPTY, classLoader, true))));

    classLoader = mock(RegionClassLoader.class, RETURNS_DEEP_STUBS);
    when(classLoader.getArtifactName()).thenReturn(getClass().getName());
    assertThat(context, not(sameInstance(selector.getContext(EMPTY, classLoader, true))));
  }

  @Test
  public void shutdownListener() {
    MuleLoggerContext context = getContext();

    ArgumentCaptor<ShutdownListener> captor = ArgumentCaptor.forClass(ShutdownListener.class);
    verify(classLoader).addShutdownListener(captor.capture());
    ShutdownListener listener = captor.getValue();
    assertThat(listener, notNullValue());

    assertThat(context, is(selector.getContext(EMPTY, classLoader, true)));
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
    LoggerContext ctx = selector.getContext("", classLoader, true);
    assertThat(ctx, instanceOf(MuleLoggerContext.class));
    assertConfigurationLocation(ctx);
  }

  @Test
  public void defaultToConfWhenNoConfigFound() {
    when(classLoader.findLocalResource(anyString())).thenReturn(null);
    File expected = new File(MuleContainerBootstrapUtils.getMuleHome(), "conf");
    expected = new File(expected, "log4j2.xml");
    LoggerContext ctx = selector.getContext("", classLoader, true);
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
    return (MuleLoggerContext) selector.getContext("", classLoader, true);
  }

  private void assertConfigurationLocation(LoggerContext ctx) {
    assertThat(ctx.getConfigLocation(), equalTo(CONFIG_LOCATION.toURI()));
  }

  private void assertStopped(final MuleLoggerContext context) {
    final ValueHolder<Boolean> contextWasAccessibleDuringShutdown = new ValueHolder<>(true);
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
