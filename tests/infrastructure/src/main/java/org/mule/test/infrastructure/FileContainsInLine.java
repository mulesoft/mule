/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure;

import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.file.Files.newBufferedReader;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;


public class FileContainsInLine extends TypeSafeMatcher<File> {

  private final Matcher<String> stringMatcher;

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
    try (BufferedReader reader = newBufferedReader(file.toPath(), defaultCharset())) {

      while ((line = reader.readLine()) != null) {
        if (stringMatcher.matches(line)) {
          return true;
        }
      }
    } catch (IOException e) {
      fail(format("Exception %s caught while reading the file %s trying to match its line with the matcher %s",
                  e.getMessage(), file.getAbsolutePath(), stringMatcher.toString()));
      return false;
    }
    return false;
  }
}
