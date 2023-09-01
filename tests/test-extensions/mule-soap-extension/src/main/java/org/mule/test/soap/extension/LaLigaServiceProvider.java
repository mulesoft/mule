/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.soap.extension;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.extension.api.soap.WebServiceDefinition;
import org.mule.sdk.api.annotation.semantics.connectivity.Url;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class LaLigaServiceProvider implements SoapServiceProvider {

  public static final String LA_LIGA = "La Liga";
  public static final String LA_LIGA_SERVICE_A = "LaLigaServiceA";
  public static final String LA_LIGA_PORT_A = "LaLigaPortA";

  @Parameter
  private String firstDivision;

  @Parameter
  private String secondDivision;

  @Parameter
  @Url
  private String wsdlLocation;

  public LaLigaServiceProvider() {}

  LaLigaServiceProvider(String wsdlLocation) {
    this.wsdlLocation = wsdlLocation;
    this.firstDivision = "FIRST_DIV";
  }

  @Override
  public List<WebServiceDefinition> getWebServiceDefinitions() {
    return ImmutableList.<WebServiceDefinition>builder().add(getFirstDivisionService()).build();
  }

  WebServiceDefinition getFirstDivisionService() {
    return WebServiceDefinition.builder().withId("A").withFriendlyName(firstDivision).withWsdlUrl(wsdlLocation)
        .withService(LA_LIGA_SERVICE_A).withPort(LA_LIGA_PORT_A).build();
  }

}
