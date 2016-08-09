/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.infrastructure;

import static java.nio.charset.Charset.defaultCharset;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.junit.internal.matchers.TypeSafeMatcher;


public class FileContainsInLine extends TypeSafeMatcher<File> {

  private final Matcher<String> stringMatcher;

  @Factory
  public static Matcher<File> hasLine(Matcher<String> matcher) {
    return new FileContainsInLine(matcher);
  }

  private FileContainsInLine(Matcher<String> matcher) {
    stringMatcher = matcher;
  }


  @Override
  public void describeTo(Description description) {
    description.appendText("a file where a line ").appendDescriptionOf(stringMatcher);
  }

  @Override
  public boolean matchesSafely(File file) {
    String line;
    try (BufferedReader reader = Files.newBufferedReader(file.toPath(), defaultCharset())) {

      while ((line = reader.readLine()) != null) {
        if (stringMatcher.matches(line)) {
          return true;
        }
      }
    } catch (IOException e) {
      fail(String.format("Exception %s caught while reading the file %s trying to match its line with the matcher %s",
                         e.getMessage(), file.getAbsolutePath(), stringMatcher.toString()));
      return false;
    }
    return false;
  }
}
