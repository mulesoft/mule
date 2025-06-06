/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config;

import static org.mule.runtime.api.artifact.ArtifactType.DOMAIN;
import static org.mule.test.allure.AllureConstants.CustomizationServiceFeature.CUSTOMIZATION_SERVICE;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import org.mule.runtime.api.config.custom.CustomizationService.ServiceInterceptor;
import org.mule.runtime.core.internal.config.CustomService;
import org.mule.runtime.core.internal.config.DefaultCustomizationService;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

import io.qameta.allure.Feature;

@Feature(CUSTOMIZATION_SERVICE)
public class DefaultCustomizationServiceTestCase extends AbstractMuleTestCase {

  public static final String SERVICE_ID = "foo";

  private DefaultCustomizationService customizationService = new DefaultCustomizationService();

  @Test
  void overridesDefaultServiceClass() throws Exception {
    final Class<String> serviceClass = String.class;

    customizationService.overrideDefaultServiceClass(SERVICE_ID, serviceClass);

    final CustomService customService = customizationService.getOverriddenService(SERVICE_ID).get();
    assertServiceClass(customService, serviceClass);
  }

  @Test
  void overridesDefaultService() throws Exception {
    final Object service = new Object();

    customizationService.overrideDefaultServiceImpl(SERVICE_ID, service);

    final CustomService customService = customizationService.getOverriddenService(SERVICE_ID).get();
    assertServiceInstance(customService, service);
  }

  @Test
  void registersCustomServiceClass() throws Exception {
    final Class<String> serviceClass = String.class;

    customizationService.registerCustomServiceClass(SERVICE_ID, serviceClass);

    assertThat(customizationService.getCustomServices().size(), equalTo(1));

    final CustomService customService = customizationService.getCustomServices().get(SERVICE_ID);
    assertServiceClass(customService, serviceClass);
  }

  @Test
  void registersCustomService() throws Exception {
    final Object service = new Object();

    customizationService.registerCustomServiceImpl(SERVICE_ID, service);

    assertThat(customizationService.getCustomServices().size(), equalTo(1));

    final CustomService customService = customizationService.getCustomServices().get(SERVICE_ID);
    assertServiceInstance(customService, service);
  }

  @Test
  void interceptsDefaultService() {
    final Object defaultService = new Object();
    final Object service = new Object();

    customizationService.interceptDefaultServiceImpl(SERVICE_ID, serviceInterceptor -> {
      assertThat(serviceInterceptor.getDefaultServiceImpl().isPresent(), is(true));
      assertThat(serviceInterceptor.getDefaultServiceImpl().get(), is(defaultService));
      serviceInterceptor.overrideServiceImpl(service);
    });

    assertThat(customizationService.getDefaultServices().size(), equalTo(1));

    final CustomService customService = customizationService.getOverriddenService(SERVICE_ID).get();
    assertServiceInstanceWithDefault(customService, defaultService, service);
  }

  @Test
  void interceptsWithoutDefaultService() {
    final Object service = new Object();

    customizationService.interceptDefaultServiceImpl(SERVICE_ID, serviceInterceptor -> {
      assertThat(serviceInterceptor.getDefaultServiceImpl().isPresent(), is(false));
      serviceInterceptor.overrideServiceImpl(service);
    });

    assertThat(customizationService.getDefaultServices().size(), equalTo(1));

    final CustomService customService = customizationService.getOverriddenService(SERVICE_ID).get();
    assertServiceInstance(customService, service);
  }

  @Test
  void skipsDefaultService() {
    customizationService.interceptDefaultServiceImpl(SERVICE_ID, ServiceInterceptor::remove);

    assertThat(customizationService.getDefaultServices().size(), equalTo(1));

    final CustomService customService = customizationService.getOverriddenService(SERVICE_ID).get();
    assertThat(customService.getServiceImpl().isPresent(), is(false));
  }

  @Test
  void artifactProperties() {
    final var artifactProperties = new HashMap<String, String>();
    artifactProperties.put("key1", "value1");
    customizationService.setArtifactProperties(artifactProperties);
    artifactProperties.put("key2", "value2");
    customizationService.setArtifactType(DOMAIN);

    assertThat(customizationService.getArtifactProperties(), hasKey("key1"));
    assertThat(customizationService.getArtifactProperties(), not(hasKey("key2")));
    assertThat(customizationService.getArtifactType(), is(DOMAIN));
  }

  private void assertServiceInstance(CustomService customService, Object service) {
    assertThat(customService.getServiceImpl().get(), equalTo(service));
    assertThat(customService.getServiceClass().isPresent(), is(false));
  }

  private void assertServiceInstanceWithDefault(CustomService customService, Object defaultService, Object service) {
    assertThat(customService.getServiceImpl(defaultService).get(), equalTo(service));
    assertThat(customService.getServiceClass().isPresent(), is(false));
  }

  private void assertServiceClass(CustomService customService, Class<String> serviceClass) {
    assertThat(customService.getServiceClass().get(), equalTo(serviceClass));
    assertThat(customService.getServiceImpl().isPresent(), is(false));
  }

}
