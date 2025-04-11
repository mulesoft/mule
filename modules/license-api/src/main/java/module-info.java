/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * This module provides an API for license verification.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.license.api {

  exports org.mule.runtime.module.license.api;
  exports org.mule.runtime.module.license.api.exception;

  uses org.mule.runtime.module.license.api.LicenseValidator;

}
