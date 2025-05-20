/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.semantic.extension;

import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_21;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.sdk.api.annotation.JavaVersionSupport;
import org.mule.test.semantic.extension.connection.ApiKeySemanticConnectionProvider;
import org.mule.test.semantic.extension.connection.BasicAuthSemanticConnectionProvider;
import org.mule.test.semantic.extension.connection.ClientSecretSemanticConnectionProvider;
import org.mule.test.semantic.extension.connection.CustomAuthSemanticConnectionProvider;
import org.mule.test.semantic.extension.connection.DigestSemanticConnectionProvider;
import org.mule.test.semantic.extension.connection.KerberosSemanticConnectionProvider;
import org.mule.test.semantic.extension.connection.UnsecuredSemanticConnectionProvider;

@Extension(name = "Semantic Terms")
@JavaVersionSupport({JAVA_21, JAVA_17})
@Operations(SemanticTermsOperations.class)
@ConnectionProviders({BasicAuthSemanticConnectionProvider.class, ApiKeySemanticConnectionProvider.class,
    ClientSecretSemanticConnectionProvider.class, CustomAuthSemanticConnectionProvider.class,
    DigestSemanticConnectionProvider.class, KerberosSemanticConnectionProvider.class,
    UnsecuredSemanticConnectionProvider.class})
@Sources(SemanticTermsSource.class)
public class SemanticTermsExtension {

}
