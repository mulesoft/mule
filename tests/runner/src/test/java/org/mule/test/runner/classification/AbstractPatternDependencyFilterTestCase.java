/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.classification;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.aether.artifact.Artifact;
import org.junit.After;
import org.junit.Before;

public abstract class AbstractPatternDependencyFilterTestCase {

  protected Artifact artifact;

  @Before
  public void before() {
    artifact = mock(Artifact.class);
    when(artifact.getGroupId()).thenReturn("org.foo");
    when(artifact.getArtifactId()).thenReturn("bar");
    when(artifact.getExtension()).thenReturn("jar");
    when(artifact.getClassifier()).thenReturn("test");
    when(artifact.getBaseVersion()).thenReturn("1.0-SNAPSHOT");
  }

  @After
  public void after() {
    verify(artifact, atLeastOnce()).getGroupId();
    verify(artifact, atLeastOnce()).getArtifactId();
    verify(artifact, atLeastOnce()).getExtension();
    verify(artifact, atLeastOnce()).getClassifier();
    verify(artifact, atLeastOnce()).getBaseVersion();
  }

  private boolean accept(String pattern) {
    AbstractPatternDependencyFilter filter = newFilter(pattern);
    return filter.accept(artifact);
  }

  protected void matches(String pattern) {
    assertThat(accept(pattern), is(true));
  }

  protected void noMatches(String pattern) {
    assertThat(accept(pattern), is(false));
  }

  protected abstract AbstractPatternDependencyFilter newFilter(String pattern);

}
