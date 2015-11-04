/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.ConfigurationException;
import org.mule.api.context.MuleContextBuilder;
import org.mule.api.context.MuleContextFactory;
import org.mule.api.context.notification.MuleContextNotificationListener;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.context.notification.MuleContextNotification;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.ForceXalanTransformerFactory;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.util.concurrent.Latch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ExceptionStrategyConfigurationFailuresTestCase extends AbstractMuleTestCase
{

    @Rule
    public SystemProperty useXalan;

    /**
     * Verify that regardless of the XML library used, validation errors are handled correctly.
     * @return
     */
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                                             {false},
                                             {true}
        });
    }

    public ExceptionStrategyConfigurationFailuresTestCase(boolean isUseXalan)
    {
        if (isUseXalan)
        {
            useXalan = new ForceXalanTransformerFactory();
        }
        else
        {
            useXalan = null;
        }
    }

    @Test(expected = ConfigurationException.class)
    public void testNamedFlowExceptionStrategyFails() throws Exception
    {
        loadConfiguration("org/mule/test/integration/exceptions/named-flow-exception-strategy.xml");
    }

    @Test(expected = ConfigurationException.class)
    public void testNamedServiceExceptionStrategyFails() throws Exception
    {
        loadConfiguration("org/mule/test/integration/exceptions/named-service-exception-strategy.xml");
    }

    @Test(expected = ConfigurationException.class)
    public void testReferenceExceptionStrategyAsGlobalExceptionStrategy() throws Exception
    {
        loadConfiguration("org/mule/test/integration/exceptions/reference-global-exception-strategy.xml");
    }

    @Test(expected = ConfigurationException.class)
    public void testChoiceExceptionStrategyCantHaveMiddleExceptionStrategyWithoutExpression() throws Exception
    {
        loadConfiguration("org/mule/test/integration/exceptions/exception-strategy-in-choice-without-expression.xml");
    }

    @Test(expected = ConfigurationException.class)
    public void testChoiceExceptionStrategyCantHaveDefaultExceptionStrategy() throws Exception
    {
        loadConfiguration("org/mule/test/integration/exceptions/default-exception-strategy-in-choice.xml");
    }

    @Test(expected = ConfigurationException.class)
    public void testDefaultEsFailsAsReferencedExceptionStrategy() throws Exception
    {
        loadConfiguration("org/mule/test/integration/exceptions/default-es-as-referenced-exception-strategy.xml");
    }

    @Test(expected = ConfigurationException.class)
    public void testDefaultExceptionStrategyReferencesNonExistentExceptionStrategy() throws Exception
    {
        loadConfiguration("org/mule/test/integration/exceptions/default-exception-strategy-reference-non-existent-es.xml");
    }

    @Test(expected = ConfigurationException.class)
    public void testDefaultExceptionStrategyReferencesExceptionStrategyWithExpression() throws Exception
    {
        loadConfiguration("org/mule/test/integration/exceptions/default-exception-strategy-reference-has-expression.xml");
    }

    @Test(expected = ConfigurationException.class)
    public void testChoiceExceptionStrategyWithMultipleHandleRedeliveryExceptionStrategies() throws Exception
    {
        loadConfiguration("org/mule/test/integration/exceptions/choice-exception-strategy-multiple-rollback.xml");
    }

    @Test(expected = ConfigurationException.class)
    public void testChoiceExceptionStrategyWithMultipleHandleRedeliveryExceptionStrategiesWithGlobalAndDefault() throws Exception
    {
        loadConfiguration("org/mule/test/integration/exceptions/choice-exception-strategy-multiple-rollback-global.xml");
    }

    @Test(expected = ConfigurationException.class)
    public void testCatchExceptionStrategyWithWhenWithoutChoice() throws Exception
    {
        loadConfiguration("org/mule/test/integration/exceptions/when-without-choice-in-catch-es.xml");
    }

    @Test(expected = ConfigurationException.class)
    public void testRollbackExceptionStrategyWithWhenWithoutChoice() throws Exception
    {
        loadConfiguration("org/mule/test/integration/exceptions/when-without-choice-in-rollback-es.xml");
    }

    private void loadConfiguration(String configuration) throws MuleException, InterruptedException
    {
        MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
        List<ConfigurationBuilder> builders = new ArrayList<ConfigurationBuilder>();
        builders.add(new SpringXmlConfigurationBuilder(configuration));
        MuleContextBuilder contextBuilder = new DefaultMuleContextBuilder();
        MuleContext muleContext = muleContextFactory.createMuleContext(builders, contextBuilder);
        final AtomicReference<Latch> contextStartedLatch = new AtomicReference<Latch>();
        contextStartedLatch.set(new Latch());
        muleContext.registerListener(new MuleContextNotificationListener<MuleContextNotification>()
        {
            @Override
            public void onNotification(MuleContextNotification notification)
            {
                if (notification.getAction() == MuleContextNotification.CONTEXT_STARTED)
                {
                    contextStartedLatch.get().countDown();
                }
            }
        });
        muleContext.start();
        contextStartedLatch.get().await(20, TimeUnit.SECONDS);
    }

}
