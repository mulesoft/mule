/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import org.mule.runtime.core.internal.config.CustomService;
import org.mule.runtime.core.internal.config.DefaultCustomizationService;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class DefaultCustomizationServiceTestCase extends AbstractMuleTestCase {

  public static final String SERVICE_ID = "foo";

  private DefaultCustomizationService customizationService = new DefaultCustomizationService();

  @Test
  public void overridesDefaultServiceClass() throws Exception {
    final Class<String> serviceClass = String.class;

    customizationService.overrideDefaultServiceClass(SERVICE_ID, serviceClass);

    final CustomService customService = customizationService.getOverriddenService(SERVICE_ID).get();
    assertServiceClass(customService, serviceClass);
  }

  @Test
  public void overridesDefaultService() throws Exception {
    final Object service = new Object();

    customizationService.overrideDefaultServiceImpl(SERVICE_ID, service);

    final CustomService customService = customizationService.getOverriddenService(SERVICE_ID).get();
    assertServiceInstance(customService, service);
  }

  @Test
  public void registersCustomServiceClass() throws Exception {
    final Class<String> serviceClass = String.class;

    customizationService.registerCustomServiceClass(SERVICE_ID, serviceClass);

    assertThat(customizationService.getCustomServices().size(), equalTo(1));

    final CustomService customService = customizationService.getCustomServices().get(SERVICE_ID);
    assertServiceClass(customService, serviceClass);
  }

  @Test
  public void registersCustomService() throws Exception {
    final Object service = new Object();

    customizationService.registerCustomServiceImpl(SERVICE_ID, service);

    assertThat(customizationService.getCustomServices().size(), equalTo(1));

    final CustomService customService = customizationService.getCustomServices().get(SERVICE_ID);
    assertServiceInstance(customService, service);
  }

  private void assertServiceInstance(CustomService customService, Object service) {
    assertThat(customService.getServiceImpl().get(), equalTo(service));
    assertThat(customService.getServiceClass().isPresent(), is(false));
  }

  private void assertServiceClass(CustomService customService, Class<String> serviceClass) {
    assertThat(customService.getServiceClass().get(), equalTo(serviceClass));
    assertThat(customService.getServiceImpl().isPresent(), is(false));
  }

}
