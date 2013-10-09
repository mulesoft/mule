/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.lifecycle;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class JSR250ObjectLifecycleTracker implements MuleContextAware
    {
        private final List<String> tracker = new ArrayList<String>();

        public List<String> getTracker() {
            return tracker;
        }

        public void setMuleContext(MuleContext context)
        {
            tracker.add("setMuleContext");
        }

        @PostConstruct
        public void init()
        {
            tracker.add("initialise");
        }

        @PreDestroy
        public void dispose()
        {
            tracker.add("dispose");
        }
    }
