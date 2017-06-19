/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.application;

import static org.mule.runtime.container.api.MuleFoldersUtil.CLASSES_FOLDER;
import static org.mule.runtime.container.api.MuleFoldersUtil.LIB_FOLDER;
import static org.mule.runtime.container.api.MuleFoldersUtil.SHARED_FOLDER;
import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.module.deployment.impl.internal.artifact.DescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;

import java.io.File;

/**
 * Creates temporary applications descriptors that are outside the default Mule applications directory
 * {@link MuleFoldersUtil#getAppsFolder()}.
 */
public class TemporaryApplicationDescriptorFactory extends ApplicationDescriptorFactory {

  public TemporaryApplicationDescriptorFactory(ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader,
                                               DescriptorLoaderRepository descriptorLoaderRepository) {
    super(artifactPluginDescriptorLoader, descriptorLoaderRepository);
  }

  @Override
  protected File getAppClassesFolder(ApplicationDescriptor descriptor) {
    return new File(getApplicationFolder(descriptor), CLASSES_FOLDER);
  }

  @Override
  protected File getAppLibFolder(ApplicationDescriptor descriptor) {
    return new File(getApplicationFolder(descriptor), LIB_FOLDER);
  }

  @Override
  protected File getAppSharedLibsFolder(ApplicationDescriptor descriptor) {
    return new File(new File(getApplicationFolder(descriptor), LIB_FOLDER), SHARED_FOLDER);
  }

  private File getApplicationFolder(ApplicationDescriptor descriptor) {
    return new File(descriptor.getRootFolder(), descriptor.getName());
  }
}

