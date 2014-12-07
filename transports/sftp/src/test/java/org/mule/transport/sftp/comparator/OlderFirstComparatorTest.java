/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp.comparator;

import com.jcraft.jsch.SftpATTRS;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.*;

import static org.junit.Assert.*;

import java.util.AbstractMap;
import java.util.Map;

/**
 * Created by christianlangmann on 06/12/14.
 */
public class OlderFirstComparatorTest {

    private static final SftpATTRS OLD_ATTR = mock(SftpATTRS.class);
    private static final SftpATTRS NEW_ATTR = mock(SftpATTRS.class);
    private static final Map.Entry<String, SftpATTRS> OLD_FILE = new AbstractMap.SimpleEntry("file1", OLD_ATTR);
    private static final Map.Entry<String, SftpATTRS> NEW_FILE = new AbstractMap.SimpleEntry("file2", NEW_ATTR);

    final private static OlderFirstComparator comparator = new OlderFirstComparator();

    @BeforeClass
    public static void Setup() {
        when(OLD_ATTR.getMTime()).thenReturn(1000);
        when(NEW_ATTR.getMTime()).thenReturn(2000);
    }

    @Test
    public void testOlder() {
        assertEquals((long)comparator.compare(OLD_FILE, NEW_FILE), -1L);
    }

    @Test
    public void testNewer() {
        assertEquals(comparator.compare(NEW_FILE, OLD_FILE), 1L);
    }

    @Test
    public void testSame() {
        assertEquals(comparator.compare(OLD_FILE, OLD_FILE), 0L);
    }

    @Test(expected=Exception.class)
    public void testParam1Null() {
        comparator.compare(null, OLD_FILE);
    }

    @Test(expected=Exception.class)
    public void testParam2Null() {
        comparator.compare(OLD_FILE, null);
    }

    @Test(expected=Exception.class)
    public void testInvalidParam1() {
        comparator.compare(OLD_ATTR, OLD_FILE);
    }

    @Test(expected=Exception.class)
    public void testInvalidParam2() {
        comparator.compare(OLD_FILE, OLD_ATTR);
    }

}
