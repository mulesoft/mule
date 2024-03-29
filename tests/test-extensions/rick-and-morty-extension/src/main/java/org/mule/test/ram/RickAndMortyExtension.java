/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.ram;

import static org.mule.runtime.extension.api.soap.WebServiceDefinition.builder;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_11;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_8;

import static java.util.Collections.singletonList;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.extension.api.soap.WebServiceDefinition;
import org.mule.runtime.extension.api.soap.annotation.SoapMessageDispatcherProviders;
import org.mule.sdk.api.annotation.JavaVersionSupport;
import org.mule.sdk.api.annotation.dsl.xml.Xml;
import org.mule.sdk.api.annotation.semantics.connectivity.Endpoint;
import org.mule.sdk.api.annotation.semantics.connectivity.Port;
import org.mule.sdk.api.annotation.semantics.connectivity.Url;

import java.util.List;

@Extension(name = "RAM")
@JavaVersionSupport({JAVA_8, JAVA_11, JAVA_17})
@Xml(prefix = "ram")
@SoapMessageDispatcherProviders({MiniverseDispatcherProvider.class,
    DefaultPortalGunDispatcherProvider.class,
    TestHttpMessageDispatcherProvider.class})
public class RickAndMortyExtension implements SoapServiceProvider {

  public static final String RICKS_PHRASE = "WUBBA LUBBA DUB DUB";

  @Parameter
  @Url
  private String wsdlUrl;

  @Parameter
  @Endpoint
  private String service;

  @Parameter
  @Port
  private String port;

  @Override
  public List<WebServiceDefinition> getWebServiceDefinitions() {
    return singletonList(builder().withId("ram").withWsdlUrl(wsdlUrl).withPort(port).withService(service).build());
  }
}
