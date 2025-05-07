/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.mule.runtime.api.component.Component.Annotations.NAME_ANNOTATION_KEY;
import static org.mule.runtime.config.internal.dsl.spring.BeanDefinitionFactory.resolveProcessorSourceLocation;
import static org.mule.test.allure.AllureConstants.Logging.LOGGING;
import static org.mule.test.allure.AllureConstants.Logging.LoggingStory.FLOW_STACK;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.ast.internal.DefaultComponentMetadataAst;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(LOGGING)
@Story(FLOW_STACK)
public class BeanDefinitionFactoryRepresentationSourceLocationTestCase extends AbstractMuleTestCase {

  @Test
  public void withFailingProcessorNoPathResolver() {
    assertThat(resolveProcessorSourceLocation(DefaultComponentMetadataAst.builder()
                                                  .setFileName("unknown")
                                                  .build()),
               is("unknown:-1"));
  }

  @Test
  public void withFailingProcessorPathResolver() {
    assertThat(resolveProcessorSourceLocation(DefaultComponentMetadataAst.builder()
                                                  .setFileName("muleApp.xml")
                                                  .setStartLine(10)
                                                  .build()),
               is("muleApp.xml:10"));
  }

  @Test
  public void withFailingProcessorNotPathResolver() {
    assertThat(resolveProcessorSourceLocation(DefaultComponentMetadataAst.builder()
                                                  .setFileName("unknown")
                                                  .build()),
               is("unknown:-1"));
  }

  @Test
  public void withAnnotatedFailingProcessorNoPathResolver() {
    assertThat(resolveProcessorSourceLocation(DefaultComponentMetadataAst.builder()
                                                  .setFileName("muleApp.xml")
                                                  .setStartLine(10)
                                                  .putDocAttribute(NAME_ANNOTATION_KEY.getLocalPart(), "Mock Component")
                                                  .build()),
               is("muleApp.xml:10"));
  }

  @Test
  public void withAnnotatedFailingProcessorPathResolver() {
    assertThat(resolveProcessorSourceLocation(DefaultComponentMetadataAst.builder()
                                                  .setFileName("muleApp.xml")
                                                  .setStartLine(10)
                                                  .putDocAttribute(NAME_ANNOTATION_KEY.getLocalPart(), "Mock Component")
                                                  .build()),
               is("muleApp.xml:10"));
  }

  @Test
  public void withAnnotatedFailingProcessorNotPathResolver() {
    assertThat(resolveProcessorSourceLocation(DefaultComponentMetadataAst.builder()
                                                  .setFileName("muleApp.xml")
                                                  .setStartLine(10)
                                                  .putDocAttribute(NAME_ANNOTATION_KEY.getLocalPart(), "Mock Component")
                                                  .build()),
               is("muleApp.xml:10"));
  }

}
