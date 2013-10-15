/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.annotations;

import org.ibeans.impl.view.TextView;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Basically, just test we don't get an error. Since the result is unstructured text it is hard to make many assertions
 * on it
 */
public class TextUsageViewTestCase extends AbstractIBeansTestCase
{
    @Test
    public void testUsageView() throws Exception
    {
        TextView view = new TextView();
        String string = view.createView(TestUriIBean.class);

        assertNotNull(string);
        System.out.println(string);

        assertTrue(string.contains("doSomething("));
        assertTrue(string.contains("doSomethingElse("));
        assertTrue(string.contains("doSomethingNoParams("));
    }
}
