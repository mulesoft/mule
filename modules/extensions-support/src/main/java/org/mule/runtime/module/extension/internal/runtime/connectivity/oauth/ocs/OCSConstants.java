/*
 * (c) 2003-2020 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

  private OCSConstants() {
  }
}
