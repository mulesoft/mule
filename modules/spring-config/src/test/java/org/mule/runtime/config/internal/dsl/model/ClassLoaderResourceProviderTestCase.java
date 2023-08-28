/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.dsl.model;

import static java.lang.Thread.currentThread;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.runtime.config.internal.model.dsl.ClassLoaderResourceProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

public class ClassLoaderResourceProviderTestCase extends AbstractMuleTestCase {

  private ClassLoaderResourceProvider resourceProvider;

  @Before
  public void setUp() {
    resourceProvider = new ClassLoaderResourceProvider(currentThread().getContextClassLoader());
  }

  @Test
  public void sameJarResourceGetsLoaded() {
    verifyResourceGetsLoadedSuccessfully("META-INF/mule-core.xsd");
  }

  @Test
  public void differentJarResourceGetsLoaded() {
    verifyResourceGetsLoadedSuccessfully("javax/inject/Inject.class");
  }

  @Test
  public void absolutePathResourceGetsLoaded() {
    File file = FileUtils.toFile(getClass().getClassLoader().getResource("META-INF/mule-module.properties"));
    verifyResourceGetsLoadedSuccessfully(file.getAbsolutePath());
  }

  private void verifyResourceGetsLoadedSuccessfully(String resource) {
    try (InputStream content = resourceProvider.getResourceAsStream(resource)) {
      assertThat(content, notNullValue());
    } catch (IOException e) {
      fail();
    }
  }

}
