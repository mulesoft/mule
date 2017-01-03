/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static java.io.File.separator;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor.DEFAULT_POLICY_CONFIGURATION_RESOURCE;
import static org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor.META_INF;
import static org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor.MULE_POLICY_JSON;
import static org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorFactory.PLUGIN_DEPENDENCIES;
import static org.mule.runtime.module.deployment.impl.internal.policy.FileSystemPolicyClassLoaderModelLoader.CLASSES_DIR;
import org.mule.runtime.api.deployment.meta.MulePolicyModel;
import org.mule.runtime.api.deployment.persistence.MulePolicyModelJsonSerializer;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.artifact.builder.AbstractArtifactFileBuilder;
import org.mule.tck.ZipUtils.ZipResource;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Creates Mule Application Policy files.
 */
public class PolicyFileBuilder extends AbstractArtifactFileBuilder<PolicyFileBuilder> {

  private List<ArtifactPluginFileBuilder> plugins = new LinkedList<>();
  private Properties properties = new Properties();
  private MulePolicyModel mulePolicyModel;

  public PolicyFileBuilder(String id) {
    super(id);
  }

  @Override
  public String getConfigFile() {
    return DEFAULT_POLICY_CONFIGURATION_RESOURCE;
  }

  @Override
  protected PolicyFileBuilder getThis() {
    return this;
  }


  /**
   * Adds a model describer to the policy describer file.
   *
   * @param mulePolicyModel the describer to store under
   *        {@link PolicyTemplateDescriptor#META_INF}/{@link PolicyTemplateDescriptor#MULE_POLICY_JSON} file
   * @return the same builder instance
   */
  public PolicyFileBuilder describedBy(MulePolicyModel mulePolicyModel) {
    checkImmutable();
    checkArgument(mulePolicyModel != null, "JSON describer cannot be null");
    this.mulePolicyModel = mulePolicyModel;

    return this;
  }

  @Override
  protected List<ZipResource> getCustomResources() {
    final List<ZipResource> customResources = new LinkedList<>();

    for (ArtifactPluginFileBuilder plugin : plugins) {
      customResources
          .add(new ZipResource(plugin.getArtifactFile().getAbsolutePath(),
                               "plugins/" + plugin.getArtifactFile().getName()));
    }

    if (mulePolicyModel != null) {
      final File jsonDescriptorFile = new File(getTempFolder(), META_INF + separator + MULE_POLICY_JSON);
      jsonDescriptorFile.deleteOnExit();

      String jsonDescriber = new MulePolicyModelJsonSerializer().serialize(mulePolicyModel);
      try {
        writeStringToFile(jsonDescriptorFile, jsonDescriber);
      } catch (IOException e) {
        throw new IllegalStateException("There was an issue generating the JSON file for " + this.getId(), e);
      }
      customResources.add(new ZipResource(jsonDescriptorFile.getAbsolutePath(), META_INF + "/" + MULE_POLICY_JSON));
    }
    return customResources;
  }

  /**
   * Adds a dependency against another plugin
   *
   * @param pluginName name of the plugin to be dependent. Non empty.
   * @return the same builder instance
   */
  public PolicyFileBuilder dependingOn(String pluginName) {
    checkImmutable();
    checkArgument(!isEmpty(pluginName), "Plugin name cannot be empty");
    String plugins = properties.getProperty(PLUGIN_DEPENDENCIES);
    if (isEmpty(plugins)) {
      plugins = pluginName;
    } else {
      plugins = plugins + ", " + pluginName;
    }

    properties.setProperty(PLUGIN_DEPENDENCIES, plugins);

    return this;
  }

  /**
   * Sets the configuration file used for the policy.
   *
   * @param configFile policy configuration from a external file or test resource. Non empty.
   * @return the same builder instance
   */
  public PolicyFileBuilder definedBy(String configFile) {
    checkImmutable();
    checkArgument(!StringUtils.isEmpty(configFile), "Config file cannot be empty");
    this.resources.add(new ZipResource(configFile, DEFAULT_POLICY_CONFIGURATION_RESOURCE));

    return this;
  }

  /**
   * Adds a resource file to the artifact classes folder.
   *
   * @param resourceFile class file from a external file or test resource. Non empty.
   * @param targetFile name to use on the added resource. Non empty.
   * @return the same builder instance
   */
  public PolicyFileBuilder usingResource(String resourceFile, String targetFile) {
    checkImmutable();
    checkArgument(!isEmpty(resourceFile), "Resource file cannot be empty");
    resources.add(new ZipResource(resourceFile, CLASSES_DIR + separator + targetFile));

    return getThis();
  }
}
