package org.mule.runtime.module.deployment.internal.util;

import static org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor.EXTENSION_BUNDLE_TYPE;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.ARTIFACT_ID;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.CLASSIFIER;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.GROUP_ID;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.TYPE;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.VERSION;

import static org.apache.commons.io.FileUtils.copyInputStreamToFile;

import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.collection.SmallMap;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

public final class Utils {

    public static File getResourceFile(String resource) throws URISyntaxException {
        return new File(Utils.class.getResource(resource).toURI());
    }

    public static File getResourceFile(String resource, File tempFolder) {
        final File targetFile = new File(tempFolder, resource);
        try {
            copyInputStreamToFile(Utils.class.getResourceAsStream(resource), targetFile);
        } catch (IOException e) {
            throw new MuleRuntimeException(e);
        }
        return targetFile;
    }

    public static MuleArtifactLoaderDescriptor createBundleDescriptorLoader(String artifactId, String classifier,
                                                                               String bundleDescriptorLoaderId) {
        return createBundleDescriptorLoader(artifactId, classifier, bundleDescriptorLoaderId, "1.0.0");
    }

    public static MuleArtifactLoaderDescriptor createBundleDescriptorLoader(String artifactId, String classifier,
                                                                             String bundleDescriptorLoaderId, String version) {
        Map<String, Object> attributes = SmallMap.of(VERSION, version,
                GROUP_ID, "org.mule.test",
                ARTIFACT_ID, artifactId,
                CLASSIFIER, classifier,
                TYPE, EXTENSION_BUNDLE_TYPE);

        return new MuleArtifactLoaderDescriptor(bundleDescriptorLoaderId, attributes);
    }
}
