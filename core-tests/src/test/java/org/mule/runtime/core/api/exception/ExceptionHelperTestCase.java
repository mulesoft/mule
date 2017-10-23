/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.exception;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.sort;
import static org.apache.commons.collections.CollectionUtils.forAllDo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.runtime.api.exception.ExceptionHelper.getExceptionInfo;
import static org.mule.runtime.api.exception.ExceptionHelper.getExceptionStack;
import static org.mule.runtime.api.exception.ExceptionHelper.getExceptionsAsList;
import static org.mule.runtime.api.exception.ExceptionHelper.getNonMuleException;
import static org.mule.runtime.api.exception.ExceptionHelper.getRootException;
import static org.mule.runtime.api.exception.ExceptionHelper.getRootMuleException;
import static org.mule.runtime.api.exception.ExceptionHelper.summarise;
import static org.mule.runtime.api.exception.MuleException.refreshVerboseExceptions;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToBuildMessage;

import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.internal.transformer.ResolverException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.size.SmallTest;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections.Closure;
import org.apache.commons.collections.comparators.ComparableComparator;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

@SmallTest
public class ExceptionHelperTestCase extends AbstractMuleTestCase {

  /**
   * When running this tests alone, this ensures the environment is consistent with how the tests are run altogether.
   */
  @BeforeClass
  public static void beforeClass() {
    refreshVerboseExceptions();
  }

  @Rule
  public SystemProperty verbose = new SystemProperty("mule.verbose.exceptions", "true") {

    @Override
    public void before() throws Throwable {
      super.before();
      refreshVerboseExceptions();
    }

    @Override
    public void after() {
      super.after();
      refreshVerboseExceptions();
    }
  };

  @Test
  public void nestedExceptionRetrieval() throws Exception {
    Exception testException = getException();
    Throwable t = getRootException(testException);
    assertNotNull(t);
    assertThat(t.getMessage(), is("blah"));
    assertThat(t.getCause(), nullValue());

    t = getRootMuleException(testException);
    assertThat(t.getMessage(), is("bar"));
    assertThat(t.getCause(), not(nullValue()));

    List<Throwable> l = getExceptionsAsList(testException);
    assertThat(l, hasSize(3));

    Map<String, Object> info = getExceptionInfo(testException);
    assertThat(info.entrySet(), hasSize(2));
    assertThat(info, hasEntry("info_1", "Imma in!"));
    assertThat(info, hasEntry("info_2", "Imma out!"));
  }

  @Test
  public void summarizeWithDepthBeyondStackTraceLength() {
    Exception exception = getException();
    int numberOfStackFrames = exception.getStackTrace().length;
    int depth = numberOfStackFrames + 1;

    Throwable summary = summarise(exception, depth);
    assertThat(summary, not(nullValue()));
  }

  @Test
  public void getNonMuleExceptionCause() {
    assertThat(getNonMuleException(new ResolverException(failedToBuildMessage(), null)), nullValue());
    assertThat(getNonMuleException(new ResolverException(failedToBuildMessage(),
                                                         new ConfigurationException(failedToBuildMessage(), null))),
               nullValue());
    assertThat(getNonMuleException(new ResolverException(failedToBuildMessage(),
                                                         new ConfigurationException(failedToBuildMessage(),
                                                                                    new IllegalArgumentException()))),
               instanceOf(IllegalArgumentException.class));
    assertThat(getNonMuleException(new ResolverException(failedToBuildMessage(),
                                                         new ConfigurationException(failedToBuildMessage(),
                                                                                    new IllegalArgumentException(new NullPointerException())))),
               instanceOf(IllegalArgumentException.class));
    assertThat(getNonMuleException(new IllegalArgumentException()), instanceOf(IllegalArgumentException.class));
  }

  @Test
  public void filteredStackIncludingNonMuleCode() {
    int calls = 5;
    try {
      generateStackEntries(calls, input -> forAllDo(singleton(null), input1 -> {
        throw new RuntimeException(new DefaultMuleException(createStaticMessage("foo")));
      }));
      fail("Expected exception");
    } catch (Exception e) {
      assertThat(getExceptionStack(e),
                 StringByLineMatcher.matchesLineByLine("foo \\(org.mule.runtime.api.exception.DefaultMuleException\\)",
                                                       "  " + ExceptionHelperTestCase.class.getName()
                                                           + ".lambda\\$[^\\(]*\\(ExceptionHelperTestCase.java:[0-9]+\\)",
                                                       "  org.apache.commons.collections.CollectionUtils.forAllDo\\(CollectionUtils.java:[0-9]+\\)",
                                                       "  " + ExceptionHelperTestCase.class.getName()
                                                           + ".lambda\\$[^\\(]*\\(ExceptionHelperTestCase.java:[0-9]+\\)",
                                                       "  " + ExceptionHelperTestCase.class.getName()
                                                           + ".generateStackEntries\\(ExceptionHelperTestCase.java:[0-9]+\\)",
                                                       "  \\(" + (calls + 16) + " more...\\)")); // recursive
    }
  }

