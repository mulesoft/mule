/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.maven;

import static java.util.Collections.singletonList;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.qameta.allure.Issue;
import java.io.File;
import java.net.URI;
import java.util.List;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.junit.Test;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

public class ArtifactClassLoaderModelBuilderTestCase {

  private List<Profile> profiles;

  @Test
  @Issue("MULE-19355")
  public void testFindArtifactPackagerPluginDoesNotThrowException_IfProfileBuildIsNull() {
    // Given
    Model model = mock(Model.class);
    Profile profile = mock(Profile.class);
    String profileId = "profileId";
    when(profile.getId()).thenReturn(profileId);
    profiles = singletonList(profile);
    when(model.getProfiles()).thenReturn(profiles);

    File artifactFolder = mock(File.class);
    BundleDescriptor artifactBundleDescriptor = new BundleDescriptor.Builder()
        .setGroupId("some.group.id")
        .setArtifactId("some-artifact-id")
        .setVersion("1.2.3")
        .build();

    ArtifactClassLoaderModelBuilder artifactClassLoaderModelBuilder =
        new ArtifactClassLoaderModelBuilder(artifactFolder, artifactBundleDescriptor) {

          @Override
          protected List<URI> processPluginAdditionalDependenciesURIs(BundleDependency bundleDependency) {
            return null;
          }

          @Override
          protected List<String> getActiveProfiles() {
            return singletonList(profileId);
          }
        };

    // When
    artifactClassLoaderModelBuilder.findArtifactPackagerPlugin(model);

    // Then
    // No NPE is thrown

  }

}
