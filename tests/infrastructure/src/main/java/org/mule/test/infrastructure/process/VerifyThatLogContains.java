/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process;

import org.mule.test.infrastructure.AbstractFileMatcher;

import org.hamcrest.Factory;
import org.hamcrest.Matcher;

public class VerifyThatLogContains extends AbstractFileMatcher<MuleProcessController>
{
    String app;

    @Factory
    public static <T> Matcher<MuleProcessController> VerifyThatLogContains(String regex)
    {
        return new VerifyThatLogContains(null, regex);
    }

    @Factory
    public static <T> Matcher<MuleProcessController> VerifyThatLogContains(String appName, String regex)
    {
        return new VerifyThatLogContains(appName,regex);
    }

    private VerifyThatLogContains(String appName, String regex)
    {
        this.app = appName;
        this.regex = regex;
    }

    @Override
    public boolean matchesSafely(MuleProcessController muleProcessController)
    {
        this.file = app == null ? muleProcessController.getLog() : muleProcessController.getLog(app);
        return this.matches();
    }



}
