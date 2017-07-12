/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file.filters;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class FilenameRegexFilterTestCase extends AbstractMuleTestCase
{

    @Test
    public void testFilenameRegexFilter()
    {
        FilenameRegexFilter filter = new FilenameRegexFilter();
        assertThat(filter.getPattern(), nullValue());
        assertThat(filter.accept("foo"), equalTo(false));

        filter.setPattern("[0-9]*_test.csv");
        assertThat(filter.getPattern(), notNullValue());
        filter.setCaseSensitive(true);
        assertThat(filter.getPattern(), notNullValue());
        filter.setPattern(null);
        assertThat(filter.getPattern(), nullValue());

        filter.setPattern("[0-9]*_test.csv");
        filter.setCaseSensitive(true);
        String fileNameMatch = "20060101_test.csv";
        String fileNameNoMatch1 = "20060101_test_test.csv";
        String fileNameNoMatch2 = "20060101_TEST.csv";

        assertThat(filter.getPattern(), notNullValue());
        assertThat(filter.accept(fileNameMatch), equalTo(true));
        assertThat(filter.accept(fileNameNoMatch1), equalTo(false));
        assertThat(filter.accept(fileNameNoMatch2), equalTo(false));

        filter.setCaseSensitive(false);
        assertThat(filter.accept(fileNameMatch), equalTo(true));
        assertThat(filter.accept(fileNameNoMatch1), equalTo(false));
        assertThat(filter.accept(fileNameNoMatch2), equalTo(true));

        filter.setPattern("test\\d{2,4}.csv");
        String fileNameNoMatchRange = "test.csv";
        String fileNameNoMatchRange1 = "test1.csv";
        String fileNameMatchRange2 = "test11.csv";
        String fileNameMatchRange3 = "test111.csv";
        String fileNameMatchRange4 = "test1111.csv";
        String fileNameNoMatchRange5 = "test11111.csv";
        assertThat(filter.accept(fileNameNoMatchRange), equalTo(false));
        assertThat(filter.accept(fileNameNoMatchRange1), equalTo(false));
        assertThat(filter.accept(fileNameMatchRange2), equalTo(true));
        assertThat(filter.accept(fileNameMatchRange3), equalTo(true));
        assertThat(filter.accept(fileNameMatchRange4), equalTo(true));
        assertThat(filter.accept(fileNameNoMatchRange5), equalTo(false));
    }

}
