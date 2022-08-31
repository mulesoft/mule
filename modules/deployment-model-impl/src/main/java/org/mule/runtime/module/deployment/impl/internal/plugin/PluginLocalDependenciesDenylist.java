/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.plugin;

import static java.util.Collections.unmodifiableList;

import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import java.util.ArrayList;
import java.util.List;

import com.vdurmont.semver4j.Semver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to check whether a plugin should use its local resources and packages instead of the ones of the artifact where
 * it is included.
 * <p>
 * The check was added to provide backward compatibility for artifacts that use the bug fixed in MULE-17112 as a feature.
 * <p>
 * In order to have a way to add new artifacts to the denylist, the check was added in a separate class.
 *
 * @since 4.2.2
 */
// TODO W-11086334 - remove this class after this migration
public class PluginLocalDependenciesDenylist {

  private static final Logger LOGGER = LoggerFactory.getLogger(PluginLocalDependenciesDenylist.class);

  private static final List<BundleDescriptor> PLUGINS_DENYLIST;

  static {
    List<BundleDescriptor> denylist = new ArrayList<>();

    denylist.add(new BundleDescriptor.Builder()
        .setGroupId("com.mulesoft.connectors")
        .setArtifactId("mule-ibm-ctg-connector")
        .setVersion("2.3.1").build());

    denylist.add(new BundleDescriptor.Builder()
        .setGroupId("com.mulesoft.connectors")
        .setArtifactId("mule-microsoft-dynamics-nav-connector")
        .setVersion("2.0.1").build());

    PLUGINS_DENYLIST = unmodifiableList(denylist);
  }

  /**
   * Checks if the {@link BundleDescriptor} is denylisted. It means that exists a denylisted bundle descriptor such that the group
   * id and artifact id match with the artifact bundle descriptor, and which version is greater than or equal to the artifact
   * version.
   *
   * @param pluginDescriptor {@link BundleDescriptor} to search in the denylist.
   * @return true if the {@link BundleDescriptor} is denylisted, or false otherwise.
   */
  public static boolean isDenylisted(BundleDescriptor pluginDescriptor) {
    for (BundleDescriptor denylistedPluginDescriptor : PLUGINS_DENYLIST) {
      if (doDescriptorsMatch(denylistedPluginDescriptor, pluginDescriptor)) {
        LOGGER
            .warn("Plugin '{}' local dependencies won't have precedence over the dependencies of the artifact being deployed. Please update to the latest plugin version",
                  pluginDescriptor);
        return true;
      }
    }
    return false;
  }

  private static boolean doDescriptorsMatch(BundleDescriptor denylistedDescriptor, BundleDescriptor pluginBundleDescriptor) {
    if (!doGroupsMatch(denylistedDescriptor, pluginBundleDescriptor)) {
      return false;
    }

    if (!doArtifactIdsMatch(denylistedDescriptor, pluginBundleDescriptor)) {
      return false;
    }

    return isDenylistedVersionGreaterOrEqual(denylistedDescriptor.getVersion(), pluginBundleDescriptor.getVersion());
  }

  private static boolean isDenylistedVersionGreaterOrEqual(String denylistedVersion, String pluginVersion) {
    Semver denylistedSemver = new Semver(denylistedVersion);
    Semver pluginSemver = new Semver(pluginVersion);
    return !denylistedSemver.isLowerThan(pluginSemver);
  }

  private static boolean doGroupsMatch(BundleDescriptor first, BundleDescriptor second) {
    return first.getGroupId().equals(second.getGroupId());
  }

  private static boolean doArtifactIdsMatch(BundleDescriptor first, BundleDescriptor second) {
    return first.getArtifactId().equals(second.getArtifactId());
  }
}
