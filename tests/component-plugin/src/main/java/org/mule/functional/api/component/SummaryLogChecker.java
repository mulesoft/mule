/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SummaryLogChecker extends AbstractLogChecker {

  private static final Pattern SUMMARY_REGEX_PATTERN = compile("([^:]*):(.*)");

  private List<SummaryInfo> expectedInfo;
  private boolean exclusiveContent;

  @Override
  public void check(String logMessage) {
    StringBuilder errors = new StringBuilder();
    Map<String, String> actualInfoMap = new HashMap<>();
    List<String> actualLines = getMessageLinesFromLogLines(splitLines(logMessage));
    for (String line : actualLines) {
      Matcher matcher = SUMMARY_REGEX_PATTERN.matcher(line);
      if (matcher.matches()) {
        actualInfoMap.put(matcher.group(1).trim(), matcher.group(2).trim());
      }
    }
    if (exclusiveContent) {
      checkExclusive(actualInfoMap, errors);
    } else {
      checkNotExclusive(actualInfoMap, errors);
    }

    String errorMessage = errors.toString();
    if (isNotBlank(errorMessage)) {
      throw new AssertionError(lineSeparator() + errorMessage);
    }
  }

  private void evaluatePresentInfo(Map<String, String> actualInfo, StringBuilder errors) {
    for (SummaryInfo expectedInfoElement : expectedInfo) {
      if (assertAndSaveError(actualInfo,
                             hasKey(expectedInfoElement.getKey()),
                             "Missing summary line:",
                             errors)) {

        if (expectedInfoElement.getValue() != null) {
          assertAndSaveError(actualInfo.get(expectedInfoElement.getKey()),
                             is(equalTo(expectedInfoElement.getValue())),
                             format("\"%s\" has the wrong info:", expectedInfoElement.getKey()),
                             errors);
        }
      }
    }
  }

  private void checkExclusive(Map<String, String> actualInfo, StringBuilder errors) {
    evaluatePresentInfo(actualInfo, errors);
    if (actualInfo.size() > expectedInfo.size()) {
      Set<String> expectedInfoKeySet = expectedInfo.stream().map(SummaryInfo::getKey).collect(toSet());
      Set<String> extraInfo = actualInfo.keySet();
      extraInfo.removeAll(expectedInfoKeySet);
      for (String key : extraInfo) {
        errors.append(lineSeparator());
        errors.append(format("Unwanted information found. Key: \"%s\" Value: \"%s\"", key, actualInfo.get(key)));
        errors.append(lineSeparator());
      }
    }
  }

  private void checkNotExclusive(Map<String, String> expected, StringBuilder errors) {
    evaluatePresentInfo(expected, errors);
  }

  public boolean isExclusiveContent() {
    return exclusiveContent;
  }

  public void setExclusiveContent(boolean exclusiveContent) {
    this.exclusiveContent = exclusiveContent;
  }

  public List<SummaryInfo> getExpectedInfo() {
    return expectedInfo;
  }

  public void setExpectedInfo(List<SummaryInfo> expectedInfo) {
    this.expectedInfo = expectedInfo;
  }

  public static class SummaryInfo {

    private String key;
    private String value = null;

    public SummaryInfo() {}

    public SummaryInfo(String key, String value) {
      this.key = key;
      this.value = value;
    }

    public SummaryInfo(String key) {
      this.key = key;
    }

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

  }
}
