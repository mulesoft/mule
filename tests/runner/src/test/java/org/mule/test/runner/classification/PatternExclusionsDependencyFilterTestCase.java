/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.runner.classification;

import org.junit.Test;

public class PatternExclusionsDependencyFilterTestCase extends AbstractPatternDependencyFilterTestCase {

  @Override
  protected AbstractPatternDependencyFilter newFilter(String pattern) {
    return new PatternExclusionsDependencyFilter(pattern);
  }

  @Test
  public void invalidPatternShouldBeAccepted() {
    matches("*:*:*:*:*:*");
  }

  @Test
  public void filterByGroupId() {
    noMatches("*");
    matches("org.foobar");
    noMatches("*.foo");
  }

  @Test
  public void filterByGroupIdAndArtifactId() {
    noMatches("*:*");
    matches("org.foo:barr");
    noMatches("*.foo:*ar");
  }

  @Test
  public void filterExtension() {
    noMatches("org.foo:bar:jar");
    matches("org.foo:bar:zip");
    noMatches("org.foo:bar:*ar");
  }

  @Test
  public void filterClassifier() {
    noMatches("org.foo:bar:jar:test");
    matches("org.foo:bar:jar:distro");
    noMatches("org.foo:bar:jar:*es*");
  }

  @Test
  public void filterVersion() {
    noMatches("org.foo:bar:jar:test:1.0-SNAPSHOT");
    matches("org.foo:bar:jar:test:1.0");
    noMatches("org.foo:bar:jar:test:*1.0*");
  }

}
