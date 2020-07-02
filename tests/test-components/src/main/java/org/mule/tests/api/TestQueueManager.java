/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.api;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tests.internal.TestQueue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class TestQueueManager {

    private final Map<String, TestQueue> queuesMap = new ConcurrentHashMap<>();

    public TestQueue get(String configName) {
        return queuesMap.computeIfAbsent(configName, TestQueue::new);
    }

    public TestQueue remove(String configName) {
        return queuesMap.remove(configName);
    }

    public CoreEvent read(String configName, long timeout, TimeUnit timeUnit) {
        try {
            return get(configName).pop(timeout, timeUnit);
        } catch (InterruptedException e) {
            return null;
        }
    }

    public void write(String configName, CoreEvent event) {
        try {
            get(configName).push(event);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    public int countPendingEvents(String configName) {
        return get(configName).countPendingEvents();
    }
}
