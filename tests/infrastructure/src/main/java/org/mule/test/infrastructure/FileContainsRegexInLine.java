/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.infrastructure;

import java.io.File;

import org.hamcrest.Factory;
import org.hamcrest.Matcher;


public class FileContainsRegexInLine extends AbstractRegexFileMatcher<File>
{


    private FileContainsRegexInLine(String regex)
    {
        this.regex = regex;
    }

    @Factory
    public static <T> Matcher<File> FileContainsRegexInLine(String regex)
    {
        return new FileContainsRegexInLine(regex);
    }

    @Override
    public boolean matchesSafely(File file)
    {
        this.file = file;
        return this.matches();
    }
}
