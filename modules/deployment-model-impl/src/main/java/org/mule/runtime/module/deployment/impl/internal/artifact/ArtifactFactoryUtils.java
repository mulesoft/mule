/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.artifact;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor.DEFAULT_DEPLOY_PROPERTIES_RESOURCE;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.util.func.CheckedConsumer;
import org.mule.runtime.deployment.model.api.DeployableArtifact;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.module.artifact.api.Artifact;
import org.mule.runtime.module.license.api.LicenseValidator;
import org.mule.runtime.module.license.api.PluginLicenseValidationRequest;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Utility class providing useful methods when creating {@link Artifact}s.
 */
public class ArtifactFactoryUtils {

  private ArtifactFactoryUtils() {}

  /**
   * Finds the deployment file within a given artifact.
   *
   * @param artifactDir the artifact directory where the deployment file should be present
   * @return the artifact's deployment file or {@code null} if none was found
   */
  public static File getDeploymentFile(File artifactDir) {
    if (!artifactDir.exists()) {
      throw new MuleRuntimeException(I18nMessageFactory
          .createStaticMessage(String.format("Artifact directory does not exist: '%s'", artifactDir)));
    }
    File deployFile = new File(artifactDir, DEFAULT_DEPLOY_PROPERTIES_RESOURCE);
    if (!deployFile.exists()) {
      return null;
    } else {
      return deployFile;
    }
  }

  /**
   * Validates plugin licenses within the artifact.
   *
   * @param artifactClassLoader the classloader of the artifact
   * @param artifactPlugins     the list of plugins
   * @param licenseValidator    the license validator to use for validating the license
   */
  public static void validateArtifactLicense(ClassLoader artifactClassLoader, List<ArtifactPlugin> artifactPlugins,
                                             LicenseValidator licenseValidator) {
    artifactPlugins.stream().forEach(artifactPlugin -> {
      artifactPlugin.getDescriptor().getLicenseModel().ifPresent(licenseModel -> {
        licenseModel.getRequiredEntitlement().ifPresent(entitlement -> {
          String pluginName = artifactPlugin.getDescriptor().getName();
          String providerName = licenseModel.getProvider();
          licenseValidator.validatePluginLicense(PluginLicenseValidationRequest.builder()
              .withEntitlement(entitlement)
              .withPluginName(pluginName)
              .withPluginProvider(providerName)
              .withPluginVersion(artifactPlugin.getDescriptor().getBundleDescriptor().getVersion())
              .withAllowsEvaluation(licenseModel.isAllowsEvaluation())
              .withArtifactClassLoader(artifactClassLoader)
              .withPluginClassLoader(artifactPlugin.getArtifactClassLoader().getClassLoader())
              .build());
        });
      });
    });
  }

  /**
   * If the output of {@link #getMuleContext(DeployableArtifact)} over the given {@code artifact} is present, then consume it with
   * the given {@code consumer}
   *
   * @param artifact an artifact
   * @param consumer a {@link MuleContext} {@link CheckedConsumer}
   * @since 4.2
   */
  public static void withArtifactMuleContext(DeployableArtifact artifact, CheckedConsumer<MuleContext> consumer) {
    getMuleContext(artifact).ifPresent(consumer::accept);
  }

  /**
   * Optionally returns the {@link MuleContext} associated to the given {@code artifact} (if any)
   *
   * @param artifact an artifact
   * @return an {@link Optional} {@link MuleContext}
   * @since 4.2
   */
  public static Optional<MuleContext> getMuleContext(DeployableArtifact artifact) {
    return artifact == null ? empty() : ofNullable(artifact.getArtifactContext().getMuleContext());
  }

  /**
   * @param artifact a {@link DeployableArtifact}
   * @return Whether the given {@code artifact} doesn't specify any config resources.
   */
  public static boolean isConfigLess(DeployableArtifact artifact) {
    return artifact != null && artifact.getDescriptor().getConfigResources().isEmpty();
  }
}
