/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.mvel.function;

import org.mule.runtime.core.internal.el.mvel.ExpressionLanguageContext;
import org.mule.runtime.core.internal.el.mvel.ExpressionLanguageFunction;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.el.context.MessageContext;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.map.LRUMap;

public class RegexExpressionLanguageFuntion implements ExpressionLanguageFunction {

  private static final int SINGLE_CAPTURE_GROUP = 1;
  private static final int NO_CAPTURE_GROUP = 0;

  // Caches compiled patterns to improve performance
  private Map<String, Pattern> patterns = Collections.synchronizedMap(new LRUMap(256));

  @Override
  public Object call(Object[] params, ExpressionLanguageContext context) {
    int numParams = params.length;
    if (numParams < 1 || numParams > 3) {
      throw new IllegalArgumentException("invalid number of arguments");
    }

    String regex = verifyRegex(params[0]);
    Object result = null;

    if (numParams == 1) {
      try {
        result = getMatches(regex, context.getVariable("message", MessageContext.class).payloadAs(String.class), 0, context);
      } catch (TransformerException e) {
        throw new RuntimeException("Unable to convert payload to string");
      }
    } else {
      String text = verifyText(params[1]);
      if (numParams == 2) {
        result = getMatches(regex, text, 0, context);

      } else if (numParams == 3) {
        result = getMatches(regex, text, verifyFlags(params[2]), context);
      }
    }
    return result;
  }

  protected String verifyRegex(Object regex) {
    if (regex == null) {
      throw new IllegalArgumentException("regular expression is null");
    } else if (!(regex instanceof String)) {
      throw new IllegalArgumentException("regular expression is not a string");
    }
    return (String) regex;
  }

  protected String verifyText(Object text) {
    if (text == null) {
      throw new IllegalArgumentException("text is null");
    } else if (!(text instanceof String)) {
      throw new IllegalArgumentException("text is not a string");
    }
    return (String) text;
  }

  protected int verifyFlags(Object flags) {
    if (flags == null) {
      return 0;
    } else if (!(flags instanceof Integer)) {
      throw new IllegalArgumentException("regular expression flags is not an integer");
    }
    return (Integer) flags;
  }

  protected Object getMatches(String regex, String text, int flags, ExpressionLanguageContext context) {
    Matcher matcher = createMatcher(regex, text, flags);
    Object result = null;

    if (matcher.matches()) {
      if (matcher.groupCount() == NO_CAPTURE_GROUP || matcher.groupCount() == SINGLE_CAPTURE_GROUP) {
        result = matcher.group(matcher.groupCount());
      } else {
        String[] matchedValues = new String[matcher.groupCount()];

        for (int i = 1; i <= matcher.groupCount(); i++) {
          matchedValues[i - 1] = matcher.group(i);
        }

        result = matchedValues;
      }
    }
    return result;
  }

  private Matcher createMatcher(String expression, String string, int flags) {
    Pattern pattern = patterns.get(expression + flags);

    if (pattern == null) {
      pattern = Pattern.compile(expression, flags);
      patterns.put(expression + flags, pattern);
    }

    return pattern.matcher(string);
  }

}
