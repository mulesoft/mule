/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.mule.tck.util.TestUtils.loadConfiguration;

import org.mule.api.config.ConfigurationException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.ForceXalanTransformerFactory;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.Arrays;
import java.util.Collection;

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
     * 
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


}
