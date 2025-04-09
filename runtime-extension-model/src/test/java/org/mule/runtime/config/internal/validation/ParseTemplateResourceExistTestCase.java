/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;

import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.config.internal.validation.test.AbstractCoreValidationTestCase;

import java.io.File;
import java.util.Optional;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(MULE_DSL)
@Story(DSL_VALIDATION_STORY)
public class ParseTemplateResourceExistTestCase extends AbstractCoreValidationTestCase {

  @Override
  protected Validation getValidation() {
    return new ParseTemplateResourceExist(Thread.currentThread().getContextClassLoader(), false);
  }

  @Test
  public void unexistentLocation() {
    final Optional<ValidationResultItem> msg = runValidation("ParseTemplateResourceExistTestCase#unexistentLocation",
                                                             "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                                 "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
                                                                 "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
                                                                 "xsi:schemaLocation=\"http://www.mulesoft.org/schema/mule/core "
                                                                 +
                                                                 "http://www.mulesoft.org/schema/mule/core/current/mule.xsd\" >\n"
                                                                 +
                                                                 "<flow name=\"test-projectFlow\" >\n" +
                                                                 "<parse-template outputMimeType=\"text/html\" location=\"unexistent.txt\" />\n"
                                                                 +
                                                                 "</flow>\n" +
                                                                 "</mule>")
        .stream().findFirst();

    assertThat(msg.get().getValidation().getLevel(), is(ERROR));
    assertThat(msg.get().getMessage(),
               containsString("Template location: 'unexistent.txt' not found"));
  }

  @Test
  public void locationInResourcesFolder() {
    final Optional<ValidationResultItem> msg = runValidation("ParseTemplateResourceExistTestCase#locationInResourcesFolder",
                                                             "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                                 "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
                                                                 "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
                                                                 "xsi:schemaLocation=\"http://www.mulesoft.org/schema/mule/core "
                                                                 +
                                                                 "http://www.mulesoft.org/schema/mule/core/current/mule.xsd\" >\n"
                                                                 +
                                                                 "<flow name=\"test-projectFlow\" >\n" +
                                                                 "<parse-template outputMimeType=\"text/html\" location=\"template.txt\" />\n"
                                                                 +
                                                                 "</flow>\n" +
                                                                 "</mule>")
        .stream().findFirst();

    // No errors
    assertThat(msg.isPresent(), is(false));
  }

  @Test
  @Issue("W-13682699")
  public void locationWithAbsolutePath() {
    File templateFile = new File("test-classes/template.txt");
    assertThat(templateFile.exists(), is(true));
    String absolutePath = templateFile.getAbsolutePath();

    final Optional<ValidationResultItem> msg = runValidation("ParseTemplateResourceExistTestCase#locationWithAbsolutePath",
                                                             "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                                 "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
                                                                 "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
                                                                 "xsi:schemaLocation=\"http://www.mulesoft.org/schema/mule/core "
                                                                 +
                                                                 "http://www.mulesoft.org/schema/mule/core/current/mule.xsd\" >\n"
                                                                 +
                                                                 "<flow name=\"test-projectFlow\" >\n" +
                                                                 "<parse-template outputMimeType=\"text/html\" location=\""
                                                                 + absolutePath + "\" />\n" +
                                                                 "</flow>\n" +
                                                                 "</mule>")
        .stream().findFirst();

    // No errors
    assertThat(msg.isPresent(), is(false));
  }
}
