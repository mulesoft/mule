/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.semantic.extension.connection;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.sdk.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.semantics.connectivity.KerberosAuth;
import org.mule.sdk.api.annotation.semantics.connectivity.NtlmDomain;

@KerberosAuth
@Alias("kerberos")
public class KerberosSemanticConnectionProvider extends SemanticTermsConnectionProvider {

  @Parameter
  @NtlmDomain
  private String ntlmDomain;
}

