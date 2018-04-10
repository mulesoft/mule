/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.util;

import org.mule.runtime.core.api.util.CaseInsensitiveHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>TemplateParser</code> is a simple string parser that will substitute tokens in a string with values supplied in a Map.
 */
public final class TemplateParser {

  public static final String ANT_TEMPLATE_STYLE = "ant";
  public static final String SQUARE_TEMPLATE_STYLE = "square";
  public static final String CURLY_TEMPLATE_STYLE = "curly";
  public static final String WIGGLY_MULE_TEMPLATE_STYLE = "mule";
  private static final String NULL_AS_STRING = "null";
  private static final char START_EXPRESSION = '#';
  private static final char OPEN_EXPRESSION = '[';
  private static final char CLOSE_EXPRESSION = ']';

  private static final Map<String, PatternInfo> patterns = new HashMap<>();

  static {
    patterns.put(ANT_TEMPLATE_STYLE, new PatternInfo(ANT_TEMPLATE_STYLE, "\\$\\{[^\\{\\}]+\\}", "${", "}"));
    patterns.put(SQUARE_TEMPLATE_STYLE, new PatternInfo(SQUARE_TEMPLATE_STYLE, "\\[[^\\[\\]]+\\]", "[", "]"));
    patterns.put(CURLY_TEMPLATE_STYLE, new PatternInfo(CURLY_TEMPLATE_STYLE, "\\{[^\\{\\}}]+\\}", "{", "}"));

    // Such a complex regex is needed to support nested expressions, otherwise we
    // have to do this manually or using an ANTLR grammar etc.

    //TODO MULE-14603 - Expression Regex fails on detect expression when this have an unbalanced opening bracket
    // Support for 6 levels (5 nested)
    patterns.put(WIGGLY_MULE_TEMPLATE_STYLE,
                 new PatternInfo(WIGGLY_MULE_TEMPLATE_STYLE,
                                 "#\\[((?:#?\\[(?:#?\\[(?:#?\\[(?:#?\\[(?:#?\\[.*?\\]|[^\\[\\]])*?\\]|[^\\[\\]])*?\\]|[^\\[\\]])*?\\]|[^\\[\\]])*?\\]|[^\\[\\]])*?)\\]",
                                 "#[", "]"));
  }

  /**
   * logger used by this class
   */
  protected static final Logger logger = LoggerFactory.getLogger(TemplateParser.class);

  private final Pattern pattern;
  private final int pre;
  private final int post;
  private final PatternInfo style;


  public static TemplateParser createAntStyleParser() {
    return new TemplateParser(ANT_TEMPLATE_STYLE);
  }

  public static TemplateParser createSquareBracesStyleParser() {
    return new TemplateParser(SQUARE_TEMPLATE_STYLE);
  }

  public static TemplateParser createMuleStyleParser() {
    return new TemplateParser(WIGGLY_MULE_TEMPLATE_STYLE);
  }

  private TemplateParser(String styleName) {
    this.style = patterns.get(styleName);
    if (this.style == null) {
      throw new IllegalArgumentException("Unknown template style: " + styleName);

    }
    pattern = style.getPattern();
    pre = style.getPrefix().length();
    post = style.getSuffix().length();
  }

  /**
   * Matches one or more templates against a Map of key value pairs. If a value for a template is not found in the map the
   * template is left as is in the return String
   *
   * @param props the key/value pairs to match against
   * @param template the string containing the template place holders i.e. My name is ${name}
   * @return the parsed String
   */
  public String parse(Map<?, ?> props, String template) {
    return parse(props, template, null);
  }

  /**
   * Matches one or more templates against a Map of key value pairs. If a value for a template is not found in the map the
   * template is left as is in the return String
   *
   * @param callback a callback used to resolve the property name
   * @param template the string containing the template place holders i.e. My name is ${name}
   * @return the parsed String
   */
  public String parse(TemplateCallback callback, String template) {
    return parse(null, template, callback);
  }

