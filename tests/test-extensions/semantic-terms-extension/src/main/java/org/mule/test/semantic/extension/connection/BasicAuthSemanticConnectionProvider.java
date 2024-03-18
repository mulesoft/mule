/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.semantic.extension.connection;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.semantics.connectivity.BasicAuth;
import org.mule.sdk.api.annotation.semantics.security.Password;
import org.mule.sdk.api.annotation.semantics.security.Username;

@BasicAuth
@Alias("basic-auth")
public class BasicAuthSemanticConnectionProvider extends SemanticTermsConnectionProvider {

  @Parameter
  @Username
  private String username;

  @Parameter
  @Password
  private String password;

}
