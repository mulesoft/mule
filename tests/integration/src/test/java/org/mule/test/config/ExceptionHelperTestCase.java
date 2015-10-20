/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.api.DefaultMuleException;
import org.mule.api.config.ConfigurationException;
import org.mule.api.registry.ResolverException;
import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.SystemUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.Closure;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.comparators.ComparableComparator;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.IsNull;
import org.junit.Test;

@SmallTest
public class ExceptionHelperTestCase extends AbstractMuleTestCase
{
    @Test
    public void nestedExceptionRetreval() throws Exception
    {
        Exception testException = getException();
        Throwable t = ExceptionHelper.getRootException(testException);
        assertNotNull(t);
        assertThat(t.getMessage(), is("blah"));
        assertThat(t.getCause(), nullValue());

        t = ExceptionHelper.getRootMuleException(testException);
        assertThat(t.getMessage(), is("bar"));
        assertThat(t.getCause(), not(nullValue()));

        List<Throwable> l = ExceptionHelper.getExceptionsAsList(testException);
        assertThat(l.size(), is(3));

        Map<String, String> info = ExceptionHelper.getExceptionInfo(testException);
        assertThat(info.size(), is(0));
    }

    @Test
    public void summarizeWithDepthBeyondStackTraceLength()
    {
        Exception exception = getException();
        int numberOfStackFrames = exception.getStackTrace().length;
        int depth = numberOfStackFrames + 1;

        Throwable summary = ExceptionHelper.summarise(exception, depth);
        assertThat(summary, not(nullValue()));
    }

    @Test
    public void getNonMuleExceptionCause()
    {
        assertThat(ExceptionHelper.getNonMuleException(new ResolverException(CoreMessages.failedToBuildMessage(), null)), IsNull.<Object>nullValue());
        assertThat(ExceptionHelper.getNonMuleException(new ResolverException(CoreMessages.failedToBuildMessage(),
                new ConfigurationException(CoreMessages.failedToBuildMessage(), null))), IsNull.<Object>nullValue());
        assertThat(ExceptionHelper.getNonMuleException(new ResolverException(CoreMessages.failedToBuildMessage(),
                new ConfigurationException(CoreMessages.failedToBuildMessage(),
                        new IllegalArgumentException()))), IsInstanceOf.instanceOf(IllegalArgumentException.class));
        assertThat(ExceptionHelper.getNonMuleException(new ResolverException(CoreMessages.failedToBuildMessage(),
                new ConfigurationException(CoreMessages.failedToBuildMessage(),
                        new IllegalArgumentException(new NullPointerException())))), IsInstanceOf.instanceOf(IllegalArgumentException.class));
        assertThat(ExceptionHelper.getNonMuleException(new IllegalArgumentException()),IsInstanceOf.instanceOf(IllegalArgumentException.class));
    }

    @Test
    public void filteredStackIncludingNonMuleCode()
    {
        int calls = 5;
        try
        {
            generateStackEntries(calls, new Closure()
            {
                @Override
                public void execute(Object input)
                {
                    CollectionUtils.forAllDo(Collections.singleton(null), new Closure()
                    {
                        @Override
                        public void execute(Object input)
                        {
                            throw new RuntimeException(new DefaultMuleException(MessageFactory.createStaticMessage("foo")));
                        }
                    });
                }
            });
            fail("Expected exception");
        }
        catch (Exception e)
        {
            assertThat(ExceptionHelper.getExceptionStack(e), StringByLineMatcher.matchesLineByLine(
                    "foo (org.mule.api.DefaultMuleException)",
                    "  org.mule.test.config.ExceptionHelperTestCase$1$1.execute:",
                    "  org.apache.commons.collections.CollectionUtils.forAllDo:",
                    "  org.mule.test.config.ExceptionHelperTestCase$1.execute:",
                    "  org.mule.test.config.ExceptionHelperTestCase.generateStackEntries:",
                    "  (" + (calls + 13) + " more...)")); // recursive
        }
    }

