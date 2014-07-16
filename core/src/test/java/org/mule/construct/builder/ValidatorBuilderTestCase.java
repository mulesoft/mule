/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
