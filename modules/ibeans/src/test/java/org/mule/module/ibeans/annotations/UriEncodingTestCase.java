/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
