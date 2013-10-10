/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.file.filters;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SmallTest
public class FilenameRegexFilterTestCase extends AbstractMuleTestCase
{

    @Test
    public void testFilenameRegexFilter()
    {
        FilenameRegexFilter filter = new FilenameRegexFilter();
        assertNull(filter.getPattern());
        assertFalse(filter.accept("foo"));

        filter.setPattern("[0-9]*_test.csv");
        assertNotNull(filter.getPattern());
        filter.setCaseSensitive(true);
        assertNotNull(filter.getPattern());
        filter.setPattern(null);
        assertNull(filter.getPattern());

        filter.setPattern("[0-9]*_test.csv");
        filter.setCaseSensitive(true);
        String fileNameMatch = "20060101_test.csv";
        String fileNameNoMatch1 = "20060101_test_test.csv";
        String fileNameNoMatch2 = "20060101_TEST.csv";

        assertNotNull(filter.getPattern());
        assertTrue(filter.accept(fileNameMatch));
        assertFalse(filter.accept(fileNameNoMatch1));
        assertFalse(filter.accept(fileNameNoMatch2));

        filter.setCaseSensitive(false);
        assertTrue(filter.accept(fileNameMatch));
        assertFalse(filter.accept(fileNameNoMatch1));
        assertTrue(filter.accept(fileNameNoMatch2));
    }

}
