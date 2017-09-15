/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.execution;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.internal.execution.MessageProcessPhase;
import org.mule.runtime.core.privileged.execution.MessageProcessTemplate;

import org.mockito.Mockito;

public class PhaseSupportTestHelper<T> {

  private final Class<T> supportedTemplateClass;
  private final T supportedTemplate;
  private final MessageProcessTemplate notSupportedTemplate;

  public PhaseSupportTestHelper(Class<T> supportedTemplate) {
    this.supportedTemplateClass = supportedTemplate;
    this.supportedTemplate = Mockito.mock(this.supportedTemplateClass);
    this.notSupportedTemplate = Mockito.mock(MessageProcessTemplate.class);
  }

  public void testSupportTemplates(MessageProcessPhase messageProcessPhase) {
    notSupportedTemplateTest(messageProcessPhase);
    supportedTemplateTest(messageProcessPhase);
  }

  public void notSupportedTemplateTest(MessageProcessPhase messageProcessPhase) {
    assertThat(messageProcessPhase.supportsTemplate(notSupportedTemplate), is(false));
  }

  public void supportedTemplateTest(MessageProcessPhase messageProcessPhase) {
    assertThat(messageProcessPhase.supportsTemplate((MessageProcessTemplate) supportedTemplate), is(true));
  }

}
