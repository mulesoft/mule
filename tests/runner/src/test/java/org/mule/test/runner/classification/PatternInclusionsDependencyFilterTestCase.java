/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.runner.classification;

import org.junit.Test;

public class PatternInclusionsDependencyFilterTestCase extends AbstractPatternDependencyFilterTestCase {

  @Override
  protected AbstractPatternDependencyFilter newFilter(String pattern) {
    return new PatternInclusionsDependencyFilter(pattern);
  }

  @Test
  public void invalidPatternShouldNotBeAccepted() {
    noMatches("*:*:*:*:*:*");
  }

  @Test
  public void filterByGroupId() {
    matches("*");
    noMatches("org.foobar");
    matches("*.foo");
  }

  @Test
  public void filterByGroupIdAndArtifactId() {
    matches("*:*");
    noMatches("org.foo:barr");
    matches("*.foo:*ar");
  }

  @Test
  public void filterExtension() {
    matches("org.foo:bar:jar");
    noMatches("org.foo:bar:zip");
    matches("org.foo:bar:*ar");
  }

  @Test
  public void filterClassifier() {
    matches("org.foo:bar:jar:test");
    noMatches("org.foo:bar:jar:distro");
    matches("org.foo:bar:jar:*es*");
  }

  @Test
  public void filterVersion() {
    matches("org.foo:bar:jar:test:1.0-SNAPSHOT");
    noMatches("org.foo:bar:jar:test:1.0");
    matches("org.foo:bar:jar:test:*1.0*");
  }

}
