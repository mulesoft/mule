/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.internal;

import org.mule.runtime.module.artifact.api.classloader.ExportedService;

import java.net.URL;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matches a {@link ExportedService} against a given specification.
 */
public class ExportedServiceMatcher extends TypeSafeMatcher<ExportedService> {

  private final String serviceInterface;
  private final URL resource;

  private ExportedServiceMatcher(String serviceInterface, URL resource) {
    this.serviceInterface = serviceInterface;
    this.resource = resource;
  }

  @Override
  protected boolean matchesSafely(ExportedService exportedService) {
    boolean sameServiceInterface = serviceInterface == null ? exportedService.getServiceInterface() == null
        : serviceInterface.equals(exportedService.getServiceInterface());
    boolean sameEncoding = exportedService.getResource().equals(resource);

    return sameServiceInterface && sameEncoding;
  }

  @Override
  public void describeTo(Description description) {
    description
        .appendText("a ExportedService with serviceInterface = ").appendValue(serviceInterface)
        .appendText(", resource = ").appendValue(resource);
  }

  @Override
  protected void describeMismatchSafely(ExportedService exportedService, Description mismatchDescription) {
    mismatchDescription
        .appendText("got a ExportedService with serviceInterface = ").appendValue(exportedService.getServiceInterface())
        .appendText(", resource = ").appendValue(exportedService.getResource());

  }

  @Factory
  public static Matcher<ExportedService> like(String serviceInterface, URL resource) {
    return new ExportedServiceMatcher(serviceInterface, resource);
  }

  @Factory
  public static Matcher<ExportedService> like(ExportedService exportedService) {
    return new ExportedServiceMatcher(exportedService.getServiceInterface(), exportedService.getResource());
  }
}
