/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.annotations;

import java.net.UnknownHostException;

import org.ibeans.annotation.IntegrationBean;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class OptionalParamsTestCase extends AbstractIBeansTestCase
{
    @SuppressWarnings("unused")
    @IntegrationBean
    private TestUriIBean test;

    @Test
    public void testOptionalParams() throws Exception
    {
        String result = test.doSomethingOptional("x", "y");
        assertEquals("http://doesnotexist.bom?param1=x&param2=y", result);

        result = test.doSomethingOptional("x", null);
        assertEquals("http://doesnotexist.bom?param1=x", result);

        result = test.doSomethingOptional(null, "y");
        assertEquals("http://doesnotexist.bom?param2=y", result);
    }

    @Test(expected = IllegalArgumentException.class)
    @Ignore("TODO: test is wrong because uses the expected attribute but that exception is never thrown. Running" +
            " the test you get java.lang.reflect.UndeclaredThrowableException instead of the IllegalArgumentException")
    public void paramNull() throws UnknownHostException
    {
        test.doSomethingElse("x", null);
    }

}
