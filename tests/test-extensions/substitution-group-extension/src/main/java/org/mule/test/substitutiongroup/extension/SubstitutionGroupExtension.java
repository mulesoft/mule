/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.substitutiongroup.extension;

import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_21;

import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.JavaVersionSupport;

@Extension(name = "substitutionGroup")
@JavaVersionSupport({JAVA_21, JAVA_17})
@Xml(namespace = "http://www.mulesoft.org/schema/mule/sg", prefix = "sg")
@Export(classes = {SomeExportedPojo.class})
public class SubstitutionGroupExtension {

  @Parameter
  private SomePojo somePojo;

  @Parameter
  private MuleSGPojo muleSGPojo;

}