  private String parseMule(Map<?, ?> props, String template, TemplateCallback callback) {
    if (!validateBalanceMuleStyle(template)) {
      return template;
    }

    boolean lastIsBackSlash = false;
    boolean lastStartedExpression = false;
    boolean openSingleQuotes = false;

    StringBuilder result = new StringBuilder();
    int currentPosition = 0;
    while (currentPosition < template.length()) {
      char c = template.charAt(currentPosition);

      if (lastStartedExpression && c != OPEN_EXPRESSION) {
        result.append(START_EXPRESSION);
      }

      if (lastIsBackSlash && c != '\'' && c != '"') {
        result.append("\\");
      }

      if (!lastIsBackSlash && c == '\'') {
        openSingleQuotes = !openSingleQuotes;
      }
      if (c == OPEN_EXPRESSION && lastStartedExpression && !openSingleQuotes) {
        int closing = closingBracesPosition(template, currentPosition);
        String enclosingTemplate = template.substring(currentPosition + 1, closing);

        Object value = enclosingTemplate;
        if (callback != null) {
          value = callback.match(enclosingTemplate);
          if (value == null) {
            value = NULL_AS_STRING;
          } else {
            value = parseMule(props, value.toString(), callback);
          }
        }
        result.append(value);

        currentPosition = closing;
      } else if (c != START_EXPRESSION && c != '\\') {
        result.append(c);
      }

      lastStartedExpression = c == START_EXPRESSION;
      lastIsBackSlash = c == '\\';
      currentPosition++;
    }

    return result.toString();
  }

  private int closingBracesPosition(String template, int startingPosition) {
    // This assumes that the template is balanced (simply validate first)
    int openingBraces = 1;
    boolean lastIsBackSlash = false;
    boolean openSingleQuotes = false;
    for (int i = startingPosition + 1; i < template.length(); i++) {
      char c = template.charAt(i);
      if (c == CLOSE_EXPRESSION && !openSingleQuotes) {
        openingBraces--;
      } else if (c == OPEN_EXPRESSION && !openSingleQuotes) {
        openingBraces++;
      } else if (!lastIsBackSlash && c == '\'') {
        openSingleQuotes = !openSingleQuotes;
      } else if (!lastIsBackSlash && c == '"') {
      }
      lastIsBackSlash = c == '\\';

      if (openingBraces == 0) {
        return i;
      }
    }
    return -1;
  }

  protected String parse(Map<?, ?> props, String template, TemplateCallback callback) {
    if (styleIs(WIGGLY_MULE_TEMPLATE_STYLE)) {
      return parseMule(props, template, callback);
    }
    String result = template;
    Map<?, ?> newProps = props;
    if (props != null && !(props instanceof CaseInsensitiveHashMap)) {
      newProps = new CaseInsensitiveHashMap(props);
    }

    Matcher m = pattern.matcher(result);

    while (m.find()) {
      Object value = null;

      String match = m.group();
      String propname = match.substring(pre, match.length() - post);

      if (callback != null) {
        value = callback.match(propname);
        if (value == null) {
          value = NULL_AS_STRING;
        }
      } else if (newProps != null) {
        value = newProps.get(propname);
      }

      if (value == null) {
        if (logger.isDebugEnabled()) {
          logger.debug("Value " + propname + " not found in context");
        }
      } else {
        String matchRegex = Pattern.quote(match);
        String valueString = value.toString();
        valueString = replaceBackSlash(valueString);
        valueString = replaceDollarSign(valueString);

        result = result.replaceAll(matchRegex, valueString);
      }
    }
    return result;
  }

  private boolean styleIs(String style) {
    return this.getStyle().getName().equals(style);
  }

  private boolean validateBalanceMuleStyle(String template) {
    Stack<Character> stack = new Stack<>();
    boolean lastStartedExpression = false;
    boolean lastIsBackSlash = false;
    int openBraces = 0;
    int openSingleQuotes = 0;
    int openDoubleQuotes = 0;

    for (int i = 0; i < template.length(); i++) {
      char c = template.charAt(i);
      switch (c) {
        case '\'':
          if (lastIsBackSlash) {
            break;
          }
          if (!stack.empty() && stack.peek().equals('\'')) {
            stack.pop();
            openSingleQuotes--;
          } else {
            stack.push(c);
            openSingleQuotes++;
          }
          break;
        case '"':
          if (lastIsBackSlash) {
            break;
          }
          if (!stack.empty() && stack.peek().equals('"')) {
            stack.pop();
            openDoubleQuotes--;
          } else {
            stack.push(c);
            openDoubleQuotes++;
          }
          break;
        case CLOSE_EXPRESSION:
          if (!stack.empty() && stack.peek().equals(OPEN_EXPRESSION)) {
            stack.pop();
            openBraces--;
          }
          break;
        case OPEN_EXPRESSION:
          if ((lastStartedExpression || openBraces > 0) && !(openDoubleQuotes > 0 || openSingleQuotes > 0)) {
            stack.push(c);
            openBraces++;
          }
          break;
      }
      lastStartedExpression = c == START_EXPRESSION;
      lastIsBackSlash = c == '\\';
    }

    return stack.empty();
  }

