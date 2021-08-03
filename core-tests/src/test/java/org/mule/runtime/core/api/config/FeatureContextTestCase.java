package org.mule.runtime.core.api.config;

import org.junit.Assert;
import org.junit.Test;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;

@SmallTest
public class FeatureContextTestCase {

    private static final List<String> suffixedVersions = Arrays.asList("4.4.0-SNAPSHOT", "4.4.0-rc1", "4.4.0-rc1-SNAPSHOT");
    private static final MuleVersion nonSuffixedVersion = new MuleVersion("4.4.0");

    @Test
    public void testSemverSuffixesMustBeIgnored() {
        suffixedVersions.forEach(suffixedVersion -> {
            FeatureContext featureContext = new FeatureContext(new MuleVersion(suffixedVersion), "");
            Assert.assertThat(featureContext.getArtifactMinMuleVersion().get().equals(nonSuffixedVersion), is(true));
        });
    }

}
