/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.api.config.MuleProperties.MULE_DEFAULT_PROCESSING_STRATEGY;
import static org.mule.config.spring.util.ProcessingStrategyUtils.QUEUED_THREAD_PER_PROCESSOR_PROCESSING_STRATEGY;
import org.mule.api.processor.ProcessingStrategy;
import org.mule.config.spring.util.ProcessingStrategyUtils;
import org.mule.processor.strategy.QueuedAsynchronousProcessingStrategy;
import org.mule.processor.strategy.QueuedThreadPerProcessorProcessingStrategy;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class SystemPropertyProcessingStrategyConfigTestCase extends FunctionalTestCase
{

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"Container level system property", new String[] {}, QueuedThreadPerProcessorProcessingStrategy.class},
                {"Configuration overrides system property", new String[] {"configuration-processing-strategy-config.xml"}, QueuedAsynchronousProcessingStrategy.class}
        });
    }

    private final String[] configFiles;
    private Class<? extends ProcessingStrategy> expectedStrategyType;

    @Rule
    public SystemProperty globalProcessingStrategy = new SystemProperty(MULE_DEFAULT_PROCESSING_STRATEGY, QUEUED_THREAD_PER_PROCESSOR_PROCESSING_STRATEGY);

    @Rule
    public SystemProperty localProcessingStrategy = new SystemProperty("processingStrategy", ProcessingStrategyUtils.QUEUED_ASYNC_PROCESSING_STRATEGY);

    public SystemPropertyProcessingStrategyConfigTestCase(String name, String[] configFiles, Class<? extends ProcessingStrategy> expectedStrategyType)
    {
        this.configFiles = configFiles;
        this.expectedStrategyType = expectedStrategyType;
    }

    @Override
    protected String[] getConfigFiles()
    {
        return configFiles;
    }

    @Test
    public void assertDefaultProcessingStrategy() throws Exception
    {
        assertThat(muleContext.getConfiguration().getDefaultProcessingStrategy(), is(instanceOf(expectedStrategyType)));
    }
}
