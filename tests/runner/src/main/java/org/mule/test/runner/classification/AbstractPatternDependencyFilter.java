/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.classification;

import static java.util.Arrays.asList;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;

/**
 * Base class for pattern filters.
 * <p/>
 * The artifact pattern syntax is of the form:
 * <pre>
 * [groupId]:[artifactId]:[extension]:[classifier]:[version]
 * </pre>
 *
 * @since 4.0
 */
public abstract class AbstractPatternDependencyFilter implements DependencyFilter {

  public static final String MAVEN_COORDINATES_SEPARATOR = ":";
  public static final String STAR_SYMBOL = "*";
  private final Set<String> patterns = new HashSet<String>();

  /**
   * Creates the filter using the Maven coordinates
   *
   * @param coordinates that define the inclusion patterns
   */
  public AbstractPatternDependencyFilter(final Collection<String> coordinates) {
    this.patterns.addAll(coordinates);
  }

  /**
   * Creates the filter using the Maven coordinates
   *
   * @param coordinates that define the inclusion patterns
   */
  public AbstractPatternDependencyFilter(final String... coordinates) {
    this(asList(coordinates));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean accept(final DependencyNode node, List<DependencyNode> parents) {
    final Dependency dependency = node.getDependency();
    if (dependency == null) {
      return true;
    }
    return accept(dependency.getArtifact());
  }

  /**
   * Checks if the artifact matches for the pattern defined on this filter.
   *
   * @param artifact {@link Artifact} to check if matches with the filter.
   * @return {@code true} if the artifact matches with the pattern defined.
   */
  protected boolean accept(final Artifact artifact) {
    for (final String pattern : patterns) {
      final boolean matched = accept(artifact, pattern);
      if (matched) {
        return true;
      }
    }
    return false;
  }

  private boolean accept(final Artifact artifact, final String pattern) {
    final String[] tokens =
        new String[] {artifact.getGroupId(), artifact.getArtifactId(), artifact.getExtension(), artifact.getClassifier(),
            artifact.getBaseVersion()};

    final String[] patternTokens = pattern.split(MAVEN_COORDINATES_SEPARATOR);

    // Fail immediately if pattern tokens outnumber tokens to match
    boolean matched = (patternTokens.length <= tokens.length);

    for (int i = 0; matched && i < patternTokens.length; i++) {
      matched = matches(tokens[i], patternTokens[i]);
    }

    return matched;
  }

  private boolean matches(final String token, final String pattern) {
    boolean matches;

    if (supportFullWildcardAndImpliedWildcard(pattern)) {
      matches = true;
    } else if (supportContainsWildcard(pattern)) {
      final String contains = pattern.substring(1, pattern.length() - 1);

      matches = (token.contains(contains));
    } else if (supportLeadingWildcard(pattern)) {
      final String suffix = pattern.substring(1, pattern.length());

      matches = token.endsWith(suffix);
    } else if (supportTrailingWildcard(pattern)) {
      final String prefix = pattern.substring(0, pattern.length() - 1);

      matches = token.startsWith(prefix);
    }
    // Support exact match
    else {
      matches = token.equals(pattern);
    }

    return matches;
  }


  private boolean supportFullWildcardAndImpliedWildcard(String pattern) {
    return STAR_SYMBOL.equals(pattern) || pattern.length() == 0;
  }

  private boolean supportContainsWildcard(String pattern) {
    return supportLeadingWildcard(pattern) && supportTrailingWildcard(pattern);
  }

  private boolean supportLeadingWildcard(String pattern) {
    return pattern.startsWith(STAR_SYMBOL);
  }

  private boolean supportTrailingWildcard(String pattern) {
    return pattern.endsWith(STAR_SYMBOL);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null || !getClass().equals(obj.getClass())) {
      return false;
    }

    final AbstractPatternDependencyFilter that = (AbstractPatternDependencyFilter) obj;

    return this.patterns.equals(that.patterns);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return patterns.hashCode();
  }

}
