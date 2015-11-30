/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4;

import java.util.LinkedList;
import java.util.List;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

/**
 * Provides a tool to detect the cause of flaky tests by running then multiple times.
 * It looks for classes or methods annotated with {@link FlakyTest} and generate the
 * required test methods for them. Annotation on test classes take precedence on annotated
 * test methods.
 *
 * <p>
 *     NOTE: This is a tool intended to be used by developers but not to commit any test
 *     using this test runner. That would be only valid when a flaky test fails on the
 *     continuous integration server but not locally.
 * </p>
 * <p>
 *     To use this tool annotate the test class with
 *     <pre>
 *         &#64;RunWith(FlakinessDetectorTestRunner)
 *     </pre>
 *
 *     And then annotate the flaky test class or flaky test method with
 *     <pre>
 *         &#64;FlakyTest(times= n) // where n is the number of times you want the test executed
 *     </pre>
 * </p>
 */
public class FlakinessDetectorTestRunner extends BlockJUnit4ClassRunner
{

    public FlakinessDetectorTestRunner(Class<?> type) throws InitializationError
    {
        super(type);
    }

    @Override
    protected List<FrameworkMethod> computeTestMethods()
    {
        List<FrameworkMethod> methods = super.computeTestMethods();

        List<FrameworkMethod> result;


        Class<?> testClass = getTestClass().getJavaClass();

        if (isFlakyClass(testClass))
        {
            result = computeTestMehods(methods, testClass);

        }
        else
        {
            result = computeTestMethods(methods);
        }

        return result;
    }

    private List<FrameworkMethod> computeTestMethods(List<FrameworkMethod> methods)
    {
        List<FrameworkMethod> result = new LinkedList<FrameworkMethod>();

        for (FrameworkMethod method : methods)
        {
            if (isFlakyTest(method))
            {
                for (int i = 0; i < getTimes(method); i++)
                {
                    result.add(method);
                }
            }
            else
            {
                result.add(method);
            }
        }

        return result;
    }

    private List<FrameworkMethod> computeTestMehods(List<FrameworkMethod> methods, Class<?> testClass)
    {
        List<FrameworkMethod> result = new LinkedList<FrameworkMethod>();

        for (int i = 0; i < getTimes(testClass); i++)
        {
            result.addAll(methods);
        }

        return result;
    }

    private static boolean isFlakyTest(FrameworkMethod method)
    {
        return method.getAnnotation(FlakyTest.class) != null;
    }

    private static boolean isFlakyClass(Class<?> type)
    {
        return type.getAnnotation(FlakyTest.class) != null;
    }

    private static int getTimes(FrameworkMethod method)
    {
        return method.getAnnotation(FlakyTest.class).times();
    }

    private static int getTimes(Class<?> type)
    {
        return type.getAnnotation(FlakyTest.class).times();
    }

}