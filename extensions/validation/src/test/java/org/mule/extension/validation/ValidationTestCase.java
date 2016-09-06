/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import org.mule.extension.validation.api.ValidationException;
import org.mule.extension.validation.internal.ValidationMessages;
import org.mule.functional.junit4.FlowRunner;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.functional.junit4.runners.ArtifactClassLoaderRunnerConfig;
import org.mule.runtime.core.config.i18n.Message;
import org.mule.runtime.core.exception.MessagingException;

@ArtifactClassLoaderRunnerConfig(exportClasses = {ValidationMessages.class})
abstract class ValidationTestCase extends MuleArtifactFunctionalTestCase {

  static final String VALID_URL = "http://localhost:8080";
  static final String INVALID_URL = "here";

  static final String VALID_EMAIL = "mariano.gonzalez@mulesoft.com";
  static final String INVALID_EMAIL = "@mulesoft.com";

  protected ValidationMessages messages;


  @Override
  protected void doSetUp() throws Exception {
    messages = new ValidationMessages();
  }

  protected void assertValid(FlowRunner runner) throws Exception {
    assertThat(!runner.run().getError().isPresent(), is(true));
    runner.reset();
  }

  protected void assertInvalid(FlowRunner runner, Message expectedMessage) throws Exception {
    Exception e = runner.runExpectingException();
    assertThat(e, is(instanceOf(MessagingException.class)));
    assertThat(e.getCause(), is(instanceOf(ValidationException.class)));
    assertThat(e.getCause().getMessage(), is(expectedMessage.getMessage()));
    // assert that all placeholders were replaced in message
    assertThat(e.getMessage(), not(containsString("${")));
    runner.reset();
  }
}
