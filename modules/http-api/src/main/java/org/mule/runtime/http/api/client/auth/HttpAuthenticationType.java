/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.client.auth;

/**
 * Supported HTTP authentication schemes.
 *
 * @since 4.0
 */
public enum HttpAuthenticationType {

  BASIC,

  DIGEST,

  NTLM;
}
