/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.plugin;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import com.vdurmont.semver4j.Semver;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to check whether a plugin should use its local resources and packages instead of the ones of the
 * artifact where it is included.
 * The check was added to provide backward compatibility for artifacts that use the bug fixed in MULE-17112
 * as a feature.
 * In order to have a way to add new artifacts to the blacklist, the check was added in a separate class.
 *
 * @since 4.2.2
 */
class PluginLocalDependenciesBlacklist {

  private static final Logger logger = LoggerFactory.getLogger(PluginLocalDependenciesBlacklist.class);

  private static List<BundleDescriptor> pluginsBlacklist = singletonList(new BundleDescriptor.Builder()
      .setGroupId("com.mulesoft.connectors").setArtifactId("mule-ibm-ctg-connector").setVersion("2.3.1").build());

  /**
   * Checks if the {@link ArtifactPluginDescriptor} is blacklisted. It means that exists a blacklisted bundle
   * descriptor such that the group id and artifact id match with the artifact bundle descriptor, and which
   * version is greater than or equal to the artifact version.
   *
   * @param pluginDescriptor {@link ArtifactPluginDescriptor} to search in the blacklist.
   * @return true if the {@link ArtifactPluginDescriptor} is blacklisted, or false otherwise.
   */
  static boolean isBlacklisted(ArtifactPluginDescriptor pluginDescriptor) {
    BundleDescriptor pluginBundleDescriptor = pluginDescriptor.getBundleDescriptor();
    for (BundleDescriptor blacklistedPluginDescriptor : pluginsBlacklist) {
      if (doDescriptorsMatch(blacklistedPluginDescriptor, pluginBundleDescriptor)) {
        if (logger.isWarnEnabled()) {
          logger
              .warn(format("Plugin '%s' won't load the local dependencies as locals, please update to the latest plugin version",
                           pluginDescriptor.getBundleDescriptor()));
        }
        return true;
      }
    }
    return false;
  }

  private static boolean doDescriptorsMatch(BundleDescriptor blacklistedDescriptor, BundleDescriptor pluginBundleDescriptor) {
    if (!doGroupsMatch(blacklistedDescriptor, pluginBundleDescriptor)) {
      return false;
    }

    if (!doArtifactIdsMatch(blacklistedDescriptor, pluginBundleDescriptor)) {
      return false;
    }

    return isBlacklistedVersionGreaterOrEqual(blacklistedDescriptor.getVersion(), pluginBundleDescriptor.getVersion());
  }

  private static boolean isBlacklistedVersionGreaterOrEqual(String blacklistedVersion, String pluginVersion) {
    Semver blacklistedSemver = new Semver(blacklistedVersion);
    Semver pluginSemver = new Semver(pluginVersion);

    // Check majors
    if (blacklistedSemver.getMajor() < pluginSemver.getMajor()) {
      return false;
    }

    if (blacklistedSemver.getMajor() > pluginSemver.getMajor()) {
      return true;
    }

    // Majors are equals, check minors
    if (blacklistedSemver.getMinor() < pluginSemver.getMinor()) {
      return false;
    }

    if (blacklistedSemver.getMinor() > pluginSemver.getMinor()) {
      return true;
    }

    // Majors and minors are equals, check patch versions
    if (blacklistedSemver.getPatch() < pluginSemver.getPatch()) {
      return false;
    }

    return true;
  }

  private static boolean doGroupsMatch(BundleDescriptor first, BundleDescriptor second) {
    return first.getGroupId().equals(second.getGroupId());
  }

  private static boolean doArtifactIdsMatch(BundleDescriptor first, BundleDescriptor second) {
    return first.getArtifactId().equals(second.getArtifactId());
  }
}
