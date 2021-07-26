/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.security;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.SecurityException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

/**
 * A security provider which holds authentications for multiple users concurrently.
 */
public class TestMultiuserSecurityProvider extends TestSingleUserSecurityProvider {

  private static final Logger LOGGER = getLogger(TestMultiuserSecurityProvider.class);

  private Map<String, Authentication> authentications;

  public TestMultiuserSecurityProvider() {
    super("multi-user-test");
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    authentications = new ConcurrentHashMap<String, Authentication>();
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws SecurityException {
    String user = (String) authentication.getPrincipal();
    LOGGER.debug("Authenticating user: " + user);

    // Check to see if user has already been authenticated
    Authentication oldAuth = authentications.get(user);
    if (oldAuth != null) {
      authentication = oldAuth;
      Map<String, Object> props = new HashMap<>(authentication.getProperties());
      int numberLogins = (Integer) props.get(PROPERTY_NUMBER_LOGINS);
      String favoriteColor = (String) props.get(PROPERTY_FAVORITE_COLOR);
      props.put(PROPERTY_NUMBER_LOGINS, numberLogins + 1);
      authentication = authentication.setProperties(props);
      authentications.put(user, authentication);
      LOGGER.info("Welcome back " + user + " (" + numberLogins + 1 + " logins), we remembered your favorite color: "
          + favoriteColor);
    } else {
      String favoriteColor = getFavoriteColor(user);
      LOGGER.info("First login for user: " + user + ", favorite color is " + favoriteColor);
      Map<String, Object> props = new HashMap<String, Object>();
      props.put(PROPERTY_NUMBER_LOGINS, 1);
      props.put(PROPERTY_FAVORITE_COLOR, favoriteColor);
      authentication = authentication.setProperties(props);
      authentications.put(user, authentication);
    }

    return authentication;
  }
}
