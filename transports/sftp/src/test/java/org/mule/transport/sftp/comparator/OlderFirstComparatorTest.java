/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp.comparator;

import com.jcraft.jsch.SftpATTRS;
import org.junit.Before;
import org.junit.Test;
import org.mule.transport.sftp.FileDescriptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by christianlangmann on 06/12/14.
 */
public class OlderFirstComparatorTest {

    private final SftpATTRS OLD_ATTR = mock(SftpATTRS.class);
    private final SftpATTRS NEW_ATTR = mock(SftpATTRS.class);
    private final FileDescriptor OLD_FILE = new FileDescriptor("file1", OLD_ATTR);
    private final FileDescriptor NEW_FILE = new FileDescriptor("file2", NEW_ATTR);

    final private OlderFirstComparator comparator = new OlderFirstComparator();

    @Before
    public void Setup() {
        when(OLD_ATTR.getMTime()).thenReturn(1000);
        when(NEW_ATTR.getMTime()).thenReturn(2000);
    }

    @Test
    public void testOlder() {
        assertEquals((long) comparator.compare(OLD_FILE, NEW_FILE), -1L);
    }

    @Test
    public void testNewer() {
        assertEquals(comparator.compare(NEW_FILE, OLD_FILE), 1L);
    }

    @Test
    public void testSame() {
        assertEquals(comparator.compare(OLD_FILE, OLD_FILE), 0L);
    }

    @Test(expected = Exception.class)
    public void testParam1Null() {
        comparator.compare(null, OLD_FILE);
    }

    @Test(expected = Exception.class)
    public void testParam2Null() {
        comparator.compare(OLD_FILE, null);
    }

    @Test(expected = Exception.class)
    public void testInvalidParam1() {
        comparator.compare(OLD_ATTR, OLD_FILE);
    }

    @Test(expected = Exception.class)
    public void testInvalidParam2() {
        comparator.compare(OLD_FILE, OLD_ATTR);
    }

}
