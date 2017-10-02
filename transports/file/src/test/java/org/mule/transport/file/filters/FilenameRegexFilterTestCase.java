/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
    public void testTrimRegexFilter()
    {
        FilenameRegexFilter filter = new FilenameRegexFilter();
        filter.setPattern("Complete-(.*).csv,       Complete-(.*).txt,Complete-(.*).ext, Complete-(.*).mp3");
        String fileName1 = "Complete-1.txt";
        String fileName2 = "Complete-11212321.ext";
        String fileName3 = "Complete-11212321.ex";
        String fileName4 = "Complete-11212321.mp3";
        assertThat(filter.accept(fileName1), equalTo(true));
        assertThat(filter.accept(fileName2), equalTo(true));
        assertThat(filter.accept(fileName3), equalTo(false));
        assertThat(filter.accept(fileName4), equalTo(true));
    }
    
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
