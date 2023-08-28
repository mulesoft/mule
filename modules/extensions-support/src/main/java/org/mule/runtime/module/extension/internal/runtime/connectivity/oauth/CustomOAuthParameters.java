/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import org.mule.runtime.api.util.MultiMap;

/**
 * Holds custom OAuth parameters classified by their placement
 *
 * @since 4.5.0
 */
public class CustomOAuthParameters {

  private final MultiMap<String, String> queryParams = new MultiMap<>();
  private final MultiMap<String, String> headers = new MultiMap<>();
  private final MultiMap<String, String> bodyParams = new MultiMap<>();

  public MultiMap<String, String> getQueryParams() {
    return queryParams;
  }

  public MultiMap<String, String> getHeaders() {
    return headers;
  }

  public MultiMap<String, String> getBodyParams() {
    return bodyParams;
  }
}
