/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.substitutiongroup.extension;

import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Parameter;

@XmlHints(substitutionGroup = "heisenberg:some-component", allowTopLevelDefinition = true)
public class SomeExportedPojo {

  @Parameter
  private boolean attribute;

}
