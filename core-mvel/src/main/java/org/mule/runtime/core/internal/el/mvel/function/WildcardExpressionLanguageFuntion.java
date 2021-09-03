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
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.core.internal.el.context.MessageContext;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;

public class WildcardExpressionLanguageFuntion implements ExpressionLanguageFunction {

  private static final boolean DEFAULT_SENSITIVITY = true;

  @Override
  public Object call(Object[] params, ExpressionLanguageContext context) {
    int numParams = params.length;
    if (numParams < 1 || numParams > 3) {
      throw new IllegalArgumentException("invalid number of arguments for the signature wildcard("
          + "wildcardExpression, [melExpression], [caseSensitive]) was expected");
    }

    String wildcardPattern = verifyWildcardPattern(params[0]);
    boolean result = false;

    if (numParams == 1) {
      try {
        result = isMatch(wildcardPattern, context.getVariable("message", MessageContext.class).payloadAs(String.class),
                         DEFAULT_SENSITIVITY);
      } catch (TransformerException e) {
        throw new RuntimeException("Unable to convert payload to string");
      }
    } else {
      String text = verifyText(params[1]);
      if (numParams == 2) {
        result = isMatch(wildcardPattern, text, DEFAULT_SENSITIVITY);

      } else if (numParams == 3) {
        result = isMatch(wildcardPattern, text, (Boolean) params[2]);
      }
    }

    return result;
  }

  protected String verifyWildcardPattern(Object wildcardPattern) {
    if (wildcardPattern == null) {
      throw new IllegalArgumentException("wildcard pattern is null");
    } else if (!(wildcardPattern instanceof String)) {
      throw new IllegalArgumentException("wildcard pattern is not a string");
    } else if (StringUtils.isBlank((String) wildcardPattern)) {
      throw new IllegalArgumentException("wildcard pattern cannot be blank");
    }

    return (String) wildcardPattern;
  }

  protected String verifyText(Object text) {
    if (text == null) {
      throw new IllegalArgumentException("text is null");
    } else if (!(text instanceof String)) {
      throw new IllegalArgumentException("text is not a string");
    }
    return (String) text;
  }

  protected boolean isMatch(String wildcardPattern, String text, boolean caseSensitive) {
    return FilenameUtils.wildcardMatch(text, wildcardPattern, caseSensitive ? IOCase.SENSITIVE : IOCase.INSENSITIVE);
  }

}
