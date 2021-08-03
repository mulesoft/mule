package org.mule.runtime.core.api.config;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.meta.MuleVersion;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

@SmallTest
public class FeatureContextTestCase {

  private static final List<String> suffixedVersions = Arrays.asList("4.4.0-SNAPSHOT", "4.4.0-rc1", "4.4.0-rc1-SNAPSHOT");
  private static final MuleVersion nonSuffixedVersion = new MuleVersion("4.4.0");

  @Test
  public void testSemverSuffixesMustBeIgnored() {
    suffixedVersions.forEach(suffixedVersion -> {
      FeatureContext featureContext = new FeatureContext(new MuleVersion(suffixedVersion), "");
      assertThat(featureContext.getArtifactMinMuleVersion().get().sameAs(nonSuffixedVersion), is(true));
    });
  }

}
