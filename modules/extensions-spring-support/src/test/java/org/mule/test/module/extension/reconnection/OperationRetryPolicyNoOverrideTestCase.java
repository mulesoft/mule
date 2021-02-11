/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.reconnection;

import static org.mule.runtime.api.util.MuleSystemProperties.HONOUR_OPERATION_RETRY_POLICY_TEMPLATE_OVERRIDE_PROPERTY;
import static org.mule.test.allure.AllureConstants.ReconnectionPolicyFeature.RECONNECTION_POLICIES;
import static org.mule.test.allure.AllureConstants.ReconnectionPolicyFeature.RetryTemplateStory.RETRY_TEMPLATE;

import org.junit.Rule;
import org.mule.tck.junit4.rule.SystemProperty;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Issue("MULE-19160")
@Feature(RECONNECTION_POLICIES)
@Story(RETRY_TEMPLATE)
public class OperationRetryPolicyNoOverrideTestCase extends AbstractReconnectionTestCase {

  @Rule
  public SystemProperty muleOperationRetryPolicyTemplateOverrideProperty =
      new SystemProperty(HONOUR_OPERATION_RETRY_POLICY_TEMPLATE_OVERRIDE_PROPERTY, "false");

  @Override
  protected boolean expectedAsyncWhenOperationBlockingRetryPolicyIsOverridden() {
    return true;
  }

}
