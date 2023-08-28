/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.soap.extension;

import static java.util.Collections.emptyList;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.connectivity.NoConnectivityTest;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.extension.api.soap.WebServiceDefinition;
import org.mule.runtime.extension.api.soap.annotation.Soap;

import java.util.Arrays;
import java.util.List;

@Alias("base")
@Soap({LaLigaServiceProvider.class, CalcioServiceProvider.class})
@Extension(name = "soap")
public class FootballSoapExtension implements SoapServiceProvider, NoConnectivityTest {

  public static final String LEAGUES_ID = "leagues";
  public static final String LEAGUES_FRIENDLY_NAME = "Football Leagues";
  public static final String LEAGUES_SERVICE = "LeaguesService";
  public static final String LEAGUES_PORT = "LeaguesPort";
  public static final String TEST_SERVICE_URL = "http://some-url.com";

  @Parameter
  @Optional(defaultValue = TEST_SERVICE_URL)
  private String leaguesAddress;

  @Parameter
  private String laLigaAddress;

  @Override
  public List<WebServiceDefinition> getWebServiceDefinitions() {
    return Arrays.asList(getLeaguesService(), getLaLigaService());
  }

  private WebServiceDefinition getLaLigaService() {
    return new LaLigaServiceProvider(laLigaAddress).getFirstDivisionService();
  }

  private WebServiceDefinition getLeaguesService() {
    return WebServiceDefinition.builder().withId(LEAGUES_ID).withFriendlyName(LEAGUES_FRIENDLY_NAME)
        .withWsdlUrl(leaguesAddress + "?wsdl").withAddress(leaguesAddress)
        .withService(LEAGUES_SERVICE).withPort(LEAGUES_PORT).withExcludedOperations(emptyList())
        .build();
  }

}
