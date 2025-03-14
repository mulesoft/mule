/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.javaxinject;

import static org.mule.sdk.api.meta.JavaVersion.JAVA_11;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_21;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_8;

import org.mule.runtime.api.config.ArtifactEncoding;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.sdk.api.annotation.Configurations;
import org.mule.sdk.api.annotation.ExpressionFunctions;
import org.mule.sdk.api.annotation.JavaVersionSupport;
import org.mule.sdk.api.annotation.dsl.xml.Xml;

import javax.inject.Inject;

@Extension(name = JavaxInjectCompatibilityTestExtension.JAVAX_INJECT_COMPATIBILITY_TEST_EXTENSION)
@Xml(prefix = "javax-inject",
    namespace = "http://www.mulesoft.org/schema/mule/javax-inject")
@JavaVersionSupport({JAVA_8, JAVA_11, JAVA_17, JAVA_21})

@Configurations(JavaxInjectCompatibilityTestConfiguration.class)
@ExpressionFunctions(JavaxInjectCompatibilityTestFunction.class)
public class JavaxInjectCompatibilityTestExtension {

  public static final String JAVAX_INJECT_COMPATIBILITY_TEST_EXTENSION = "Javax Inject compatibility Test Extension";

  @Inject
  private ArtifactEncoding encoding;

  public ArtifactEncoding getEncoding() {
    return encoding;
  }
}
