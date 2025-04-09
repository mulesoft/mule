/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.internal.descriptor;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;

import static java.lang.String.format;
import static java.lang.System.getProperty;

import static org.apache.commons.lang3.SystemUtils.JAVA_SPECIFICATION_VERSION;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidator;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validator for {@link DeployableArtifactDescriptor#getSupportedJavaVersions()}. Checks the currently running Java version
 * against the values provided by the descriptor.
 *
 * @since 4.6
 */
public class SupportedJvmArtifactDescriptorValidator implements ArtifactDescriptorValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(SupportedJvmArtifactDescriptorValidator.class);

  /**
   * System property to set the enforcement policy. Defined here as a decision was made not to expose it as an API yet. For now,
   * it will be for internal use only.
   *
   * @since 4.6
   */
  static final String DEPLOYABLE_ARTIFACT_JVM_ENFORCEMENT_PROPERTY =
      SYSTEM_PROPERTY_PREFIX + "jvm.version.deployableArtifact.enforcement";
  static final String JVM_ENFORCEMENT_STRICT = "STRICT";
  static final String JVM_ENFORCEMENT_LOOSE = "LOOSE";
  static final String JVM_ENFORCEMENT_DISABLED = "DISABLED";

  private final String runningJdkVersion;
  private final String enforcementMode;

  public SupportedJvmArtifactDescriptorValidator() {
    this(JAVA_SPECIFICATION_VERSION,
         getProperty(DEPLOYABLE_ARTIFACT_JVM_ENFORCEMENT_PROPERTY, JVM_ENFORCEMENT_STRICT));
  }

  SupportedJvmArtifactDescriptorValidator(String runningJdkVersion, String enforcementMode) {
    this.runningJdkVersion = runningJdkVersion;
    this.enforcementMode = enforcementMode;
  }

  @Override
  public void validate(ArtifactDescriptor descriptor) {
    if (descriptor instanceof DeployableArtifactDescriptor) {
      final Set<String> supportedJavaVersions = ((DeployableArtifactDescriptor) descriptor).getSupportedJavaVersions();

      // if the versions set is empty, assume any jvm version is good
      if (supportedJavaVersions.isEmpty()
          || supportedJavaVersions.contains(runningJdkVersion)) {
        return;
      }

      String errorMessage = getErrorMessageFor(descriptor, runningJdkVersion, supportedJavaVersions);

      if (JVM_ENFORCEMENT_STRICT.equals(enforcementMode)) {
        throw new MuleRuntimeException(createStaticMessage(errorMessage));
      } else if (JVM_ENFORCEMENT_LOOSE.equals(enforcementMode)) {
        LOGGER.warn(errorMessage);
      } else if (JVM_ENFORCEMENT_DISABLED.equals(enforcementMode)) {
        // nothing to do
      } else {
        throw new IllegalArgumentException("Unsupported " + DEPLOYABLE_ARTIFACT_JVM_ENFORCEMENT_PROPERTY + "value: "
            + enforcementMode);
      }
    }
  }

  private String getErrorMessageFor(ArtifactDescriptor descriptor, String runningJdkVersion, Set<String> supportedJavaVersions) {
    String name;
    if (descriptor instanceof ApplicationDescriptor) {
      name = "Application '" + descriptor.getName() + "'";
    } else if (descriptor instanceof DomainDescriptor) {
      name = "Domain '" + descriptor.getName() + "'";
    } else {
      name = "Artifact '" + descriptor.getName() + "'";
    }

    return format("%s does not support Java %s. Supported versions are: %s",
                  name,
                  runningJdkVersion,
                  supportedJavaVersions);
  }

}
