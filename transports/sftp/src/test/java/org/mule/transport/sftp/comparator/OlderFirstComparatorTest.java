<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> abf2faf... missing license header
/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
<<<<<<< HEAD
package org.mule.transport.sftp.comparator;

import com.jcraft.jsch.SftpATTRS;
import org.junit.Before;
import org.junit.Test;
import org.mule.transport.sftp.FileDescriptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
=======
=======
>>>>>>> 12a456f... SFTP supports comparator and reverse
=======
>>>>>>> abf2faf... missing license header
package org.mule.transport.sftp.comparator;

import com.jcraft.jsch.SftpATTRS;
import org.junit.Before;
import org.junit.Test;
import org.mule.transport.sftp.FileDescriptor;

<<<<<<< HEAD
import java.util.AbstractMap;
import java.util.Map;
<<<<<<< HEAD
>>>>>>> 797ec72... SFTP supports comparator and reverse
=======
>>>>>>> 12a456f... SFTP supports comparator and reverse

=======
>>>>>>> 2ba8afd... Adjust tests so that order and reverse-order is tested
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by christianlangmann on 06/12/14.
 */
public class OlderFirstComparatorTest {

<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 2ba8afd... Adjust tests so that order and reverse-order is tested
    private final SftpATTRS OLD_ATTR = mock(SftpATTRS.class);
    private final SftpATTRS NEW_ATTR = mock(SftpATTRS.class);
    private final FileDescriptor OLD_FILE = new FileDescriptor("file1", OLD_ATTR);
    private final FileDescriptor NEW_FILE = new FileDescriptor("file2", NEW_ATTR);
<<<<<<< HEAD

    final private OlderFirstComparator comparator = new OlderFirstComparator();

    @Before
    public void Setup() {
=======
=======
>>>>>>> 12a456f... SFTP supports comparator and reverse
    private static final SftpATTRS OLD_ATTR = mock(SftpATTRS.class);
    private static final SftpATTRS NEW_ATTR = mock(SftpATTRS.class);
    private static final FileDescriptor OLD_FILE = new FileDescriptor("file1", OLD_ATTR);
    private static final FileDescriptor NEW_FILE = new FileDescriptor("file2", NEW_ATTR);
=======
>>>>>>> 2ba8afd... Adjust tests so that order and reverse-order is tested

    final private OlderFirstComparator comparator = new OlderFirstComparator();

<<<<<<< HEAD
    @BeforeClass
    public static void Setup() {
<<<<<<< HEAD
>>>>>>> 797ec72... SFTP supports comparator and reverse
=======
>>>>>>> 12a456f... SFTP supports comparator and reverse
=======
    @Before
    public void Setup() {
>>>>>>> 0a6b968... apply codestyle
        when(OLD_ATTR.getMTime()).thenReturn(1000);
        when(NEW_ATTR.getMTime()).thenReturn(2000);
    }

    @Test
    public void testOlder() {
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
        assertEquals((long) comparator.compare(OLD_FILE, NEW_FILE), -1L);
=======
        assertEquals((long)comparator.compare(OLD_FILE, NEW_FILE), -1L);
>>>>>>> 797ec72... SFTP supports comparator and reverse
=======
        assertEquals((long)comparator.compare(OLD_FILE, NEW_FILE), -1L);
>>>>>>> 12a456f... SFTP supports comparator and reverse
=======
        assertEquals((long) comparator.compare(OLD_FILE, NEW_FILE), -1L);
>>>>>>> 0a6b968... apply codestyle
    }

    @Test
    public void testNewer() {
        assertEquals(comparator.compare(NEW_FILE, OLD_FILE), 1L);
    }

    @Test
    public void testSame() {
        assertEquals(comparator.compare(OLD_FILE, OLD_FILE), 0L);
    }

<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
    @Test(expected = Exception.class)
=======
    @Test(expected=Exception.class)
>>>>>>> 797ec72... SFTP supports comparator and reverse
=======
    @Test(expected=Exception.class)
>>>>>>> 12a456f... SFTP supports comparator and reverse
=======
    @Test(expected = Exception.class)
>>>>>>> 0a6b968... apply codestyle
    public void testParam1Null() {
        comparator.compare(null, OLD_FILE);
    }

<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
    @Test(expected = Exception.class)
=======
    @Test(expected=Exception.class)
>>>>>>> 797ec72... SFTP supports comparator and reverse
=======
    @Test(expected=Exception.class)
>>>>>>> 12a456f... SFTP supports comparator and reverse
=======
    @Test(expected = Exception.class)
>>>>>>> 0a6b968... apply codestyle
    public void testParam2Null() {
        comparator.compare(OLD_FILE, null);
    }

<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
    @Test(expected = Exception.class)
=======
    @Test(expected=Exception.class)
>>>>>>> 797ec72... SFTP supports comparator and reverse
=======
    @Test(expected=Exception.class)
>>>>>>> 12a456f... SFTP supports comparator and reverse
=======
    @Test(expected = Exception.class)
>>>>>>> 0a6b968... apply codestyle
    public void testInvalidParam1() {
        comparator.compare(OLD_ATTR, OLD_FILE);
    }

<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
    @Test(expected = Exception.class)
=======
    @Test(expected=Exception.class)
>>>>>>> 797ec72... SFTP supports comparator and reverse
=======
    @Test(expected=Exception.class)
>>>>>>> 12a456f... SFTP supports comparator and reverse
=======
    @Test(expected = Exception.class)
>>>>>>> 0a6b968... apply codestyle
    public void testInvalidParam2() {
        comparator.compare(OLD_FILE, OLD_ATTR);
    }

}
