package org.mule.runtime.module.deployment.impl.internal.maven;

import com.google.common.collect.Lists;
import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.junit.Test;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import static com.google.common.collect.Lists.*;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ArtifactClassLoaderModelBuilderTestCase {

  private List<Profile> profiles;

  @Test
  public void testFindArtifactPackagerPluginDoesNotThrowException_IfProfileBuildIsNull(){
    // When
    Model model = mock(Model.class);
    Profile profile = mock(Profile.class);
    String profileId = "profileId";
    when(profile.getId()).thenReturn(profileId);
    profiles = newArrayList(profile);
    when(model.getProfiles()).thenReturn(profiles);

    File artifactFolder = mock(File.class);
    BundleDescriptor artifactBundleDescriptor = new BundleDescriptor.Builder()
        .setGroupId("some.group.id")
        .setArtifactId("some-artifact-id")
        .setVersion("1.2.3")
        .build();

    ArtifactClassLoaderModelBuilder artifactClassLoaderModelBuilder = new ArtifactClassLoaderModelBuilder(artifactFolder, artifactBundleDescriptor) {
      @Override
      protected List<URI> processPluginAdditionalDependenciesURIs(BundleDependency bundleDependency) {
        return null;
      }

      @Override
      protected List<String> getActiveProfiles() {
        return Collections.singletonList(profileId);
      }
    };

    // When
    try {
      artifactClassLoaderModelBuilder.findArtifactPackagerPlugin(model);
    } catch (NullPointerException t){
      fail("NullPointerException should not be thrown");
    }

  }

}