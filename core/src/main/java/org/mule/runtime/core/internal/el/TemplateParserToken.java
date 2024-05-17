/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.String.format;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TemplateParserToken {

  private static TemplateParserToken getNewToken() {
    // The token ID needs to be valid in any context in which the original expression was valid -> using an integer
    String id = '1' + format("%010d", RANDOM.nextInt() & MAX_VALUE);
    return new TemplateParserToken(id);
  }

  private static final Random RANDOM = new Random();

  private final Pattern searchPattern;

  private TemplateParserToken(String id) {
    this.searchPattern = Pattern.compile(id);
  }

  public String getId() {
    return searchPattern.pattern();
  }

  public Replacement buildReplacement(String replacement) {
    return new Replacement(replacement);
  }

  static class Provider {

    private static final int POOL_INITIAL_SIZE = 10;

    // Choosing a copy-on-write array list because we anticipate reads to be extremely dominant.
    // This will give us no contention during reads at the expense of O(N) on the appends.
    // We are giving it a sensible initial size that should be good for most templates.
    // For applications with templates requiring more tokens, appends will stop happening once the application is warmed up.
    private static final List<TemplateParserToken> tokensPool = new CopyOnWriteArrayList<>(createInitialTokensBatch());

    private static TemplateParserToken[] createInitialTokensBatch() {
      TemplateParserToken[] tokens = new TemplateParserToken[POOL_INITIAL_SIZE];
      for (int i = 0; i < tokens.length; i++) {
        tokens[i] = TemplateParserToken.getNewToken();
      }
      return tokens;
    }

    private int curOffset = 0;

    public TemplateParserToken getToken() {
      if (tokensPool.size() <= curOffset) {
        // Because we are not in a critical section, it is possible that another thread may have already added a new token to the
        // pool, so we might be adding a token we don't strictly need. We consider that an acceptable trade-off for minimizing
        // contention.
        tokensPool.add(TemplateParserToken.getNewToken());
      }

      return tokensPool.get(curOffset++);
    }
  }

  class Replacement {

    private final String replacement;

    private Replacement(String replacement) {
      this.replacement = replacement;
    }

    public String replace(String original, Function<String, String> replacementMapper) {
      Matcher matcher = searchPattern.matcher(original);
      if (!matcher.find()) {
        return original;
      }

      // Now that we know there was a match, we can apply the mapper
      String mappedReplacement = replacementMapper.apply(replacement);

      // Can't use replaceFirst or appendReplacement because those make special treatment for backslashes and dollar signs,
      // we don't want that
      int lastEndOfMatch = 0;
      StringBuilder sb = new StringBuilder();
      do {
        sb.append(original, lastEndOfMatch, matcher.start());
        sb.append(mappedReplacement);
        lastEndOfMatch = matcher.end();
      } while (matcher.find());

      sb.append(original, lastEndOfMatch, original.length());
      return sb.toString();
    }
  }
}