  @Test
  public void filteredStackIncludingMixedNonMuleCode() {
    int calls = 5;
    try {
      generateStackEntries(calls, input -> {
        Comparable exceptionComparable = o -> {
          throw new RuntimeException(new DefaultMuleException(createStaticMessage("foo")));
        };
        sort(asList(exceptionComparable, exceptionComparable), ComparableComparator.getInstance());
      });
      fail("Expected exception");
    } catch (Exception e) {
      assertThat(getExceptionStack(e),
                 StringByLineMatcher.matchesLineByLine("foo \\(org.mule.runtime.api.exception.DefaultMuleException\\)",
                                                       "  " + ExceptionHelperTestCase.class.getName()
                                                           + ".lambda\\$[^\\(]*\\(ExceptionHelperTestCase.java:[0-9]+\\)",
                                                       "  org.apache.commons.collections.comparators.ComparableComparator.compare\\(ComparableComparator.java:[0-9]+\\)",
                                                       "  java.util.*", // Collections.sort
                                                       "  java.util.*", // Collections.sort
                                                       "  java.util.*", // Collections.sort
                                                       "  java.util.*", // Collections.sort
                                                       "  java.util.*", // Collections.sort
                                                       "  " + ExceptionHelperTestCase.class.getName()
                                                           + ".lambda\\$[^\\(]*\\(ExceptionHelperTestCase.java:[0-9]+\\)",
                                                       "  " + ExceptionHelperTestCase.class.getName()
                                                           + ".generateStackEntries\\(ExceptionHelperTestCase.java:[0-9]+\\)",
                                                       "  \\(" + (calls + 16) + " more...\\)")); // recursive
    }
  }

  @Test
  public void filteredStackAllMuleCode() {
    int calls = 5;
    try {
      generateStackEntries(calls, input -> {
        throw new RuntimeException(new DefaultMuleException(createStaticMessage("foo")));

      });
    } catch (Exception e) {
      assertThat(getExceptionStack(e),
                 StringByLineMatcher.matchesLineByLine("foo \\(org.mule.runtime.api.exception.DefaultMuleException\\)",
                                                       "  " + ExceptionHelperTestCase.class.getName()
                                                           + ".lambda\\$[^\\(]*\\(ExceptionHelperTestCase.java:[0-9]+\\)",
                                                       "  " + ExceptionHelperTestCase.class.getName()
                                                           + ".generateStackEntries\\(ExceptionHelperTestCase.java:[0-9]+\\)",
                                                       "  " + ExceptionHelperTestCase.class.getName()
                                                           + ".generateStackEntries\\(ExceptionHelperTestCase.java:[0-9]+\\)",
                                                       "  \\(" + (calls + 15) + " more...\\)")); // recursive
    }
  }

  private void generateStackEntries(int calls, Closure closure) {
    if (calls == 0) {
      closure.execute(null);
    } else {
      generateStackEntries(--calls, closure);
    }
  }

  private static final class StringByLineMatcher extends TypeSafeMatcher<String> {

    private final String[] expectedEntries;
    private int i = 0;

    private StringByLineMatcher(String... expectedEntries) {
      this.expectedEntries = expectedEntries;
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(format("line %d matches \"%s\"", i, expectedEntries[i]));
    }

    @Override
    protected boolean matchesSafely(String item) {
      String[] stackEntries = item.split(lineSeparator());

      if (stackEntries.length != expectedEntries.length) {
        return false;
      }

      for (String expectedEntry : expectedEntries) {
        if (!stackEntries[i].matches(expectedEntry)) {
          return false;
        }
        ++i;
      }

      return true;
    }

    public static StringByLineMatcher matchesLineByLine(String... expectedEntries) {
      return new StringByLineMatcher(expectedEntries);
    }
  }

  private Exception getException() {
    DefaultMuleException innerMuleException = new DefaultMuleException(createStaticMessage("bar"), new Exception("blah"));

    innerMuleException.addInfo("info_1", "Imma in!");

    DefaultMuleException outerMuleException = new DefaultMuleException(createStaticMessage("foo"), innerMuleException);

    outerMuleException.addInfo("info_1", "Imma out!");
    outerMuleException.addInfo("info_2", "Imma out!");

    return outerMuleException;
  }
}
