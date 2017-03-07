/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import static java.io.File.separator;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static java.util.Collections.emptySet;
import static org.apache.commons.io.IOCase.INSENSITIVE;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.container.api.MuleFoldersUtil.PLUGINS_FOLDER;
import static org.mule.runtime.core.config.bootstrap.ArtifactType.POLICY;
import static org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor.META_INF;
import static org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor.MULE_POLICY_JSON;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MulePolicyModel;
import org.mule.runtime.api.deployment.persistence.MulePolicyModelJsonSerializer;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptorLoader;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModelLoader;
import org.mule.runtime.module.artifact.descriptor.InvalidDescriptorLoaderException;
import org.mule.runtime.module.deployment.impl.internal.artifact.DescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.artifact.LoaderNotFoundException;
import org.mule.runtime.module.deployment.impl.internal.artifact.ServiceRegistryDescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;

/**
 * Creates descriptors for policy templates
 */
public class PolicyTemplateDescriptorFactory implements ArtifactDescriptorFactory<PolicyTemplateDescriptor> {

  protected static final String MISSING_POLICY_DESCRIPTOR_ERROR = "Policy must contain a " + MULE_POLICY_JSON + " file";

  private final ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader;
  private final DescriptorLoaderRepository descriptorLoaderRepository;

  /**
   * Creates a default factory
   */
  @SuppressWarnings({"unused"})
  public PolicyTemplateDescriptorFactory() {
    this(new ArtifactPluginDescriptorLoader(new ArtifactPluginDescriptorFactory()),
         new ServiceRegistryDescriptorLoaderRepository(new SpiServiceRegistry()));
  }

  /**
   * Creates a new factory
   *
   * @param artifactPluginDescriptorLoader loads the artifact descriptor for plugins used on the policy template. Non null
   * @param descriptorLoaderRepository contains all the {@link ClassLoaderModelLoader} registered on the container. Non null
   */
  public PolicyTemplateDescriptorFactory(ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader,
                                         DescriptorLoaderRepository descriptorLoaderRepository) {
    checkArgument(artifactPluginDescriptorLoader != null, "artifactPluginDescriptorLoader cannot be null");
    checkArgument(descriptorLoaderRepository != null, "descriptorLoaderRepository cannot be null");

    this.artifactPluginDescriptorLoader = artifactPluginDescriptorLoader;
    this.descriptorLoaderRepository = descriptorLoaderRepository;
  }

  @Override
  public PolicyTemplateDescriptor create(File artifactFolder) throws ArtifactDescriptorCreateException {
    final File policyJsonFile = new File(artifactFolder, META_INF + separator + MULE_POLICY_JSON);
    if (!policyJsonFile.exists()) {
      throw new ArtifactDescriptorCreateException(MISSING_POLICY_DESCRIPTOR_ERROR);
    }

    final MulePolicyModel mulePolicyModel = getMulePolicyJsonDescriber(policyJsonFile);

    final PolicyTemplateDescriptor descriptor = new PolicyTemplateDescriptor(mulePolicyModel.getName());
    descriptor.setRootFolder(artifactFolder);

    if (mulePolicyModel.getClassLoaderModelLoaderDescriptor().isPresent()) {
      descriptor.setClassLoaderModel(getClassLoaderModel(artifactFolder, mulePolicyModel));
    }

    descriptor.setBundleDescriptor(getBundleDescriptor(artifactFolder, mulePolicyModel));
    descriptor.setPlugins(parseArtifactPluginDescriptors(artifactFolder, descriptor));

    return descriptor;
  }

  private BundleDescriptor getBundleDescriptor(File artifactFolder, MulePolicyModel mulePolicyModel) {
    BundleDescriptorLoader bundleDescriptorLoader;
    try {
      bundleDescriptorLoader =
          descriptorLoaderRepository.get(mulePolicyModel.getBundleDescriptorLoader().getId(), POLICY,
                                         BundleDescriptorLoader.class);
    } catch (LoaderNotFoundException e) {
      throw new ArtifactDescriptorCreateException(invalidBundleDescriptorLoaderIdError(mulePolicyModel
          .getBundleDescriptorLoader()));
    }
    try {
      return bundleDescriptorLoader.load(artifactFolder,
                                         mulePolicyModel.getBundleDescriptorLoader().getAttributes());
    } catch (InvalidDescriptorLoaderException e) {
      throw new ArtifactDescriptorCreateException(e);
    }
  }

  private ClassLoaderModel getClassLoaderModel(File artifactFolder, MulePolicyModel mulePolicyModel) {
    MuleArtifactLoaderDescriptor muleArtifactLoaderDescriptor = mulePolicyModel.getClassLoaderModelLoaderDescriptor().get();
    ClassLoaderModelLoader classLoaderModelLoader;
    try {
      classLoaderModelLoader =
          descriptorLoaderRepository.get(muleArtifactLoaderDescriptor.getId(), POLICY, ClassLoaderModelLoader.class);
    } catch (LoaderNotFoundException e) {
      throw new ArtifactDescriptorCreateException(invalidClassLoaderModelIdError(muleArtifactLoaderDescriptor));
    }

    try {
      return classLoaderModelLoader
          .load(artifactFolder, mulePolicyModel.getClassLoaderModelLoaderDescriptor().get().getAttributes());
    } catch (InvalidDescriptorLoaderException e) {
      throw new ArtifactDescriptorCreateException(e);
    }
  }

  protected static String invalidBundleDescriptorLoaderIdError(MuleArtifactLoaderDescriptor bundleDescriptorLoader) {
    return "Unknown bundle descriptor loader: " + bundleDescriptorLoader.getId();
  }

  protected static String invalidClassLoaderModelIdError(MuleArtifactLoaderDescriptor muleArtifactLoaderDescriptor) {
    return "Unknown model loader: " + muleArtifactLoaderDescriptor.getId();
  }

  private Set<ArtifactPluginDescriptor> parseArtifactPluginDescriptors(File artifactFolder, PolicyTemplateDescriptor descriptor) {
    final File pluginsDir = new File(artifactFolder, PLUGINS_FOLDER);
    // TODO(pablo.kraan): MULE-11383 all artifacts must be .jar files
    String[] pluginZips = pluginsDir.list(new SuffixFileFilter(asList(".zip", ".jar"), INSENSITIVE));
    if (pluginZips == null) {
      return emptySet();
    }

    final Set<ArtifactPluginDescriptor> plugins = new HashSet<>(pluginZips.length);

    if (pluginZips != null && pluginZips.length != 0) {
      sort(pluginZips);

      for (String pluginZip : pluginZips) {
        File pluginZipFile = new File(pluginsDir, pluginZip);
        try {
          plugins.add(artifactPluginDescriptorLoader
              .load(pluginZipFile));
        } catch (IOException e) {
          throw new ArtifactDescriptorCreateException("Cannot load plugin descriptor: " + pluginZip, e);
        }
      }
    }

    return plugins;
  }

  private MulePolicyModel getMulePolicyJsonDescriber(File jsonFile) {
    try (InputStream stream = new FileInputStream(jsonFile)) {
      return new MulePolicyModelJsonSerializer().deserialize(IOUtils.toString(stream));
    } catch (IOException e) {
      throw new IllegalArgumentException(format("Could not read extension describer on plugin '%s'", jsonFile.getAbsolutePath()),
                                         e);
    }
  }
}
