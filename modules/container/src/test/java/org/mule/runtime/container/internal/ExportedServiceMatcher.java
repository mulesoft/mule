/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.container.internal;

import org.mule.runtime.module.artifact.api.classloader.ExportedService;

import java.net.URL;

import org.hamcrest.Description;
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

  public static Matcher<ExportedService> like(String serviceInterface, URL resource) {
    return new ExportedServiceMatcher(serviceInterface, resource);
  }

  public static Matcher<ExportedService> like(ExportedService exportedService) {
    return new ExportedServiceMatcher(exportedService.getServiceInterface(), exportedService.getResource());
  }
}
