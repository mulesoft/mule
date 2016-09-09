/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.el.ExpressionLanguage;

import java.util.Arrays;

/**
 * Includes basic configuration for the HTTP Cache-Control Header
 */
public class CacheControlHeader {

  private static final String[] DIRECTIVE = {"public", "private"};

  private String directive;
  private String noCache;
  private String noStore;
  private String mustRevalidate;
  private String maxAge;


  public CacheControlHeader() {
    noCache = "false";
    noStore = "false";
    mustRevalidate = "false";
  }

  /**
   * Evaluates all the properties in case there are expressions
   *
   * @param event MuleEvent
   * @param expressionLanguage
   */
  public void parse(Event event, ExpressionLanguage expressionLanguage) {
    directive = parse(directive, event, expressionLanguage);
    checkDirective(directive);
    noCache = parse(noCache, event, expressionLanguage);
    noStore = parse(noStore, event, expressionLanguage);
    mustRevalidate = parse(mustRevalidate, event, expressionLanguage);
    maxAge = parse(maxAge, event, expressionLanguage);
  }

  private void checkDirective(String directive) {
    if (directive != null && !Arrays.asList(DIRECTIVE).contains(directive)) {
      throw new IllegalArgumentException("Invalid Cache-Control directive: " + directive);
    }
  }

  @Override
  public String toString() {
    StringBuilder cacheControl = new StringBuilder("");
    if (directive != null) {
      cacheControl.append(directive).append(",");
    }
    if (Boolean.valueOf(noCache)) {
      cacheControl.append("no-cache").append(",");
    }
    if (Boolean.valueOf(noStore)) {
      cacheControl.append("no-store").append(",");
    }
    if (Boolean.valueOf(mustRevalidate)) {
      cacheControl.append("must-revalidate").append(",");
    }
    if (maxAge != null) {
      cacheControl.append("max-age=").append(maxAge).append(",");
    }

    String value = cacheControl.toString();
    if (value.endsWith(",")) {
      return value.substring(0, value.length() - 1);
    }
    return value;
  }

  private String parse(String value, Event event, ExpressionLanguage expressionLanguage) {
    if (value != null) {
      return expressionLanguage.parse(value, event, null);
    }
    return value;
  }

  public void setDirective(String directive) {
    this.directive = directive;
  }

  public void setNoCache(String noCache) {
    this.noCache = noCache;
  }

  public void setNoStore(String noStore) {
    this.noStore = noStore;
  }

  public void setMustRevalidate(String mustRevalidate) {
    this.mustRevalidate = mustRevalidate;
  }

  public void setMaxAge(String maxAge) {
    this.maxAge = maxAge;
  }

}
