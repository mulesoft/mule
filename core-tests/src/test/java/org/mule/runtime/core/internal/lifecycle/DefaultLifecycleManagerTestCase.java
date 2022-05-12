package org.mule.runtime.core.internal.lifecycle;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.core.api.lifecycle.LifecycleCallback;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class DefaultLifecycleManagerTestCase extends AbstractMuleTestCase {

  @Test(expected = LifecycleException.class)
  public void testDefaultLifecycleManagerTestCaseWrapsDefaultMuleExceptionIntoLifecycleException() throws MuleException {
    Lifecycle configurationProviderToolingAdapter = mock(Lifecycle.class);
    LifecycleCallback lifecycleCallback = mock(LifecycleCallback.class);
    doThrow(new DefaultMuleException("Test exception")).when(lifecycleCallback).onTransition(any(), any());
    
    DefaultLifecycleManager defaultLifecycleManager = new DefaultLifecycleManager("org.mule.runtime.module.extension.internal.runtime.config.ConfigurationProviderToolingAdapter-lisConfig", configurationProviderToolingAdapter);
    DefaultLifecycleManager defaultLifecycleManagerSpy = spy(defaultLifecycleManager);
    doNothing().when(defaultLifecycleManagerSpy).checkPhase(any());
    
    defaultLifecycleManagerSpy.fireStartPhase(lifecycleCallback);
  }
}
