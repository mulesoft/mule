/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.soap.extension;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.extension.api.soap.WebServiceDefinition;
import org.mule.runtime.extension.api.soap.annotation.Soap;
import org.mule.runtime.extension.api.soap.message.MessageDispatcher;

import java.util.List;

@Alias("base")
@Soap({LaLigaServiceProvider.class, CalcioServiceProvider.class})
@Extension(name = "soap", description = "Soap Connect Test Extension")
public class FootballSoapExtension implements SoapServiceProvider {

  public static final String LEAGUES_ID = "leagues";
  public static final String LEAGUES_FRIENDLY_NAME = "Football Leagues";
  public static final String LEAGUES_SERVICE = "LeaguesService";
  public static final String LEAGUES_PORT = "LeaguesPort";
  public static final String TEST_SERVICE_URL = "http://some-url.com";

  @Parameter
  @Optional(defaultValue = TEST_SERVICE_URL)
  private String leaguesAddress;

  @Parameter
  @Optional
  private boolean useCustomTransport;

  @Override
  public List<WebServiceDefinition> getWebServiceDefinitions() {
    return singletonList(WebServiceDefinition.builder().withId(LEAGUES_ID).withFriendlyName(LEAGUES_FRIENDLY_NAME)
        .withWsdlUrl(leaguesAddress + "?wsdl").withAddress(leaguesAddress)
        .withService(LEAGUES_SERVICE).withPort(LEAGUES_PORT).withExcludedOperations(emptyList())
        .build());
  }

  @Override
  public java.util.Optional<MessageDispatcher> getCustomDispatcher() {
    return useCustomTransport ? of(new FootballTestDispatcher()) : empty();
  }
}
