/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.infrastructure;

import static java.nio.charset.Charset.defaultCharset;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.hamcrest.Description;
import org.junit.internal.matchers.TypeSafeMatcher;

public abstract class AbstractRegexFileMatcher<T> extends TypeSafeMatcher<T>
{
    private String failure;
    protected String regex;
    protected File file;

    protected boolean matches()
    {
        BufferedReader reader = null;
        String line;
        failure = "There is no matching line for " + regex;
        try
        {
            reader = Files.newBufferedReader(file.toPath(), defaultCharset());
            while ((line = reader.readLine()) != null)
            {
                if (line.matches(regex))
                {
                    return true;
                }
            }
        }
        catch (IOException e)
        {
            failure = e.getMessage();
            return false;
        }
        finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                }
                catch (IOException e)
                {
                    failure = e.getMessage();
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public void describeTo(Description description)
    {
        description.appendText(failure);
    }
}
