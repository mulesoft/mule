/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.classification;

import static java.util.Arrays.asList;

import java.util.Collection;

import org.eclipse.aether.artifact.Artifact;

/**
 * Implementation similar to {@link org.eclipse.aether.util.filter.PatternExclusionsDependencyFilter} that adds support for
 * classifier attribute.
 * <p/>
 * Format for exclusions is:
 * 
 * <pre>
 * [groupId]:[artifactId]:[extension]:[classifier]:[version]
 * </pre>
 *
 * @since 4.0
 */
public class PatternExclusionsDependencyFilter extends AbstractPatternDependencyFilter {

  /**
   * Creates the filter using the Maven coordinates
   *
   * @param coordinates that define the exclusion patterns
   */
  public PatternExclusionsDependencyFilter(final Collection<String> coordinates) {
    super(coordinates);
  }

  /**
   * Creates the filter using the Maven coordinates
   *
   * @param coordinates that define the exclusion patterns
   */
  public PatternExclusionsDependencyFilter(final String... coordinates) {
    this(asList(coordinates));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean accept(Artifact artifact) {
    return !super.accept(artifact);
  }

}
