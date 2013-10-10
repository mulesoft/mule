/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.ibeans.annotations;

import org.ibeans.annotation.IntegrationBean;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UriEncodingTestCase extends AbstractIBeansTestCase
{
    @IntegrationBean
    private TestUriIBean test;

    @Test
    public void testEncoding1() throws Exception
    {
        String param = "This is a value with spaces";
        String result = test.doSomething(param);

        assertEquals("http://" + TestUriIBean.DO_SOMETHING_URI + "This+is+a+value+with+spaces", result);
    }

    @Test
    public void testEncoding2() throws Exception
    {
        String param = "This%20is%20a%20value%20with%20spaces";
        String result = test.doSomething(param);

        assertEquals("http://" + TestUriIBean.DO_SOMETHING_URI + "This%20is%20a%20value%20with%20spaces", result);
    }
}
