/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.support;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.service.model.MessagePartInfo;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@SmallTest
public class OutputPayloadInterceptorTestCase extends AbstractMuleTestCase
{

    private OutputPayloadInterceptor interceptor;

    @Before
    public void setUpInterceptor()
    {
        interceptor = new OutputPayloadInterceptor();
    }

    @Test
    public void testCleanUpPayload_NotAnArray() throws Exception
    {
        final List<?> someList = new ArrayList<Object>();
        assertSame(someList, interceptor.cleanUpPayload(someList));

        final String someString = "Some String";
        assertSame(someString, interceptor.cleanUpPayload(someString));
    }

    @Test
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

    @Test
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

    @Test
    public void testAddsMissingPartContentOnHandleMessage() {
        MessageContentsList messageContentsList = new MessageContentsList();
        Object content1 = new Object();
        messageContentsList.add(content1);

        List<MessagePartInfo> parts = new ArrayList<MessagePartInfo>();

        MessagePartInfo part1 = new MessagePartInfo(new QName("part1"), null);
        part1.setIndex(1);
        parts.add(part1);

        interceptor.ensurePartIndexMatchListIndex(messageContentsList, parts);

        assertEquals(2, messageContentsList.size());
        assertEquals(null, messageContentsList.get(0));
        assertEquals(content1, messageContentsList.get(1));
    }

    @Test
    public void testAddsMissingPartContentWithUnsortedPartListOnHandleMessage() {
        MessageContentsList messageContentsList = new MessageContentsList();
        Object content1 = new Object();
        messageContentsList.add(content1);
        Object content2 = new Object();
        messageContentsList.add(content2);

        List<MessagePartInfo> parts = new ArrayList<MessagePartInfo>();

        MessagePartInfo part2 = new MessagePartInfo(new QName("part2"), null);
        part2.setIndex(3);
        parts.add(part2);

        MessagePartInfo part1 = new MessagePartInfo(new QName("part1"), null);
        part1.setIndex(1);
        parts.add(part1);

        interceptor.ensurePartIndexMatchListIndex(messageContentsList, parts);

        assertEquals(4, messageContentsList.size());
        assertEquals(null, messageContentsList.get(0));
        assertEquals(content1, messageContentsList.get(1));
        assertEquals(null, messageContentsList.get(2));
        assertEquals(content2, messageContentsList.get(3));
    }
}