    @Test
    public void filteredStackIncludingMixedNonMuleCode()
    {
    	int calls = 5;
    	try
    	{
    		generateStackEntries(calls, new Closure()
    		{
    			@Override
    			public void execute(Object input)
    			{
    				Comparable exceptionComparable = new Comparable()
    				{
    					@Override
    					public int compareTo(Object o) {
    						throw new RuntimeException(new DefaultMuleException(MessageFactory.createStaticMessage("foo")));
    					}
    				};
					Collections.sort(Arrays.asList(exceptionComparable, exceptionComparable), ComparableComparator.getInstance());
    			}
    		});
    		fail("Expected exception");
    	}
    	catch (Exception e)
    	{
    		assertThat(ExceptionHelper.getExceptionStack(e), StringByLineMatcher.matchesLineByLine(
    				"foo (org.mule.api.DefaultMuleException)",
    				"  org.mule.test.config.ExceptionHelperTestCase$2$1.compareTo:",
    				"  org.apache.commons.collections.comparators.ComparableComparator.compare:",
    				"  java.util", // Collections.sort
    				"  java.util", // Collections.sort
    				"  java.util", // Collections.sort
    				"  java.util", // Collections.sort
    				"  java.util", // Collections.sort
    				"  org.mule.test.config.ExceptionHelperTestCase$2.execute:",
    				"  org.mule.test.config.ExceptionHelperTestCase.generateStackEntries:",
    				"  (" + (calls + 13) + " more...)")); // recursive
    	}
    }
    
    @Test
    public void filteredStackAllMuleCode()
    {
    	int calls = 5;
        try
        {
			generateStackEntries(calls, new Closure() 
			{
				@Override
				public void execute(Object input)
				{
					throw new RuntimeException(new DefaultMuleException(MessageFactory.createStaticMessage("foo")));

				}
			});
        }
        catch (Exception e)
        {
            assertThat(ExceptionHelper.getExceptionStack(e), StringByLineMatcher.matchesLineByLine(
                    "foo (org.mule.api.DefaultMuleException)",
                    "  org.mule.test.config.ExceptionHelperTestCase$3.execute:",
                    "  org.mule.test.config.ExceptionHelperTestCase.generateStackEntries:",
                    "  org.mule.test.config.ExceptionHelperTestCase.generateStackEntries:",
                    "  (" + (calls + 12) + " more...)")); // recursive
        }
    }

    private void generateStackEntries(int calls, Closure closure)
    {
        if (calls == 0)
        {
            closure.execute(null);
        }
        else
        {
            generateStackEntries(--calls, closure);
        }
    }

    private static final class StringByLineMatcher extends TypeSafeMatcher<String>
    {
        private final String[] expectedEntries;
        private int i = 0;

        private StringByLineMatcher(String... expectedEntries)
        {
            this.expectedEntries = expectedEntries;
        }

        @Override
        public void describeTo(Description description)
        {
            description.appendText(String.format("line %d matches \"%s\"", i, expectedEntries[i]));
        }
        
        @Override
        protected boolean matchesSafely(String item)
        {
            String[] stackEntries = item.split(SystemUtils.LINE_SEPARATOR);

            if (stackEntries.length != expectedEntries.length)
            {
                return false;
            }

            for (String expectedEntry : expectedEntries)
            {
                if (!stackEntries[i].contains(expectedEntry))
                {
                    return false;
                }
                ++i;
            }

            return true;
        }

        public static StringByLineMatcher matchesLineByLine(String... expectedEntries)
        {
            return new StringByLineMatcher(expectedEntries);
        }
    }

    private Exception getException()
    {
        return new DefaultMuleException(MessageFactory.createStaticMessage("foo"), new DefaultMuleException(
            MessageFactory.createStaticMessage("bar"), new Exception("blah")));
    }
}
