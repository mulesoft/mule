/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.soap.services;

import static java.util.Arrays.asList;
import static org.mule.test.soap.extension.CalcioServiceProvider.CALCIO_FRIENDLY_NAME;
import static org.mule.test.soap.extension.FootballSoapExtension.LEAGUES_PORT;
import static org.mule.test.soap.extension.FootballSoapExtension.LEAGUES_SERVICE;
import static org.mule.test.soap.extension.LaLigaServiceProvider.LA_LIGA;
import org.mule.services.soap.service.EchoException;

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

@WebService(portName = LEAGUES_PORT, serviceName = LEAGUES_SERVICE)
public class FootballService {

  private LaLigaService delegate = new LaLigaService();

  public static final String AUTH = "Authorized";
  public final static String JULITO_MIN_DESC = "Julio Humberto Grondona";
  public final static String JULITO_FULL_DESC = JULITO_MIN_DESC + "(September 18, 1931 - July 30, 2014), Corrupt Argentinian";

  @WebResult(name = "text")
  @WebMethod(action = "echo")
  public String getPresidentInfo(@WebParam(name = "fullInfo") boolean fullInfo) {
    if (fullInfo) {
      return JULITO_FULL_DESC;
    }
    return JULITO_MIN_DESC;
  }

  @WebResult(name = "team")
  @WebMethod(action = "getLeagueTeams")
  public List<String> getLeagueTeams(@WebParam(name = "auth", header = true) String headerIn,
                                     @WebParam(name = "name") String leagueName)
      throws EchoException {

    if (!headerIn.equals(AUTH)) {
      throw new EchoException("Missing Required Authorization Headers");
    }

    if (!leagueName.equals(LA_LIGA)) {
      throw new EchoException("No teams for league: " + leagueName);
    }

    return delegate.getTeams();
  }

  @WebResult(name = "league")
  @WebMethod(action = "getLeagues")
  public List<String> getLeagues() {
    return asList(CALCIO_FRIENDLY_NAME, LA_LIGA);
  }
}
