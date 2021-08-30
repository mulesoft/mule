/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

