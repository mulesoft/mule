/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.select;

import org.mule.extension.db.integration.AbstractQueryTimeoutTestCase;

public class SelectTimeoutTestCase extends AbstractQueryTimeoutTestCase {

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/select/select-timeout-config.xml"};
  }
}
