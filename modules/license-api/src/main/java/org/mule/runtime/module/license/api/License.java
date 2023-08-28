/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.license.api;

import java.util.Date;

public interface License {

  Date getExpirationDate();

  boolean isEvaluation();

}
