/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.ibeans.annotations;

import java.net.UnknownHostException;

import org.ibeans.annotation.IntegrationBean;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OptionalParamsTestCase extends AbstractIBeansTestCase
{
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
