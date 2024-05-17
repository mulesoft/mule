/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.util;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.String.format;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Represents a token in the context of the parsing of a template.
 * <p>
 * Tokens are used to represent a section of the template that needs to be replaced with the result of some sub-template
 * resolution.
 *
 * @see TemplateParser
 */
class TemplateParserToken {

  private static final Random RANDOM = new Random();

  private static TemplateParserToken getNewToken() {
    // The token ID needs to be valid in any context in which the original expression was valid -> using an integer
    String id = '1' + format("%010d", RANDOM.nextInt() & MAX_VALUE);
    return new TemplateParserToken(id);
  }

  private final Pattern searchPattern;

  private TemplateParserToken(String id) {
    this.searchPattern = Pattern.compile(id);
  }

  /**
   * @return The token ID. This is what will be replaced later on.
   */
  public String getId() {
    return searchPattern.pattern();
  }

  /**
   * @param replacement The replacement string that will be used at the replacement time.
   * @return A token bound to a specific replacement string.
   */
  public Replacement buildReplacement(String replacement) {
    return new Replacement(replacement);
  }

  /**
   * Entry point to retrieve {@link TemplateParserToken}s.
   * <p>
   * Performs caching of the tokens for performance and to achieve repeatability of token assignation for the same template input.
   * This is important for evaluation callbacks that use caching.
   */
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

    /**
     * Provides {@link TemplateParserToken}s.
     * <p>
     * It is guaranteed that the same {@link Provider} instance will never return the same token.
     * <p>
     * The same {@link TemplateParserToken} may be provided by different {@link Provider} instances.
     * <p>
     * Thread-safe.
     *
     * @return A {@link TemplateParserToken} ready to use.
     */
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


  /**
   * Represents a {@link TemplateParserToken} associated with a string to replace it with.
   */
  class Replacement {

    private final String replacement;

    private Replacement(String replacement) {
      this.replacement = replacement;
    }

    /**
     * Replaces all the occurrences of the token in the given string by the bound replacement string after applying a mapping
     * function.
     * <p>
     * The mapping function is not invoked if there is no match. It should not have side effects.
     *
     * @param original          The original string, potentially with tokens to be replaced.
     * @param replacementMapper The mapping function to apply to the bound replacement string before actual replacement.
     * @return The resulting string after replacements.
     */
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
