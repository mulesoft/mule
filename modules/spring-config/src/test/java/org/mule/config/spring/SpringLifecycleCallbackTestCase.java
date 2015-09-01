/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class SpringLifecycleCallbackTestCase extends AbstractMuleTestCase
{

    private SpringRegistryLifecycleManager springRegistryLifecycleManager;

    @Mock
    private SpringRegistry springRegistry;

    @Mock
    private MuleContext muleContext;

    private SpringLifecycleCallback callback;

    @Before
    public void before()
    {
        springRegistryLifecycleManager = new SpringRegistryLifecycleManager("id", springRegistry, muleContext);
        springRegistryLifecycleManager.registerPhases();

        callback = new SpringLifecycleCallback(springRegistryLifecycleManager);
    }

    @Test
    public void phaseAppliesInDependencyOrder() throws Exception
    {
        Map<String, Initialisable> objects = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++)
        {
            final String key = String.valueOf(i);
            objects.put(key, newInitialisable());
        }

        Map<String, ?> childsOf1 = new LinkedHashMap<>(objects);
        childsOf1.remove("1");
        childsOf1.remove("4");
        childsOf1.remove("5");

        Map<String, Object> childsOf4 = new LinkedHashMap<>();
        childsOf4.put("5", objects.get("5"));

        when(springRegistry.getDependencies("1")).thenReturn((Map<String, Object>) childsOf1);
        when(springRegistry.getDependencies("4")).thenReturn(childsOf4);
        when(springRegistry.lookupEntriesForLifecycle(Initialisable.class)).thenReturn(objects);
        InOrder inOrder = inOrder(objects.values().toArray());

        callback.onTransition(Initialisable.PHASE_NAME, springRegistry);

        verifyInitialisation(inOrder, objects, "2", "3", "1", "5", "4");
    }

    private void verifyInitialisation(InOrder inOrder, Map<String, Initialisable> objects, String... keys) throws InitialisationException
    {
        for (String key : keys)
        {
            inOrder.verify(objects.get(key)).initialise();
        }
    }

    private Initialisable newInitialisable() throws Exception
    {
        return mock(Initialisable.class);
    }

}
