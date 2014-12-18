/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class StringRegexMatcher extends TypeSafeMatcher<String>
{
    private String regex = null;

    public StringRegexMatcher(String regexGiven)
    {
        regex = regexGiven;
    }

    @Override
    public boolean matchesSafely(String stringToCheck)
    {
        return stringToCheck.matches(regex);
    }

    @Override
    public void describeTo(Description description)
    {
        description.appendText("matches with the regex ").appendValue(regex);
    }

    @Override
    protected void describeMismatchSafely(String string, Description mismatchDescription)
    {
        mismatchDescription.appendText("was ").appendValue(string);
    }

    @Factory
    public static Matcher<String> matches(String regex)
    {
        return new StringRegexMatcher(regex);
    }
}

