/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.internal;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.tck.util.MuleContextUtils.mockNotificationsHandling;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.config.spring.internal.dsl.model.ConfigurationDependencyResolver;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.LinkedHashMap;
import java.util.Map;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class SpringLifecycleCallbackTestCase extends AbstractMuleTestCase {

  private SpringRegistryLifecycleManager springRegistryLifecycleManager;

  @Mock
  private SpringRegistry springRegistry;

  @Mock
  private MuleContext muleContext;

  private SpringLifecycleCallback callback;

  @Before
  public void before() throws RegistrationException {
    final MuleRegistry registry = mock(MuleRegistry.class);
    mockNotificationsHandling(registry);
    when(muleContext.getRegistry()).thenReturn(registry);
    springRegistry = mock(SpringRegistry.class, RETURNS_DEEP_STUBS);
    springRegistryLifecycleManager =
        new SpringRegistryLifecycleManager("id", springRegistry, muleContext);
    springRegistryLifecycleManager.registerPhases(springRegistry);

    callback = new SpringLifecycleCallback(springRegistryLifecycleManager, springRegistry);
  }

  @Test
  public void phaseAppliesInDependencyOrder() throws Exception {
    Map<String, Initialisable> objects = new LinkedHashMap<>();
    for (int i = 1; i <= 5; i++) {
      final String key = String.valueOf(i);
      Initialisable object = newInitialisable();
      objects.put(key, object);
      when(springRegistry.get(key)).thenReturn(object);
    }

    Map<String, ?> childsOf1 = new LinkedHashMap<>(objects);
    childsOf1.remove("1");
    childsOf1.remove("4");
    childsOf1.remove("5");

    Map<String, Object> childsOf4 = new LinkedHashMap<>();
    childsOf4.put("5", objects.get("5"));

    when(springRegistry.getBeanDependencyResolver())
        .thenReturn(new DefaultBeanDependencyResolver(mock(ConfigurationDependencyResolver.class, RETURNS_DEEP_STUBS),
                                                      springRegistry));
    when(springRegistry.getDependencies("1")).thenReturn((Map<String, Object>) childsOf1);
    when(springRegistry.getDependencies("4")).thenReturn(childsOf4);
    when(springRegistry.lookupEntriesForLifecycle(Initialisable.class)).thenReturn(objects);
    InOrder inOrder = inOrder(objects.values().toArray());

    callback.onTransition(Initialisable.PHASE_NAME, springRegistry);

    verifyInitialisation(inOrder, objects, "2", "3", "1", "5", "4");
  }

  private void verifyInitialisation(InOrder inOrder, Map<String, Initialisable> objects, String... keys)
      throws InitialisationException {
    for (String key : keys) {
      inOrder.verify(objects.get(key)).initialise();
    }
  }

  private Initialisable newInitialisable() throws Exception {
    return mock(Initialisable.class);
  }

}
