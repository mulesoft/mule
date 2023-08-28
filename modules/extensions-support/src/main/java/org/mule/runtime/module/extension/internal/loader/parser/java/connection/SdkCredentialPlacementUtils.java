/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.connection;

import org.mule.runtime.extension.api.security.CredentialsPlacement;

/**
 * Helper class to handle credential placement.
 *
 * @since 4.5.0
 */
public class SdkCredentialPlacementUtils {

  /**
   * @param credentialsPlacement the credential placement to translate to the extensions-api enum.
   * @return the corresponding credential placement from the extensions-api that derives the given one.
   */
  public static CredentialsPlacement from(org.mule.sdk.api.security.CredentialsPlacement credentialsPlacement) {
    switch (credentialsPlacement) {
      case BODY:
        return CredentialsPlacement.BODY;
      case QUERY_PARAMS:
        return CredentialsPlacement.QUERY_PARAMS;
      case BASIC_AUTH_HEADER:
        return CredentialsPlacement.BASIC_AUTH_HEADER;
      default:
        return CredentialsPlacement.BODY;
    }
  }

}
