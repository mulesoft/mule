/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.plugin;

import static java.util.Collections.singletonList;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import com.vdurmont.semver4j.Semver;

import java.util.List;

class PluginLocalDependenciesBlacklist {

  private static List<BundleDescriptor> pluginsBlacklist = singletonList(new BundleDescriptor.Builder()
      .setGroupId("com.mulesoft.connectors").setArtifactId("mule-ibm-ctg-connector").setVersion("2.3.1").build());

  static boolean isBlacklisted(ArtifactPluginDescriptor pluginDescriptor) {
    BundleDescriptor pluginBundleDescriptor = pluginDescriptor.getBundleDescriptor();
    for (BundleDescriptor blacklistedPluginDescriptor : pluginsBlacklist) {
      if (doDescriptorsMatch(blacklistedPluginDescriptor, pluginBundleDescriptor)) {
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
