/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.soap.extension;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.extension.api.soap.WebServiceDefinition;
import org.mule.runtime.extension.api.soap.annotation.Soap;

import java.net.MalformedURLException;
import java.net.URL;
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

  @Override
  public List<WebServiceDefinition> getWebServiceDefinitions() {
    try {
      return singletonList(WebServiceDefinition.builder().withId(LEAGUES_ID).withFriendlyName(LEAGUES_FRIENDLY_NAME)
          .withWsdlUrl(new URL(leaguesAddress + "?wsdl")).withAddress(new URL(leaguesAddress))
          .withService(LEAGUES_SERVICE).withPort(LEAGUES_PORT).withExcludedOperations(emptyList())
          .build());
    } catch (MalformedURLException e) {
      throw new RuntimeException("Error Parsing Leagues Address", e);
    }
  }

}
