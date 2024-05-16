/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TemplateParserToken {

  private static final Random RANDOM = new Random();

  private final String id;
  private final Pattern searchPattern;

  public static TemplateParserToken getNewToken() {
    // The token ID needs to be valid in any context in which the original expression was valid -> using an integer
    String id = '1' + format("%010d", RANDOM.nextInt() & MAX_VALUE);
    return new TemplateParserToken(id);
  }

  private TemplateParserToken(String id) {
    this.id = id;
    this.searchPattern = Pattern.compile(id);
  }

  public String getId() {
    return id;
  }

  public Pattern getSearchPattern() {
    return searchPattern;
  }

  public Replacement buildReplacement(String replacement) {
    return new Replacement(this, replacement);
  }

  static class Provider {

    private static final List<TemplateParserToken> tokensPool = new ArrayList<>();

    private int curOffset = 0;

    public TemplateParserToken getToken() {
      // TODO: thread safety
      if (tokensPool.size() <= curOffset) {
        tokensPool.add(TemplateParserToken.getNewToken());
      }

      return tokensPool.get(curOffset++);
    }
  }

  static class Replacement {

    private final TemplateParserToken token;
    private final String replacement;

    private Replacement(TemplateParserToken token, String replacement) {
      this.token = token;
      this.replacement = replacement;
    }

    public String replace(String original, Function<String, String> replacementMapper) {
      Matcher matcher = token.getSearchPattern().matcher(original);
      if (!matcher.find()) {
        return original;
      }

      // Can't use replaceFirst or appendReplacement because those make special treatment for backslashes and dollar signs,
      // we don't want that
      return original.substring(0, matcher.start())
          + replacementMapper.apply(this.replacement)
          + original.substring(matcher.end());
    }
  }
}
