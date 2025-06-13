/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.runtime.config.internal.error.MuleCoreErrorTypeRepository.MULE_CORE_ERROR_TYPE_REPOSITORY;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.util.IdentifierParsingUtils;
import org.mule.runtime.extension.api.loader.parser.ErrorModelParser;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class MuleSdkErrorModelParserTestCase extends AbstractMuleTestCase {

  // see org.mule.runtime.extension.api.error.MuleErrors
  // MULE:SOURCE_RESPONSE_SEND is a child from MULE:SOURCE_RESPONSE
  // MULE:SOURCE_RESPONSE is a child from MULE:SOURCE
  // MULE:SOURCE is a child from MULE:ANY
  private static final ErrorType SOURCE_RESPONSE_SEND =
      MULE_CORE_ERROR_TYPE_REPOSITORY.getErrorType(IdentifierParsingUtils.parseErrorType("SOURCE_RESPONSE_SEND", "MULE")).get();

  @Test
  public void parserFromType() {
    ErrorModelParser sourceResponseSendParser = new MuleSdkErrorModelParser(SOURCE_RESPONSE_SEND);
    assertThat(sourceResponseSendParser.getNamespace(), is("MULE"));
    assertThat(sourceResponseSendParser.getType(), is("SOURCE_RESPONSE_SEND"));
    assertThat(sourceResponseSendParser.getParent().isPresent(), is(true));

    ErrorModelParser sourceResponseParser = sourceResponseSendParser.getParent().get();
    assertThat(sourceResponseParser.getNamespace(), is("MULE"));
    assertThat(sourceResponseParser.getType(), is("SOURCE_RESPONSE"));
    assertThat(sourceResponseParser.getParent().isPresent(), is(true));

    ErrorModelParser sourceParser = sourceResponseParser.getParent().get();
    assertThat(sourceParser.getNamespace(), is("MULE"));
    assertThat(sourceParser.getType(), is("SOURCE"));
    assertThat(sourceParser.getParent().isPresent(), is(true));

    ErrorModelParser anyParser = sourceParser.getParent().get();
    assertThat(anyParser.getNamespace(), is("MULE"));
    assertThat(anyParser.getType(), is("ANY"));
    assertThat(anyParser.getParent().isPresent(), is(false));
  }
}
