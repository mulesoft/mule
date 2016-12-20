/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor.DEFAULT_POLICY_CONFIGURATION_RESOURCE;
import static org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorFactory.PLUGIN_DEPENDENCIES;
import static org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateDescriptorFactory.POLICY_PROPERTIES;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.module.artifact.builder.AbstractArtifactFileBuilder;
import org.mule.tck.ZipUtils;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Creates Mule Application Policy files.
 */
public class PolicyFileBuilder extends AbstractArtifactFileBuilder<PolicyFileBuilder> {

  private List<ArtifactPluginFileBuilder> plugins = new LinkedList<>();
  private Properties properties = new Properties();

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

  @Override
  protected List<ZipUtils.ZipResource> getCustomResources() {
    final List<ZipUtils.ZipResource> customResources = new LinkedList<>();

    for (ArtifactPluginFileBuilder plugin : plugins) {
      customResources
          .add(new ZipUtils.ZipResource(plugin.getArtifactFile().getAbsolutePath(),
                                        "plugins/" + plugin.getArtifactFile().getName()));
    }

    if (!properties.isEmpty()) {
      final File applicationPropertiesFile = new File(getTempFolder(), POLICY_PROPERTIES);
      applicationPropertiesFile.deleteOnExit();
      createPropertiesFile(applicationPropertiesFile, properties);

      customResources.add(new ZipUtils.ZipResource(applicationPropertiesFile.getAbsolutePath(), POLICY_PROPERTIES));
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
    this.resources.add(new ZipUtils.ZipResource(configFile, DEFAULT_POLICY_CONFIGURATION_RESOURCE));

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
    resources.add(new ZipUtils.ZipResource(resourceFile, "classes/" + targetFile));

    return getThis();
  }

  /**
   * Adds a property into the policy properties file.
   *
   * @param propertyName name fo the property to add. Non empty
   * @param propertyValue value of the property to add. Non null.
   * @return the same builder instance
   */
  public PolicyFileBuilder configuredWith(String propertyName, String propertyValue) {
    checkImmutable();
    checkArgument(!isEmpty(propertyName), "Property name cannot be empty");
    checkArgument(propertyValue != null, "Property value cannot be null");
    properties.put(propertyName, propertyValue);

    return this;
  }
}
