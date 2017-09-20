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

  private static final Map<String, PatternInfo> patterns = new HashMap<>();

  static {
    patterns.put(ANT_TEMPLATE_STYLE, new PatternInfo(ANT_TEMPLATE_STYLE, "\\$\\{[^\\{\\}]+\\}", "${", "}"));
    patterns.put(SQUARE_TEMPLATE_STYLE, new PatternInfo(SQUARE_TEMPLATE_STYLE, "\\[[^\\[\\]]+\\]", "[", "]"));
    patterns.put(CURLY_TEMPLATE_STYLE, new PatternInfo(CURLY_TEMPLATE_STYLE, "\\{[^\\{\\}}]+\\}", "{", "}"));

    // Such a complex regex is needed to support nested expressions, otherwise we
    // have to do this manually or using an ANTLR grammar etc.

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

  protected String parse(Map<?, ?> props, String template, TemplateCallback callback) {
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
