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
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.config.spring.util.ProcessingStrategyUtils;
import org.mule.runtime.core.api.processor.ProcessingStrategy;
import org.mule.runtime.core.construct.flow.DefaultFlowProcessingStrategy;
import org.mule.runtime.core.processor.strategy.AsynchronousProcessingStrategy;
import org.mule.runtime.core.processor.strategy.SynchronousProcessingStrategy;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ConfigurationProcessingStrategyParserTestCase extends FunctionalTestCase
{

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {ProcessingStrategyUtils.DEFAULT_PROCESSING_STRATEGY, DefaultFlowProcessingStrategy.class},
                {ProcessingStrategyUtils.SYNC_PROCESSING_STRATEGY, SynchronousProcessingStrategy.class},
                {ProcessingStrategyUtils.ASYNC_PROCESSING_STRATEGY, AsynchronousProcessingStrategy.class},
        });
    }

    private final Class<? extends ProcessingStrategy> expectedStrategyType;

    @Rule
    public SystemProperty processingStrategyProperty;

    public ConfigurationProcessingStrategyParserTestCase(String defaultProcessingStrategy, Class<? extends ProcessingStrategy> expectedStrategyType)
    {
        this.expectedStrategyType = expectedStrategyType;
        processingStrategyProperty = new SystemProperty("processingStrategy", defaultProcessingStrategy);
    }

    @Override
    protected String getConfigFile()
    {
        return "configuration-processing-strategy-config.xml";
    }

    @Test
    public void verifyConfigurationProcessingStrategy() throws Exception
    {
        assertThat(muleContext.getConfiguration().getDefaultProcessingStrategy(), is(instanceOf(expectedStrategyType)));
    }
}