  private String replaceDollarSign(String valueString) {
    if (valueString.indexOf('$') != -1) {
      valueString = valueString.replace("$", "\\$");
    }
    return valueString;
  }

  private String replaceBackSlash(String valueString) {

    if (valueString.indexOf("\\") != -1) {
      valueString = valueString.replace("\\", "\\\\");
    }
    return valueString;
  }

  /**
   * Matches one or more templates against a Map of key value pairs. If a value for a template is not found in the map the
   * template is left as is in the return String
   *
   * @param props the key/value pairs to match against
   * @param templates A List of templates
   * @return the parsed String
   */
  public List<?> parse(Map<?, ?> props, List<?> templates) {
    if (templates == null) {
      return new ArrayList<>();
    }

    List<String> list = new ArrayList<>(templates.size());
    templates.stream().map(tmpl -> parse(props, tmpl.toString())).forEach(list::add);
    return list;
  }

  /**
   * Matches one or more templates against a Map of key value pairs. If a value for a template is not found in the map the
   * template is left as is in the return String
   *
   * @param props the key/value pairs to match against
   * @param templates A Map of templates. The values for each map entry will be parsed
   * @return the parsed String
   */
  public Map<?, ?> parse(final Map<?, ?> props, Map<?, ?> templates) {
    return parse(token -> props.get(token), templates);
  }

  public Map<?, ?> parse(TemplateCallback callback, Map<?, ?> templates) {
    if (templates == null) {
      return new HashMap<>();
    }

    Map<Object, String> map = new HashMap<>(templates.size());
    for (Map.Entry<?, ?> entry : templates.entrySet()) {
      map.put(entry.getKey(), parse(callback, entry.getValue().toString()));
    }
    return map;
  }

  public PatternInfo getStyle() {
    return style;
  }

  public boolean isContainsTemplate(String value) {
    if (value == null) {
      return false;
    }

    Matcher m = pattern.matcher(value);
    return m.find();
  }

  public boolean isValid(String expression) {
    if (styleIs(WIGGLY_MULE_TEMPLATE_STYLE)) {
      return validateBalanceMuleStyle(expression);
    }

    try {
      style.validate(expression);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public void validate(String expression) throws IllegalArgumentException {
    style.validate(expression);
  }

  @FunctionalInterface
  public interface TemplateCallback {

    Object match(String token);
  }


  public static class PatternInfo {

    String name;
    String regEx;
    String prefix;
    String suffix;

    PatternInfo(String name, String regEx, String prefix, String suffix) {
      this.name = name;
      this.regEx = regEx;
      if (prefix.length() < 1 || prefix.length() > 2) {
        throw new IllegalArgumentException("Prefix can only be one or two characters long: " + prefix);
      }
      this.prefix = prefix;
      if (suffix.length() != 1) {
        throw new IllegalArgumentException("Suffix can only be one character long: " + suffix);
      }
      this.suffix = suffix;
    }

    public String getRegEx() {
      return regEx;
    }

    public String getPrefix() {
      return prefix;
    }

    public String getSuffix() {
      return suffix;
    }

    public String getName() {
      return name;
    }

    public Pattern getPattern() {
      return Pattern.compile(regEx, Pattern.CASE_INSENSITIVE);
    }

    public void validate(String expression) throws IllegalArgumentException {
      String currentExpression = expression;
      int lastMatchIdx = 0;
      while (lastMatchIdx < expression.length()) {
        int start = currentExpression.indexOf(prefix);
        if (start == -1) {
          // no more expressions to validate
          break;
        }
        lastMatchIdx += start;
        currentExpression = currentExpression.substring(start);
        Matcher m = getPattern().matcher(currentExpression);
        boolean found = m.find();
        if (found) {
          if (!currentExpression.startsWith(m.group())) {
            throw new IllegalArgumentException("Invalid Expression");
          }
          int matchSize = m.group().length();
          lastMatchIdx += matchSize;
          currentExpression = currentExpression.substring(matchSize);
        } else {
          throw new IllegalArgumentException("Invalid Expression");
        }
      }
    }

  }
}
