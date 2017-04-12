/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.email.retriever;

import static org.mule.extension.email.util.EmailTestUtils.JUANI_EMAIL;

import org.mule.extension.email.EmailConnectorTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.util.TestConnectivityUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class IMAPSpecialCharactersTestCase extends EmailConnectorTestCase {

  private TestConnectivityUtils connectivityUtils;

  @Rule
  public SystemProperty rule = TestConnectivityUtils.disableAutomaticTestConnectivity();

  private static final String SPECIAL_CHARACTER_PASSWORD = "*uawH*IDXlh2p%21xSPOx%23%25zLpL";

  @Rule
  public SystemProperty specialCharacterPassword = new SystemProperty("specialCharacterPassword", SPECIAL_CHARACTER_PASSWORD);

  public IMAPSpecialCharactersTestCase() {
    super(JUANI_EMAIL, SPECIAL_CHARACTER_PASSWORD);
  }

  @Override
  protected String getConfigFile() {
    return "retriever/imap-special-characters-password.xml";
  }

  @Override
  public String getProtocol() {
    return "imap";
  }

  @Before
  public void createUtils() {
    connectivityUtils = new TestConnectivityUtils(muleContext);
  }

  @Test
  public void configSpecialCharacterCredentials() {
    connectivityUtils.assertSuccessConnection("configSpecialCharacterCredentials");
  }

}
