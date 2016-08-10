/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.registry.ResolverException;
import org.mule.runtime.core.component.ComponentException;
import org.mule.runtime.core.config.i18n.CoreMessages;

import java.sql.SQLDataException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ChoiceExceptionStrategyTestCase extends AbstractIntegrationTestCase
{
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/exceptions/choice-exception-strategy.xml";
    }

    @Test
    public void testMatchesCorrectExceptionStrategy() throws Exception
    {
        callAndThrowException(new IllegalStateException(), "0 catch-2");
    }

    @Test
    public void testMatchesCorrectExceptionStrategyUsingWrapper() throws Exception
    {
        callAndThrowException(new ResolverException(CoreMessages.createStaticMessage(""), new IllegalStateException()), "0 catch-2");
    }

    @Test
    public void testMatchesCorrectExceptionStrategyUsingWrapperAndCause() throws Exception
    {
        callAndThrowException(new ResolverException(CoreMessages.createStaticMessage(""), new RuntimeException(new IllegalStateException())), "0 catch-2");
    }

    @Test
    public void testMatchesCorrectExceptionStrategyUsingBaseClass() throws Exception
    {
        callAndThrowException(new BaseException(), "0 catch-3");
    }

    @Test
    public void testMatchesCorrectExceptionStrategyUsingSubtypeClass() throws Exception
    {
        callAndThrowException(new ResolverException(CoreMessages.createStaticMessage(""), new SubtypeException()), "0 catch-4");
    }

    @Test
    public void testMatchesCorrectExceptionStrategyUsingSubtypeSubtypeClass() throws Exception
    {
        callAndThrowException(new SubtypeSubtypeException(), "0 catch-4");
    }

    @Test
    public void testMatchesCorrectExceptionStrategyUsingRegex() throws Exception
    {
        callAndThrowException(new AnotherTypeMyException(), "0 catch-5");
    }
    
    @Test
    public void testMatchesCorrectExceptionStrategyUsingGroovyExpressionEvaluator() throws Exception
    {
        callAndThrowException("groovy", new SQLDataException(), "groovy catch-6");
    }

    @Test
    public void testMatchesCorrectExceptionStrategyUsingStartsWithWildcard() throws Exception
    {
        callAndThrowException(new StartsWithException(), "0 catch-7");
    }

    @Test
    public void testMatchesCorrectExceptionStrategyUsingFinishesWithWildcard() throws Exception
    {
        callAndThrowException(new ThisExceptionFinishesWithException(), "0 catch-8");
    }

    @Test
    public void testMatchesCorrectExceptionStrategyUsingMatchesAll() throws Exception
    {
        callAndThrowException(new AnotherTotallyDifferentKindOfException(), "0 catch-9");
    }
    
    @Test
    public void testMatchesCorrectExceptionStrategyUsingFinishesWithSomethingElse() throws Exception
    {
        callAndThrowException(new ThisExceptionFinishesWithSomethingElse(), "0 groovified");
    }

    @Test
    public void testMatchesCorrectExceptionUsingNoCause() throws Exception
    {
        expectedException.expect(ComponentException.class);
        expectedException.expectCause(instanceOf(ResolverException.class));
        callAndThrowException(new ResolverException(CoreMessages.createStaticMessage("")), null);
    }

    @Test
    public void testNoMatchThenCallDefaultExceptionStrategy() throws Exception
    {
        callAndThrowException(new ArithmeticException(), "0 global catch es");
    }

    private void callAndThrowException(final Exception exceptionToThrow, final String expectedMessage) throws Exception
    {
        callAndThrowException("0", exceptionToThrow, expectedMessage);
    }
    
    private void callAndThrowException(Object payload, final Exception exceptionToThrow, final String expectedMessage) throws Exception
    {
        FunctionalTestComponent ftc = getFunctionalTestComponent("matchesCorrectExceptionStrategyUsingExceptionType");
        ftc.setEventCallback((context, component) ->
        {
            throw exceptionToThrow;
        });
        MuleMessage response = flowRunner("matchesCorrectExceptionStrategyUsingExceptionType").withPayload(payload).run().getMessage();
        assertThat(getPayloadAsString(response), is(expectedMessage));
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
