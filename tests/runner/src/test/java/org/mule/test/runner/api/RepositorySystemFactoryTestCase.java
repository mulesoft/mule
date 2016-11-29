/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

@SmallTest
public class RepositorySystemFactoryTestCase extends AbstractMuleTestCase {

  private static final String MAVEN_CENTRAL = "http://central.maven.org/maven2/";

  @Rule
  public TemporaryFolder root = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = none();

  private Artifact commonsCollectionsArtifact = new DefaultArtifact("commons-collections:commons-collections:jar:3.2");

  @Test
  public void onlineRepository() throws Exception {
    assumeThat(MAVEN_CENTRAL, isReachable());

    WorkspaceLocationResolver workspaceLocationResolver = mock(WorkspaceLocationResolver.class);
    File mavenLocalRepositoryLocation = root.newFolder("repository");
    DependencyResolver dependencyResolver = RepositorySystemFactory.newOnlineDependencyResolver(
                                                                                                Collections.<URL>emptyList(),
                                                                                                workspaceLocationResolver,
                                                                                                mavenLocalRepositoryLocation,
                                                                                                newArrayList(MAVEN_CENTRAL));

    final ArtifactResult artifactResult = dependencyResolver.resolveArtifact(commonsCollectionsArtifact);
    assertThat(artifactResult, not(nullValue()));
    assertThat(artifactResult.getArtifact().getFile().getParentFile(),
               equalTo(new File(mavenLocalRepositoryLocation,
                                "commons-collections" + File.separator + "commons-collections" + File.separator + "3.2")));
  }

  @Test
  public void offlineRepository() throws Exception {
    WorkspaceLocationResolver workspaceLocationResolver = mock(WorkspaceLocationResolver.class);
    File mavenLocalRepositoryLocation = root.newFolder("repository");
    DependencyResolver dependencyResolver = RepositorySystemFactory.newOnlineDependencyResolver(
                                                                                                Collections.<URL>emptyList(),
                                                                                                workspaceLocationResolver,
                                                                                                mavenLocalRepositoryLocation,
                                                                                                Collections.<String>emptyList());

    expectedException.expect(ArtifactResolutionException.class);
    expectedException.expectCause(instanceOf(ArtifactNotFoundException.class));
    dependencyResolver.resolveArtifact(commonsCollectionsArtifact);
  }

  private Matcher<String> isReachable() {
    return new TypeSafeMatcher<String>() {

      @Override
      public void describeTo(Description description) {
        description.appendText("Test ignored due to it requires to access internet");
      }

      @Override
      protected boolean matchesSafely(String remoteServer) {
        try {
          HttpURLConnection connection = (HttpURLConnection) new URL(remoteServer).openConnection();
          connection.setRequestMethod("HEAD");
          return connection.getResponseCode() == 200;
        } catch (IOException e) {
          return false;
        }
      }
    };
  }

}
