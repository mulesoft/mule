/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorFactoryProvider.artifactDescriptorFactoryProvider;

import static org.apache.commons.io.IOUtils.copy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.artifact.DescriptorLoaderRepositoryFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.internal.util.ServiceRegistryDescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Test the overriding of app properties by system properties
 */
public class PropertyOverridesTestCase extends AbstractMuleTestCase {

  private final Map<String, String> existingProperties = new HashMap<>();

  private void setSystemProperties() {
    setSystemProperty("texas", "province");
    setSystemProperty("-Operu", "nation");
    setSystemProperty("-Dtexas.capital", "houston");
    setSystemProperty("-O-Dperu.capital", "bogota");
    setSystemProperty("-Omule", "wayCool");
    setSystemProperty("-Omule.mmc", "evenCooler");
  }

  @Test
  public void testOverrides() throws Exception {
    File tempProps = File.createTempFile("property", "overrides");
    InputStream input = getClass().getClassLoader().getResourceAsStream("overridden.properties");
    FileOutputStream output = new FileOutputStream(tempProps);
    copy(input, output);
    input.close();
    output.close();
    ApplicationDescriptor descriptor = new ApplicationDescriptor("app");
    ApplicationDescriptorFactory applicationDescriptorFactory =
        new ApplicationDescriptorFactory(new ArtifactPluginDescriptorLoader(artifactDescriptorFactoryProvider()
            .createArtifactPluginDescriptorFactory(new DescriptorLoaderRepositoryFactory().createDescriptorLoaderRepository(),
                                                   ArtifactDescriptorValidatorBuilder.builder())),
                                         new ServiceRegistryDescriptorLoaderRepository(),
                                         ArtifactDescriptorValidatorBuilder.builder());
    applicationDescriptorFactory.setApplicationProperties(descriptor, tempProps);
    Map<String, String> appProps = descriptor.getAppProperties();
    assertEquals("state", appProps.get("texas"));
    assertEquals("country", appProps.get("peru"));
    assertEquals("austin", appProps.get("texas.capital"));
    assertEquals("4", appProps.get("peru.capital.numberOfletters"));
    assertEquals("runtime", appProps.get("mule"));
    assertEquals("ipaas", appProps.get("mule.ion"));

    try {
      setSystemProperties();
      descriptor = new ApplicationDescriptor("app");
      applicationDescriptorFactory.setApplicationProperties(descriptor, tempProps);
      appProps = descriptor.getAppProperties();
      assertEquals("state", appProps.get("texas"));
      assertEquals("nation", appProps.get("peru"));
      assertEquals("austin", appProps.get("texas.capital"));
      assertEquals("4", appProps.get("peru.capital.numberOfletters"));
      assertEquals("wayCool", appProps.get("mule"));
      assertEquals("ipaas", appProps.get("mule.ion"));
      assertEquals("evenCooler", appProps.get("mule.mmc"));

      descriptor = new ApplicationDescriptor("app");
      applicationDescriptorFactory.setApplicationProperties(descriptor, new File("nonexistent.nonexistent"));
      appProps = descriptor.getAppProperties();
      assertNull(appProps.get("texas"));
      assertEquals("nation", appProps.get("peru"));
      assertNull(appProps.get("texas.capital"));
      assertNull(appProps.get("peru.capital.numberOfletters"));
      assertEquals("wayCool", appProps.get("mule"));
      assertNull(appProps.get("mule.ion"));
      assertEquals("evenCooler", appProps.get("mule.mmc"));
    } finally {
      resetSystemProperties();
    }
  }

  private void resetSystemProperties() {
    for (Map.Entry<String, String> entry : existingProperties.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      if (value == null) {
        System.getProperties().remove(key);
      } else {
        System.setProperty(key, entry.getValue());
      }
    }
  }

  private void setSystemProperty(String key, String value) {
    String previous = System.setProperty(key, value);
    existingProperties.put(key, previous);
  }
}
