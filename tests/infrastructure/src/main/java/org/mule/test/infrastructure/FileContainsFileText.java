/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure;

import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;


public class FileContainsFileText extends TypeSafeMatcher<File> {

  private final File fileContentToMatch;
  private final List<String> fileToMatchContent = new ArrayList<>();
  private int lineToMatchIndex = 0;

  public static Matcher<File> hasFileContent(String fileLocation) {
    return new FileContainsFileText(fileLocation);
  }

  private FileContainsFileText(String fileLocation) {
    try {
      this.fileContentToMatch = new File(this.getClass().getClassLoader().getResource(fileLocation).toURI());
      try (BufferedReader contentToMatchReader = Files.newBufferedReader(fileContentToMatch.toPath(), defaultCharset())) {
        String line;
        while ((line = contentToMatchReader.readLine()) != null) {
          fileToMatchContent.add(line);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void describeTo(Description description) {
    description.appendText("Could not find a match between the provided file and " + fileContentToMatch.getAbsolutePath());
  }

  @Override
  public boolean matchesSafely(File file) {
    try (BufferedReader reader = Files.newBufferedReader(file.toPath(), defaultCharset())) {
      String currentLine;
      while ((currentLine = reader.readLine()) != null) {
        if (fileToMatchContent.get(lineToMatchIndex).trim().equals(currentLine.trim())) {
          if (lineToMatchIndex == fileToMatchContent.size() - 1) {
            return true;
          }
          lineToMatchIndex++;
        } else {
          lineToMatchIndex = 0;
        }
      }
    } catch (IOException e) {
      fail(format("Exception %s caught while reading the file %s",
                  e.getMessage(), file.getAbsolutePath()));
      return false;
    }
    return false;
  }
}
