/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.registry.ResolverException;
import org.mule.config.i18n.CoreMessages;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;

import java.sql.SQLDataException;

import org.junit.Test;

public class ChoiceExceptionStrategyTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/exceptions/choice-exception-strategy.xml";
    }

    @Test
    public void testMatchesCorrectExceptionStrategy() throws Exception
    {
        callVmAndThrowException(new IllegalStateException(),"0 catch-2");
    }

    @Test
    public void testMatchesCorrectExceptionStrategyUsingWrapper() throws Exception
    {
        callVmAndThrowException(new ResolverException(CoreMessages.createStaticMessage(""), new IllegalStateException()), "0 catch-2");
    }

    @Test
    public void testMatchesCorrectExceptionStrategyUsingWrapperAndCause() throws Exception
    {
        callVmAndThrowException(new ResolverException(CoreMessages.createStaticMessage(""), new RuntimeException(new IllegalStateException())), "0 catch-2");
    }

    @Test
    public void testMatchesCorrectExceptionStrategyUsingBaseClass() throws Exception
    {
        callVmAndThrowException(new BaseException(), "0 catch-3");
    }

    @Test
    public void testMatchesCorrectExceptionStrategyUsingSubtypeClass() throws Exception
    {
        callVmAndThrowException(new ResolverException(CoreMessages.createStaticMessage(""), new SubtypeException()), "0 catch-4");
    }

    @Test
    public void testMatchesCorrectExceptionStrategyUsingSubtypeSubtypeClass() throws Exception
    {
        callVmAndThrowException(new SubtypeSubtypeException(), "0 catch-4");
    }

    @Test
    public void testMatchesCorrectExceptionStrategyUsingRegex() throws Exception
    {
        callVmAndThrowException(new AnotherTypeMyException(), "0 catch-5");
    }
    
    @Test
    public void testMatchesCorrectExceptionStrategyUsingGroovyExpressionEvaluator() throws Exception
    {
        callVmAndThrowException("groovy", new SQLDataException(), "groovy catch-6");
    }

    @Test
    public void testMatchesCorrectExceptionStrategyUsingStartsWithWildcard() throws Exception
    {
        callVmAndThrowException(new StartsWithException(), "0 catch-7");
    }

    @Test
    public void testMatchesCorrectExceptionStrategyUsingFinishesWithWildcard() throws Exception
    {
        callVmAndThrowException(new ThisExceptionFinishesWithException(), "0 catch-8");
    }

    @Test
    public void testMatchesCorrectExceptionStrategyUsingMatchesAll() throws Exception
    {
        callVmAndThrowException(new AnotherTotallyDifferentKindOfException(), "0 catch-9");
    }
    
    @Test
    public void testMatchesCorrectExceptionStrategyUsingFinishesWithSomethingElse() throws Exception
    {
        callVmAndThrowException(new ThisExceptionFinishesWithSomethingElse(), "0 groovified");
    }

    @Test
    public void testMatchesCorrectExceptionUsingNoCause() throws Exception
    {
        callVmAndThrowException(new ResolverException(CoreMessages.createStaticMessage("")), "{NullPayload}");
    }

    @Test
    public void testNoMatchThenCallDefaultExceptionStrategy() throws Exception
    {
        callVmAndThrowException(new ArithmeticException(),"0 global catch es");
    }

    private void callVmAndThrowException(final Exception exceptionToThrow, final String expectedMessage) throws Exception
    {
        callVmAndThrowException("0", exceptionToThrow, expectedMessage);
    }
    
    private void callVmAndThrowException(Object payload, final Exception exceptionToThrow, final String expectedMessage) throws Exception
    {
        MuleClient client = muleContext.getClient();
        FunctionalTestComponent ftc = getFunctionalTestComponent("matchesCorrectExceptionStrategyUsingExceptionType");
        ftc.setEventCallback(new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                throw exceptionToThrow;
            }
        });
        MuleMessage response = client.send("vm://in", payload, null);
        assertThat(response.getPayloadAsString(), is(expectedMessage));
    }

    public static class BaseException extends Exception
    {
    }

    public static class SubtypeException extends BaseException
    {
    }

    public static class SubtypeSubtypeException extends SubtypeException
    {
    }

    public static class AnotherTypeMyException extends Exception
    {
    }

    public static class StartsWithException extends Exception
    {
    }

    public static class ThisExceptionFinishesWithException extends Exception
    {
    }

    public static class ThisExceptionFinishesWithSomethingElse extends Exception
    {
    }

    public static class AnotherTotallyDifferentKindOfException extends Exception
    {
    }
}
