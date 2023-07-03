/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.Validation.Level.WARN;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;
import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.io.File;
import java.util.Optional;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(MULE_DSL)
@Story(DSL_VALIDATION_STORY)
public class ParseTemplateResourceExistTestCase extends AbstractCoreValidationTestCase {

  @Override
  protected Validation getValidation() {
    return new ParseTemplateResourceExist(Thread.currentThread().getContextClassLoader(), false);
  }

  @Test
  public void unexistentLocation() {
    final Optional<ValidationResultItem> msg = runValidation("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
        "xsi:schemaLocation=\"http://www.mulesoft.org/schema/mule/core " +
        "http://www.mulesoft.org/schema/mule/core/current/mule.xsd\" >\n" +
        "<flow name=\"test-projectFlow\" >\n" +
        "<parse-template outputMimeType=\"text/html\" location=\"unexistent.txt\" />\n" +
        "</flow>\n" +
        "</mule>")
            .stream().findFirst();

    assertThat(msg.get().getValidation().getLevel(), is(ERROR));
    assertThat(msg.get().getMessage(),
               containsString("Template location: 'unexistent.txt' not found"));
  }

  @Test
  public void locationInResourcesFolder() {
    final Optional<ValidationResultItem> msg = runValidation("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
        "xsi:schemaLocation=\"http://www.mulesoft.org/schema/mule/core " +
        "http://www.mulesoft.org/schema/mule/core/current/mule.xsd\" >\n" +
        "<flow name=\"test-projectFlow\" >\n" +
        "<parse-template outputMimeType=\"text/html\" location=\"template.txt\" />\n" +
        "</flow>\n" +
        "</mule>")
            .stream().findFirst();

    // No errors
    assertThat(msg.isPresent(), is(false));
  }

  @Test
  public void locationWithAbsolutePath() {
    File templateFile = new File("template.txt");
    String absolutePath = templateFile.getAbsolutePath();

    final Optional<ValidationResultItem> msg = runValidation("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
        "xsi:schemaLocation=\"http://www.mulesoft.org/schema/mule/core " +
        "http://www.mulesoft.org/schema/mule/core/current/mule.xsd\" >\n" +
        "<flow name=\"test-projectFlow\" >\n" +
        "<parse-template outputMimeType=\"text/html\" location=\"" + absolutePath + "\" />\n" +
        "</flow>\n" +
        "</mule>")
            .stream().findFirst();

    // No errors
    assertThat(msg.isPresent(), is(false));
  }
}
