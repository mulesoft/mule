/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.infrastructure;

import static java.nio.file.Files.readAllBytes;
import static java.util.regex.Pattern.DOTALL;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matcher that allows to validate if a regex matches the content of a file.
 */
public class FileContainsRegex extends TypeSafeMatcher<File> {

  private final Pattern pattern;

  public static Matcher<File> matchesRegex(String fileLocation) {
    return new FileContainsRegex(fileLocation);
  }

  private FileContainsRegex(String regex) {
    pattern = Pattern.compile(regex, DOTALL);
  }


  @Override
  public void describeTo(Description description) {
    description.appendText("Could not find a match between the provided file and the regex" + pattern.pattern());
  }

  @Override
  public boolean matchesSafely(File file) {
    String fileContent;
    try {
      fileContent = new String(readAllBytes(file.toPath()));
      return pattern.matcher(fileContent).matches();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
