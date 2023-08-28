/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.ram;

import static java.util.Collections.singletonList;
import static org.mule.runtime.extension.api.soap.WebServiceDefinition.builder;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.extension.api.soap.WebServiceDefinition;
import org.mule.runtime.extension.api.soap.annotation.SoapMessageDispatcherProviders;
import org.mule.sdk.api.annotation.dsl.xml.Xml;
import org.mule.sdk.api.annotation.semantics.connectivity.Endpoint;
import org.mule.sdk.api.annotation.semantics.connectivity.Port;
import org.mule.sdk.api.annotation.semantics.connectivity.Url;

import java.util.List;

@Extension(name = "RAM")
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
