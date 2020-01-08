/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ocs;

import org.mule.runtime.api.component.ConfigurationProperties;

/**
 * Constants for the OAuth Client Service functionality.
 * <p>
 * These are used to extract configuration arguments from the application's {@link ConfigurationProperties} object:
 * he following properties will be fetch:
 *
 *  <ul>
 *  <li>{@link OCSConstants#OCS_SERVICE_URL}</li>
 *  <li>{@link OCSConstants#OCS_PLATFORM_AUTH_URL}</li>
 *  <li>{@link OCSConstants#OCS_CLIENT_ID}</li>
 *  <li>{@link OCSConstants#OCS_CLIENT_SECRET}</li>
 *  <li>{@link OCSConstants#OCS_ORG_ID}</li>
 *  </ul>
 * <p>
 * If any of these properties are missing, {@link IllegalStateException} will be thrown when using this client.
 * Because this feature is at the moment experimental, the presence of these properties also act as a feature flag. If any is
 * absent, the feature will not work
 *
 * @since 4.3.0
 */
public final class OCSConstants {

  /**
   * Key to obtain the URL of the OCS API
   */
  public static final String OCS_SERVICE_URL = "ocsServiceUrl";

  /**
   * PlatformManagedDancerTestCase.setupServices
   * Key to obtain the URL of the OAuth service provider that grants access tokens to the OCS API
   */
  public static final String OCS_PLATFORM_AUTH_URL = "ocsPlatformAuthenticationUrl";

  /**
   * Key to obtain the client id to obtain an access token for the OCS API
   */
  public static final String OCS_CLIENT_ID = "ocsServiceClientId";

  /**
   * Key to obtain the client secret to obtain an access token to the OCS API
   */
  public static final String OCS_CLIENT_SECRET = "ocsServiceClientSecret";

  /**
   * Key to obtain the organization id to be used when accessing the the OCS API
   */
  public static final String OCS_ORG_ID = "orgId";

  private OCSConstants() {}
}
