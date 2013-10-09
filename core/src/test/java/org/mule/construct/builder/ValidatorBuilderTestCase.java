/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.construct.builder;

import static org.junit.Assert.assertEquals;

import org.mule.construct.Validator;
import org.mule.exception.DefaultMessagingExceptionStrategy;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class ValidatorBuilderTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testConfigurationWithoutErrorExpression() throws Exception
    {
        final Validator validator = new ValidatorBuilder().name("test-validator-no-error")
            .inboundAddress("test://foo.in")
            .validationFilter(new PayloadTypeFilter(Integer.class))
            .ackExpression("#[string:GOOD:#[message:payload]]")
            .nackExpression("#[string:BAD:#[message:payload]]")
            .outboundAddress("test://foo.out")
            .exceptionStrategy(new DefaultMessagingExceptionStrategy(muleContext))
            .build(muleContext);

        assertEquals("test-validator-no-error", validator.getName());
    }

    @Test
    public void testFullConfiguration() throws Exception
    {
        final Validator validator = new ValidatorBuilder().name("test-validator-full")
            .inboundAddress("test://foo.in")
            .validationFilter(new PayloadTypeFilter(Integer.class))
            .ackExpression("#[string:GOOD:#[message:payload]]")
            .nackExpression("#[string:BAD:#[message:payload]]")
            .errorExpression("#[string:ERROR:#[message:payload]]")
            .outboundAddress("test://foo.out")
            .exceptionStrategy(new DefaultMessagingExceptionStrategy(muleContext))
            .build(muleContext);

        assertEquals("test-validator-full", validator.getName());
    }
}
