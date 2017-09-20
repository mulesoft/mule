/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.DefaultEncoding;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.List;

@TypeDsl(allowTopLevelDefinition = true)
public class PhoneNumber {

  @Parameter
  private String mobile;

  @Parameter
  private String home;

  @DefaultEncoding
  private String countryEncoding;

  @Parameter
  private List<String> areaCodes;

  public String getMobile() {
    return mobile;
  }

  public String getHome() {
    return home;
  }

  public String getCountryEncoding() {
    return countryEncoding;
  }

  public List<String> getAreaCodes() {
    return areaCodes;
  }
}
