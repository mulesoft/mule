/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.soap.extension;

import static java.util.Collections.singletonList;
import static org.mule.test.soap.extension.CalcioServiceProvider.CALCIO_DESC;
import static org.mule.test.soap.extension.CalcioServiceProvider.CALCIO_ID;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.extension.api.soap.WebServiceDefinition;

import java.util.List;

@Alias(value = CALCIO_ID, description = CALCIO_DESC)
public class CalcioServiceProvider implements SoapServiceProvider {

  public static final String CALCIO_ID = "higuain-gp";
  public static final String CALCIO_DESC = "This a nice description about the league where higuain is a star *";
  public static final String CALCIO_FRIENDLY_NAME = "Calcio";
  public static final String CALCIO_URL = "http://www.higuain-no-mereces-nada.com/nada";

  @Override
  public List<WebServiceDefinition> getWebServiceDefinitions() {
    return singletonList(getFirstDivisionService());
  }

  private WebServiceDefinition getFirstDivisionService() {
    return WebServiceDefinition.builder()
        .withService(CALCIO_ID)
        .withFriendlyName(CALCIO_FRIENDLY_NAME)
        .withWsdlUrl(CALCIO_URL)
        .withService("ServiceA")
        .withPort("PortA")
        .build();
  }
}
