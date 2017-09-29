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
import static org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor.DEFAULT_POLICY_CONFIGURATION_RESOURCE;
import static org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor.META_INF;
import static org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor.MULE_ARTIFACT;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.MULE_ARTIFACT_JSON_DESCRIPTOR;
import org.mule.runtime.api.deployment.meta.MulePolicyModel;
import org.mule.runtime.api.deployment.persistence.MulePolicyModelJsonSerializer;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.tck.ZipUtils.ZipResource;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Creates Mule Application Policy files.
 */
public class PolicyFileBuilder extends DeployableFileBuilder<PolicyFileBuilder> {

  private MulePolicyModel mulePolicyModel;

  public PolicyFileBuilder(String artifactId) {
    super(artifactId);
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
   *        {@link PolicyTemplateDescriptor#META_INF}/{@link PolicyTemplateDescriptor#MULE_ARTIFACT_JSON_DESCRIPTOR} file
   * @return the same builder instance
   */
  public PolicyFileBuilder describedBy(MulePolicyModel mulePolicyModel) {
    checkImmutable();
    checkArgument(mulePolicyModel != null, "JSON describer cannot be null");
    this.mulePolicyModel = mulePolicyModel;

    return this;
  }

  @Override
  protected List<ZipResource> doGetCustomResources() {
    final List<ZipResource> customResources = new LinkedList<>();

    if (mulePolicyModel != null) {
      final File jsonDescriptorFile =
          new File(getTempFolder(), META_INF + separator + MULE_ARTIFACT + separator + MULE_ARTIFACT_JSON_DESCRIPTOR);
      jsonDescriptorFile.deleteOnExit();

      String jsonDescriber = new MulePolicyModelJsonSerializer().serialize(mulePolicyModel);
      try {
        writeStringToFile(jsonDescriptorFile, jsonDescriber);
      } catch (IOException e) {
        throw new IllegalStateException("There was an issue generating the JSON file for " + this.getId(), e);
      }
      customResources
          .add(new ZipResource(jsonDescriptorFile.getAbsolutePath(),
                               META_INF + "/" + MULE_ARTIFACT + "/" + MULE_ARTIFACT_JSON_DESCRIPTOR));
    }
    return customResources;
  }

}
