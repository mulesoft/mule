/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
