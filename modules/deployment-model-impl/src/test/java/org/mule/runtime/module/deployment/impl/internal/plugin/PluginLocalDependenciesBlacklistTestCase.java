/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.plugin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.module.deployment.impl.internal.plugin.PluginLocalDependenciesBlacklist.isBlacklisted;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class PluginLocalDependenciesBlacklistTestCase extends AbstractMuleTestCase {

  @Test
  public void ibmCTGIsBlacklisted() {
    BundleDescriptor ctgBundleDescriptor = new BundleDescriptor.Builder().setGroupId("com.mulesoft.connectors")
        .setArtifactId("mule-ibm-ctg-connector").setVersion("2.3.1").build();
    assertThat(isBlacklisted(ctgBundleDescriptor), is(true));
  }

  @Test
  public void priorVersionsAreBlacklisted() {
    BundleDescriptor priorMajor = new BundleDescriptor.Builder().setGroupId("com.mulesoft.connectors")
        .setArtifactId("mule-ibm-ctg-connector").setVersion("1.4.4").build();

    BundleDescriptor priorMinor = new BundleDescriptor.Builder().setGroupId("com.mulesoft.connectors")
        .setArtifactId("mule-ibm-ctg-connector").setVersion("2.2.4").build();

    BundleDescriptor priorPatch = new BundleDescriptor.Builder().setGroupId("com.mulesoft.connectors")
        .setArtifactId("mule-ibm-ctg-connector").setVersion("2.3.0").build();

    assertThat(isBlacklisted(priorMajor), is(true));
    assertThat(isBlacklisted(priorMinor), is(true));
    assertThat(isBlacklisted(priorPatch), is(true));
  }

  @Test
  public void latterVersionsAreNotBlacklisted() {
    BundleDescriptor latterMajor = new BundleDescriptor.Builder().setGroupId("com.mulesoft.connectors")
        .setArtifactId("mule-ibm-ctg-connector").setVersion("3.0.0").build();

    BundleDescriptor latterMinor = new BundleDescriptor.Builder().setGroupId("com.mulesoft.connectors")
        .setArtifactId("mule-ibm-ctg-connector").setVersion("2.4.0").build();

    BundleDescriptor latterPatch = new BundleDescriptor.Builder().setGroupId("com.mulesoft.connectors")
        .setArtifactId("mule-ibm-ctg-connector").setVersion("2.3.2").build();

    assertThat(isBlacklisted(latterMajor), is(false));
    assertThat(isBlacklisted(latterMinor), is(false));
    assertThat(isBlacklisted(latterPatch), is(false));
  }

  @Test
  public void wrongGroupOrArtifactDontMatch() {
    BundleDescriptor wrongGroup = new BundleDescriptor.Builder().setGroupId("com.mulesoft.wrong")
        .setArtifactId("mule-ibm-ctg-connector").setVersion("2.3.1").build();
    assertThat(isBlacklisted(wrongGroup), is(false));

    BundleDescriptor wrongArtifact = new BundleDescriptor.Builder().setGroupId("com.mulesoft.connectors")
        .setArtifactId("mule-ibm-ctg-wrong").setVersion("2.3.1").build();
    assertThat(isBlacklisted(wrongArtifact), is(false));
  }

}
