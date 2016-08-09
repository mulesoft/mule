/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.file.api.matcher;

import static org.mule.runtime.core.util.Preconditions.checkArgument;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * A {@link Predicate} which tests random paths in {@link String} representation to match a specific pattern.
 * <p>
 * The pattern can be either a regex or glob expression. The pattern can qualify itself by using thee &quot;glob:&quot; or
 * &quot;regex:&quot; prefixes. If no prefix is supplied, glob is assumed by default.
 *
 * @since 4.0
 */
public final class PathMatcherPredicate implements Predicate<String> {

  private static final String GLOB_PREFIX = "glob:";
  private static final String REGEX_PREFIX = "regex:";

  private final Predicate<String> delegate;

  /**
   * Creates a new instance using the given pattern
   *
   * @param pattern the pattern to be used to test paths.
   */
  public PathMatcherPredicate(String pattern) {
    delegate = getPredicateForFilename(pattern);
  }

  /**
   * @param path the path to test
   * @return whether the given {@code path} matches {@code this} instance pattern
   */
  @Override
  public boolean test(String path) {
    checkArgument(!StringUtils.isBlank(path), "Cannot match a blank filename");
    return delegate.test(path);
  }

  private Predicate<String> getPredicateForFilename(String pattern) {
    if (pattern.startsWith(REGEX_PREFIX)) {
      return Pattern.compile(stripRegexPrefix(pattern)).asPredicate();
    } else if (pattern.startsWith(GLOB_PREFIX)) {
      return getGlobPredicate(pattern);
    } else {
      return getGlobPredicate(GLOB_PREFIX + pattern);
    }
  }

  private Predicate<String> getGlobPredicate(String pattern) {
    PathMatcher matcher = FileSystems.getDefault().getPathMatcher(pattern);
    return path -> matcher.matches(Paths.get(path));
  }

  private String stripRegexPrefix(String pattern) {
    return pattern.replaceAll(REGEX_PREFIX, "");
  }
}
