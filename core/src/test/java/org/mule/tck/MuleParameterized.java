/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck;

import org.mule.util.ClassUtils;
import org.mule.util.FileUtils;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runners.Parameterized;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

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

    /**
     * Override collection from org.junit.runners.Parameterized
     */
    private final ArrayList<Runner> muleRunners = new ArrayList<Runner>();

    public MuleParameterized(Class<?> klass) throws Throwable
    {
        super(klass);

        // Create our own list of test runners, excluding any configurations which are not enabled.
        List<Runner> runners = super.getChildren();        
        List<Object[]> parametersList = getParametersList(getTestClass());
        Object parameter;
        for (int i = 0; i < parametersList.size(); i++)
        {
            parameter = parametersList.get(i)[0];
            if (!(parameter instanceof ParameterizedConfiguration))
            {
                throw new IllegalArgumentException("Parameters for Mule test classes should implement the ParameterizedConfiguration interface");
            }
            if (((ParameterizedConfiguration) parameter).isEnabled())
            {
                muleRunners.add(runners.get(i));
            }
        }
        
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

    @Override
    protected List<Runner> getChildren() 
    {
        return muleRunners;
    }

    /**
     * Read the test exclusions file and find the tests to be excluded from running.
     */
    public void getExcluded()
    {
        try
        {
            // We find the physical classpath root URL of the test class and
            // use that to find the correct resource. Works fine everywhere,
            // regardless of classloaders. See MULE-2414
            URL[] urls = new URL[]{ClassUtils.getClassPathRoot(getTestClass().getJavaClass())};
            URL fileUrl = new URLClassLoader(urls).getResource("mule-test-exclusions.txt");

            if (fileUrl != null)
            {
                // in case .txt is in jar
                URI fileUri = new URI(StringUtils.removeStart(fileUrl.toString(), "jar:"));

                // this iterates over all lines in the exclusion file
                Iterator<String> lines = FileUtils.lineIterator(FileUtils.newFile(fileUri));

                ArrayList<String> s = new ArrayList<String>();
                String line = null;
                while (lines.hasNext())
                {
                    line = StringUtils.trimToEmpty(lines.next());
                    if (!(line.startsWith("#")) && line != "" && line.length() > 0)
                    {
                        s.add(line);
                    }
                }
                excludedTests = s;
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
         * Checks the test description against the list of excluded tests
         */
        @Override
        public boolean shouldRun(Description description)
        {

            for (int i = 0; i < excludedTests.size(); i++)
            {
                // use contains instead of equals since parameterized tests list
                // their name as an index, i.e. [0] when filtering against the test
                // class name and methodName[index]ClassName when checking against
                // individual test methods. The test exclusions file contains the
                // full class name to exclude.
                if (description.toString().contains(excludedTests.get(i)))
                {
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

    //////////////////////////////////////////////////////////////////////////////
    // The following code is a copy/paste from org.junit.runners.Parameterized
    // which is unfortunately necessary in order to be able to override some of
    // its functionality. Hopefully JUnit will make these methods protected rather 
    // than private in a future version so that this is not necessary.
    //////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("unchecked")
    private List<Object[]> getParametersList(TestClass klass) throws Throwable
    {
        return (List<Object[]>) getParametersMethod(klass).invokeExplosively(null);
    }

    private FrameworkMethod getParametersMethod(TestClass testClass) throws Exception
    {
        List<FrameworkMethod> methods = testClass.getAnnotatedMethods(Parameters.class);
        for (FrameworkMethod each : methods)
        {
            int modifiers = each.getMethod().getModifiers();
            if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)) return each;
        }

        throw new Exception("No public static parameters method on class " + testClass.getName());
    }

    //////////////////////////////////////////////////////////////////////////////
}
