/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;
import static org.mule.test.config.dsl.LifecycleAction.DISPOSE;
import static org.mule.test.config.dsl.LifecycleAction.GET_OBJECT;
import static org.mule.test.config.dsl.LifecycleAction.INITIALISE;
import static org.mule.test.config.dsl.LifecycleAction.START;
import static org.mule.test.config.dsl.LifecycleAction.STOP;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.construct.Flow;
import org.mule.test.config.dsl.LifecycleSensingMessageProcessor;
import org.mule.test.config.dsl.LifecycleSensingObjectFactory;

import org.junit.Test;

public class ComponentLifecycleTestCase extends AbstractIntegrationTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/spring/component-lifecycle-config.xml";
    }

    @Test
    public void globalElementLifecycle()
    {
        LifecycleSensingMessageProcessor lifecycleSensingMessageProcessor = muleContext.getRegistry().get("globalElement");
        assertObjectFactoryAndMessageProcessorLifecycle(lifecycleSensingMessageProcessor);
    }

    @Test
    public void innerElementLifecycle()
    {
        Flow flow = muleContext.getRegistry().get("flow");
        LifecycleSensingMessageProcessor lifecycleSensingMessageProcessor = (LifecycleSensingMessageProcessor) flow.getMessageProcessors().get(0);
        assertObjectFactoryAndMessageProcessorLifecycle(lifecycleSensingMessageProcessor);
    }

    private void assertObjectFactoryAndMessageProcessorLifecycle(LifecycleSensingMessageProcessor lifecycleSensingMessageProcessor)
    {
        LifecycleSensingObjectFactory lifecycleSensingObjectFactory = lifecycleSensingMessageProcessor.getObjectFactory();
        assertThat(lifecycleSensingObjectFactory.getLifecycleActions(), hasItems(GET_OBJECT));
        assertThat(lifecycleSensingMessageProcessor.getLifecycleActions(), hasItems(INITIALISE, START));
        muleContext.dispose();
        assertThat(lifecycleSensingObjectFactory.getLifecycleActions(), hasItems(GET_OBJECT));
        assertThat(lifecycleSensingMessageProcessor.getLifecycleActions(), hasItems(INITIALISE, START, STOP, DISPOSE));
    }
}
