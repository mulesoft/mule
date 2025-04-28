/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.ast.api.validation.Validation.Level.WARN;
import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;

import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.config.internal.validation.test.AbstractCoreValidationTestCase;

import java.util.Optional;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(MULE_DSL)
@Story(DSL_VALIDATION_STORY)
public class InsecureTLSValidationTestCase extends AbstractCoreValidationTestCase {

  @Override
  protected Validation getValidation() {
    return new InsecureTLSValidation();
  }

  @Test
  public void TLSInsecureValidation() {
    final Optional<ValidationResultItem> msg = runValidation("InsecureTLSValidationTestCase#TLSInsecureValidation",
                                                             "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                                 "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
                                                                 "      xmlns:tls=\"http://www.mulesoft.org/schema/mule/tls\"\n" +
                                                                 "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                                                                 +
                                                                 "      xsi:schemaLocation=\"http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\n"
                                                                 +
                                                                 "                           http://www.mulesoft.org/schema/mule/tls http://www.mulesoft.org/schema/mule/tls/current/mule-tls.xsd\">\n"
                                                                 +
                                                                 "\n" +
                                                                 "    <tls:context name=\"TLS_Context\">\n" +
                                                                 "        <tls:trust-store path=\"${truststore.path}\" password=\"${truststore.password}\" type=\"${truststore.type}\" insecure=\"true\"/>\n"
                                                                 +
                                                                 "        <tls:key-store type=\"${keystore.type}\" path=\"${keystore.path}\" alias=\"${keystore.alias}\" keyPassword=\"${keystore.keyPassword}\" password=\"${keystore.password}\" />\n"
                                                                 +
                                                                 "     </tls:context>\n" +
                                                                 "    \n" +
                                                                 "</mule>")
        .stream().findFirst();

    assertThat(msg.get().getValidation().getLevel(), is(WARN));
    assertThat(msg.get().getMessage(),
               containsString("Setting insecure to true renders connections vulnerable to attack. Use it only for prototyping or testing. Never use it in production environments."));
  }
}
