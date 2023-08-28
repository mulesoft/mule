/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.substitutiongroup.extension;

import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.param.Parameter;

@Extension(name = "substitutionGroup")
@Xml(namespace = "http://www.mulesoft.org/schema/mule/sg", prefix = "sg")
@Export(classes = {SomeExportedPojo.class})
public class SubstitutionGroupExtension {

  @Parameter
  private SomePojo somePojo;

  @Parameter
  private MuleSGPojo muleSGPojo;

}
