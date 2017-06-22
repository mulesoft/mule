/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor.DEFAULT_ARTIFACT_PROPERTIES_RESOURCE;
import static org.mule.runtime.deployment.model.api.application.ApplicationDescriptor.DEFAULT_CONFIGURATION_RESOURCE;
import static org.mule.runtime.deployment.model.api.application.ApplicationDescriptor.DEFAULT_CONFIGURATION_RESOURCE_LOCATION;
import static org.mule.runtime.deployment.model.api.application.ApplicationDescriptor.MULE_APPLICATION_JSON;
import org.mule.runtime.api.deployment.meta.MuleApplicationModel;
import org.mule.runtime.api.deployment.persistence.MuleApplicationModelJsonSerializer;
import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.util.PropertiesUtils;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.module.deployment.impl.internal.artifact.AbstractDeployableDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.artifact.DescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

/**
 * Creates artifact descriptor for application
 */
public class ApplicationDescriptorFactory
    extends AbstractDeployableDescriptorFactory<MuleApplicationModel, ApplicationDescriptor> {

  public static final String SYSTEM_PROPERTY_OVERRIDE = "-O";

  public ApplicationDescriptorFactory(ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader,
                                      DescriptorLoaderRepository descriptorLoaderRepository) {
    super(artifactPluginDescriptorLoader, descriptorLoaderRepository);
  }

  @Override
  protected String getDescriptorFileName() {
    return MULE_APPLICATION_JSON;
  }

  @Override
  protected void doDescriptorConfig(MuleApplicationModel artifactModel, ApplicationDescriptor descriptor) {
    artifactModel.getDomain().ifPresent(domain -> {
      descriptor.setDomain(domain);
    });

    File appClassesFolder = getAppClassesFolder(descriptor);
    // get a ref to an optional app props file (right next to the descriptor)
    setApplicationProperties(descriptor, new File(appClassesFolder, DEFAULT_ARTIFACT_PROPERTIES_RESOURCE));
  }

  @Override
  protected ApplicationDescriptor createArtifactDescriptor(String name) {
    return new ApplicationDescriptor(name);
  }

  @Override
  protected String getDefaultConfigurationResourceLocation() {
    return DEFAULT_CONFIGURATION_RESOURCE_LOCATION;
  }

  @Override
  protected String getDefaultConfigurationResource() {
    return DEFAULT_CONFIGURATION_RESOURCE;
  }

  @Override
  protected ArtifactType getArtifactType() {
    return APP;
  }

  @Override
  protected MuleApplicationModel deserializeArtifactModel(InputStream stream) throws IOException {
    return new MuleApplicationModelJsonSerializer().deserialize(IOUtils.toString(stream));
  }

  protected File getAppLibFolder(ApplicationDescriptor descriptor) {
    return MuleFoldersUtil.getAppLibFolder(descriptor.getName());
  }

  protected File getAppSharedLibsFolder(ApplicationDescriptor descriptor) {
    return MuleFoldersUtil.getAppSharedLibsFolder(descriptor.getName());
  }

  protected File getAppClassesFolder(ApplicationDescriptor descriptor) {
    return MuleFoldersUtil.getAppClassesFolder(descriptor.getName());
  }

  public void setApplicationProperties(ApplicationDescriptor desc, File appPropsFile) {
    // ugh, no straightforward way to convert a HashTable to a map
    Map<String, String> m = new HashMap<>();

    if (appPropsFile.exists() && appPropsFile.canRead()) {
      final Properties props;
      try {
        props = PropertiesUtils.loadProperties(appPropsFile.toURI().toURL());
      } catch (IOException e) {
        throw new IllegalArgumentException("Unable to obtain application properties file URL", e);
      }
      for (Object key : props.keySet()) {
        m.put(key.toString(), props.getProperty(key.toString()));
      }
    }

    // Override with any system properties prefixed with "-O" for ("override"))
    Properties sysProps = System.getProperties();
    for (Map.Entry<Object, Object> entry : sysProps.entrySet()) {
      String key = entry.getKey().toString();
      if (key.startsWith(SYSTEM_PROPERTY_OVERRIDE)) {
        m.put(key.substring(SYSTEM_PROPERTY_OVERRIDE.length()), entry.getValue().toString());
      }
    }
    desc.setAppProperties(m);
  }
}
