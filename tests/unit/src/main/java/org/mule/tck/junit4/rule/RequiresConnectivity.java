/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.rule;

import static org.junit.Assume.assumeTrue;

import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.net.URL;

/**
 * Checks that a connection can be established to a provided Url. If not, the test case is ignored.
 *
 * @since 4.1
 */
public class RequiresConnectivity extends ExternalResource {

  private String connectivityUrl;

  /**
   * Constructs a JUnit Rule to assume connectivity to a provided url.
   *
   * @param connectivityUrl the url to check connectivity to.
   */
  public RequiresConnectivity(String connectivityUrl) {
    this.connectivityUrl = connectivityUrl;
  }

  @Override
  protected void before() throws Throwable {
    super.before();

    try {
      new URL(connectivityUrl).openConnection().getContent();
    } catch (IOException e) {
      assumeTrue("No connectivity to " + connectivityUrl + ". Ignoring test.", false);
    }
  }
}
