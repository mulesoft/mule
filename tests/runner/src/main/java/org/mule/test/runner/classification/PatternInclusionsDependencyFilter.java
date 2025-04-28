/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
