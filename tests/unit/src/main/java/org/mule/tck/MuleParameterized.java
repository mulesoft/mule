/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runners.Parameterized;
import org.mule.util.FileUtils;
import org.mule.util.StringUtils;

/**
 * <code>MuleParameterized</code> adds test exclusions to the
 * <code>Parameterized</code> class. This is used for running JUnit4 tests with
 * parameters. The test exclusion logic in AbstractMuleTestCase does not work in
 * JUnit4 most likely due to the fact the JUnit4 does not extend the TestCase class
 * when using annotations, which are necessary for parameterized testing.
 */
public class MuleParameterized extends Parameterized
{
    /**
     * The list of tests to exclude
     */
    private static ArrayList<String> excludedTests = new ArrayList<String>();

    protected static transient Log logger = LogFactory.getLog(org.mule.tck.MuleParameterized.class);    
    
    public MuleParameterized(Class<?> klass) throws Throwable
    {
        super(klass);
        getExcluded();
        try
        {
            filter(excludeFilter);
        }
        // we need to ignore this error since we do filtering against the test method
        // names and not the test class, since the 'name' for the test class is
        // always an index when running parameterized tests, i.e. [0]
        catch (NoTestsRemainException e)
        {
            // ignore
        }
    }

    /**
     * Read the test exclusions file and find the tests to be excluded from running.
     */
    public void getExcluded()
    {      
        try
        {
            URL fileUrl = this.getClass().getClassLoader().getResource("mule-test-exclusions.txt");

            if (fileUrl != null)
            {
                // in case .txt is in jar
                URI fileUri = new URI(StringUtils.removeStart(fileUrl.toString(), "jar:"));

                // this iterates over all lines in the exclusion file
                @SuppressWarnings("unchecked")
                Iterator<String> lines = FileUtils.lineIterator(FileUtils.newFile(fileUri));

                ArrayList<String> s = new ArrayList<String>();
                String line;
                while (lines.hasNext())
                {
                    line = StringUtils.trimToEmpty(lines.next());
                    if (!(line.startsWith("#")) && !line.equals("") && line.length() > 0)
                    {
                        s.add(line);
                        logger.info("adding test to the list of exclusions : " + line);
                    }
                }
                excludedTests = s;
            }
            else
            {
                logger.info("did not find test exclusions file");
            }
        }
        catch (IOException ioex)
        {
            // ignore
        }
        catch (URISyntaxException e)
        {
            // ignore
        }
    }

    private static Filter excludeFilter = new Filter()
    {
        /**
         * Checks the test description against the list of excluded tests. TODO: take
         * this one step further and allow you to exclude specific tests in a test
         * class. Currently, parameterized tests have a name like this:
         * testMethod[index](TestClass)
         */
        @Override
        public boolean shouldRun(Description description)
        {
            for (String excludedTest : excludedTests)
            {
                // use contains instead of equals since parameterized tests list
                // their name as an index, i.e. [0] when filtering against the test
                // class name and methodName[index]ClassName when checking against
                // individual test methods. The test exclusions file contains the
                // full class name to exclude.
                if (description.getChildren().get(0).toString().contains(excludedTest))
                {
                    logger.info("skipping test : " + description.getChildren().get(0).toString());
                    return false;
                }
            }
            return true;
        }

        @Override
        public String describe()
        {
            return "excludes tests from mule-test-exclusions.txt";
        }
    };
}
