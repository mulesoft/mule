/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
