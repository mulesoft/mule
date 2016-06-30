/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mule.util.FileUtils.newFile;

import java.io.File;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.BeforeClass;

public class MuleContainerStartupSplashScreenTestCase extends AbstractSplashScreenTestCase<MuleContainerStartupSplashScreen>
{
    private static final String FIRST_PATCH = "SE-4242-3.8.0.jar";
    private static final String SECOND_PATCH = "SE-9999-3.7.3.jar";
    private static final String COMPLEX_LOG_PART = "* Applied patches:                                                   *\n" +
                                                   "*  - " + FIRST_PATCH + "                                               *\n" +
                                                   "*  - " + SECOND_PATCH + "                                               *\n" +
                                                   "* Mule system properties:                                            *\n";

    @BeforeClass
    public static void setUpPatches()
    {
        File libFolder = newFile(workingDirectory.getRoot(), "lib/user");
        libFolder.mkdirs();
        newFile(libFolder, FIRST_PATCH).mkdir();
        newFile(libFolder, "library.jar").mkdir();
        newFile(libFolder, SECOND_PATCH).mkdir();
    }

    @Before
    public void setUp()
    {
        splashScreen = new MuleContainerStartupSplashScreen();
    }

    @Override
    protected void setUpSplashScreen()
    {
        splashScreen.doBody();
    }

    @Override
    protected Matcher<String> getSimpleLogMatcher()
    {
        return not(containsString(COMPLEX_LOG_PART));
    }

    @Override
    protected Matcher<String> getComplexLogMatcher()
    {
        return containsString(COMPLEX_LOG_PART);
    }
}
