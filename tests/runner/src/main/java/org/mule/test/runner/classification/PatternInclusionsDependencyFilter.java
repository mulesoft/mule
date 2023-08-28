/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.runner.classification;

import static java.util.Arrays.asList;

import java.util.Collection;

/**
 * Implementation similar to {@link org.eclipse.aether.util.filter.PatternInclusionsDependencyFilter} that adds support for
 * classifier attribute.
 * <p/>
 * Format for inclusions is:
 * 
 * <pre>
 * [groupId]:[artifactId]:[extension]:[classifier]:[version]
 * </pre>
 *
 * @since 4.0
 */
public class PatternInclusionsDependencyFilter extends AbstractPatternDependencyFilter {

  /**
   * Creates the filter using the Maven coordinates
   *
   * @param coordinates that define the inclusion patterns
   */
  public PatternInclusionsDependencyFilter(final Collection<String> coordinates) {
    super(coordinates);
  }

  /**
   * Creates the filter using the Maven coordinates
   *
   * @param coordinates that define the inclusion patterns
   */
  public PatternInclusionsDependencyFilter(final String... coordinates) {
    this(asList(coordinates));
  }

}
