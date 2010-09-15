/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf.support;

import org.mule.tck.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.cxf.message.MessageContentsList;

public class OutputPayloadInterceptorTestCase extends AbstractMuleTestCase
{

    private OutputPayloadInterceptor interceptor;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        interceptor = new OutputPayloadInterceptor();
    }

    public void testCleanUpPayload_NotAnArray() throws Exception
    {
        final List<?> someList = new ArrayList<Object>();
        assertSame(someList, interceptor.cleanUpPayload(someList));

        final String someString = "Some String";
        assertSame(someString, interceptor.cleanUpPayload(someString));
    }

    public void testCleanUpPayload_NonObjectArray() throws Exception
    {
        final String someString = "Some String";
        assertSame(someString, interceptor.cleanUpPayload(new String[]{someString, null}));

        final String[] arrayOf2Strings = {someString, "someOther String"};
        assertSame(arrayOf2Strings, interceptor.cleanUpPayload(arrayOf2Strings));

        final String[] arrayOf2Strings1Null = {someString, "someOther String", null};
        assertTrue(Arrays.equals(new String[]{arrayOf2Strings1Null[0], arrayOf2Strings1Null[1]},
            (Object[]) interceptor.cleanUpPayload(arrayOf2Strings1Null)));
    }

    public void testCleanUpPayload_ArrayOfObjects()
    {
        final Integer someInteger = Integer.valueOf(1);
        assertSame(someInteger, interceptor.cleanUpPayload(new Object[]{someInteger, null}));

        final Object[] arrayOf2Objects = {someInteger, new Object()};
        assertSame(arrayOf2Objects, interceptor.cleanUpPayload(arrayOf2Objects));

        final Object[] arrayOf2Objects1Null = {someInteger, null, new Object(),
            MessageContentsList.REMOVED_MARKER};
        assertTrue(Arrays.equals(new Object[]{arrayOf2Objects1Null[0], arrayOf2Objects1Null[2]},
            (Object[]) interceptor.cleanUpPayload(arrayOf2Objects1Null)));
    }
}
