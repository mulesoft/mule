/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.descriptor;

import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.CLASSLOADER_MODEL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(CLASSLOADING_ISOLATION)
@Story(CLASSLOADER_MODEL)
public class ClassLoaderModelTestCase extends AbstractMuleTestCase {

  @Test
  public void sanitizesExportedResources() {
    Set<String> exportedResources = new HashSet<>();
    exportedResources.add("META-INF\\schemas\\schema.xml");
    exportedResources.add("META-INF\\README.txt");
    ClassLoaderModel classLoaderModel =
        new ClassLoaderModel.ClassLoaderModelBuilder().exportingResources(exportedResources).build();

    assertThat(classLoaderModel.getExportedResources(), containsInAnyOrder("META-INF/schemas/schema.xml", "META-INF/README.txt"));
  }
}
