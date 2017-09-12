package org.mule.functional.api.component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class SummaryLogChecker extends AbstractLogChecker {

  private static final Pattern SUMMARY_REGEX_PATTERN = Pattern.compile("([^:]*):(.*)");

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
    if (!StringUtils.isBlank(errorMessage)) {
      throw new AssertionError(errorMessage);
    }
  }

  private void evaluatePresentInfo(Map<String, String> actualInfo, StringBuilder errors) {
    for (SummaryInfo expectedInfoElement : expectedInfo) {
      if (!actualInfo.containsKey(expectedInfoElement.getKey())) {
        errors.append(String.format("Missing summary line. Expected: \"%s\" with info: \"%s\"\n", expectedInfoElement.getKey(),
                                    expectedInfoElement.getValue()));
      } else {
        if (expectedInfoElement.getValue() != null
            && !actualInfo.get(expectedInfoElement.getKey()).equals(expectedInfoElement.getValue())) {
          errors.append(String.format("\"%s\" has the wrong info.\nEXPECTED: \"%s\"\nGOT: \"%s\"\n",
                                      expectedInfoElement.getValue(), actualInfo.get(expectedInfoElement.getKey())));
        }
      }
    }
  }

  private void checkExclusive(Map<String, String> actualInfo, StringBuilder errors) {
    evaluatePresentInfo(actualInfo, errors);
    if (actualInfo.size() > expectedInfo.size()) {
      Set<String> expectedInfoKeySet = expectedInfo.stream().map(SummaryInfo::getKey).collect(Collectors.toSet());
      Set<String> extraInfo = actualInfo.keySet();
      extraInfo.removeAll(expectedInfoKeySet);
      for (String key : extraInfo) {
        errors.append(String.format("Unwanted information found. Key: \"%s\" Value: \"%s\\n", key, actualInfo.get(key)));
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
