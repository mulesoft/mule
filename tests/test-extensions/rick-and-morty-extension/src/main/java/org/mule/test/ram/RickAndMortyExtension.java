/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.ram;

import static java.util.Collections.singletonList;
import static org.mule.runtime.extension.api.soap.WebServiceDefinition.builder;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.soap.HttpMessageDispatcherProvider;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.extension.api.soap.WebServiceDefinition;
import org.mule.runtime.extension.api.soap.annotation.SoapMessageDispatcherProviders;

import java.util.List;

@Extension(name = "RAM")
@Xml(prefix = "ram")
@SoapMessageDispatcherProviders({MiniverseDispatcherProvider.class,
    DefaultPortalGunDispatcherProvider.class,
    HttpMessageDispatcherProvider.class})
public class RickAndMortyExtension implements SoapServiceProvider {

  public static final String RICKS_PHRASE = "WUBBA LUBBA DUB DUB";

  @Parameter
  private String wsdlUrl;

  @Parameter
  private String service;

  @Parameter
  private String port;

  @Override
  public List<WebServiceDefinition> getWebServiceDefinitions() {
    return singletonList(builder().withId("ram").withWsdlUrl(wsdlUrl).withPort(port).withService(service).build());
  }
}
