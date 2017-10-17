/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config;

/**
 * <code>MuleDeploymentProperties</code> is a set of deployment constants.
 */
public class MuleDeploymentProperties {

  public static final String DEPLOYMENT_PROPERTY_PREFIX = "mule.application.deployment";
  public static final String MULE_MUTE_APP_LOGS_DEPLOYMENT_PROPERTY = DEPLOYMENT_PROPERTY_PREFIX + ".muteLog";
  public static final String MULE_LAZY_INIT_DEPLOYMENT_PROPERTY = DEPLOYMENT_PROPERTY_PREFIX + ".lazyInit";
  public static final String MULE_LAZY_INIT_ENABLE_XML_VALIDATIONS_DEPLOYMENT_PROPERTY =
      MULE_LAZY_INIT_DEPLOYMENT_PROPERTY + ".enableXmlValidations";

}
