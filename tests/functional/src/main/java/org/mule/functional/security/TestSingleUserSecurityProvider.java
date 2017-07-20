/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.security;

import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.core.api.security.AbstractSecurityProvider;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A security provider which only authenticates a single user at a time (i.e., authentication of a new user overwrites the
 * previous authentication).
 */
public class TestSingleUserSecurityProvider extends AbstractSecurityProvider {

  public static final String PROPERTY_FAVORITE_COLOR = "FAVORITE_COLOR";
  public static final String PROPERTY_NUMBER_LOGINS = "NUMBER_LOGINS";

  private Authentication authentication;

  private static final Logger LOGGER = LoggerFactory.getLogger(TestSingleUserSecurityProvider.class);

  public TestSingleUserSecurityProvider() {
    super("single-user-test");
  }

  public TestSingleUserSecurityProvider(String name) {
    super(name);
  }

  @Override
  public Authentication authenticate(Authentication authenticationRequest) throws SecurityException {
    String user = (String) authenticationRequest.getPrincipal();
    LOGGER.debug("Authenticating user: " + user);

    // Check to see if user has already been authenticated
    if (authentication != null) {
      Map<String, Object> props = authentication.getProperties();
      int numberLogins = (Integer) props.get(PROPERTY_NUMBER_LOGINS);
      String favoriteColor = (String) props.get(PROPERTY_FAVORITE_COLOR);
      props.put(PROPERTY_NUMBER_LOGINS, numberLogins + 1);
      authentication = authentication.setProperties(props);
      LOGGER.info("Welcome back " + user + " (" + numberLogins + 1 + " logins), we remembered your favorite color: "
          + favoriteColor);
    } else {
      String favoriteColor = getFavoriteColor(user);
      LOGGER.info("First login for user: " + user + ", favorite color is " + favoriteColor);
      Map<String, Object> props = new HashMap<String, Object>();
      props.put(PROPERTY_NUMBER_LOGINS, 1);
      props.put(PROPERTY_FAVORITE_COLOR, favoriteColor);
      authentication = authenticationRequest.setProperties(props);
    }

    return authentication;
  }

  // This info. would be stored in an LDAP or RDBMS
  String getFavoriteColor(String user) {
    if (user.equals("marie"))
      return "bright red";
    else if (user.equals("stan"))
      return "metallic blue";
    else if (user.equals("cindy"))
      return "dark violet";
    else
      return null;
  }
}
