/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.Component.Annotations.NAME_ANNOTATION_KEY;
import static org.mule.runtime.config.internal.dsl.spring.BeanDefinitionFactory.resolveProcessorRepresentation;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;

import org.mule.runtime.ast.api.ComponentMetadataAst;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class BeanDefinitionFactoryRepresentationTestCase extends AbstractMuleTestCase {

  @Test
  public void withFailingProcessorNoPathResolver() {
    assertThat(resolveProcessorRepresentation("BeanDefinitionFactoryRepresentationTestCase", fromSingleComponent("Mock@1"),
                                              ComponentMetadataAst.builder()
                                                  .setFileName("unknown")
                                                  .build()),
               is("Mock@1 @ BeanDefinitionFactoryRepresentationTestCase:unknown:-1"));
  }

  @Test
  public void withFailingProcessorPathResolver() {
    assertThat(resolveProcessorRepresentation("BeanDefinitionFactoryRepresentationTestCase",
                                              fromSingleComponent("flow/processor"),
                                              ComponentMetadataAst.builder()
                                                  .setFileName("muleApp.xml")
                                                  .setStartLine(10)
                                                  .build()),
               is("flow/processor @ BeanDefinitionFactoryRepresentationTestCase:muleApp.xml:10"));
  }

  @Test
  public void withFailingProcessorNotPathResolver() {
    assertThat(resolveProcessorRepresentation("BeanDefinitionFactoryRepresentationTestCase", fromSingleComponent("Mock@1"),
                                              ComponentMetadataAst.builder()
                                                  .setFileName("unknown")
                                                  .build()),
               is("Mock@1 @ BeanDefinitionFactoryRepresentationTestCase:unknown:-1"));
  }

  @Test
  public void withAnnotatedFailingProcessorNoPathResolver() {
    assertThat(resolveProcessorRepresentation("BeanDefinitionFactoryRepresentationTestCase", fromSingleComponent("Mock@1"),
                                              ComponentMetadataAst.builder()
                                                  .setFileName("muleApp.xml")
                                                  .setStartLine(10)
                                                  .putDocAttribute(NAME_ANNOTATION_KEY.getLocalPart(), "Mock Component")
                                                  .build()),
               is("Mock@1 @ BeanDefinitionFactoryRepresentationTestCase:muleApp.xml:10 (Mock Component)"));
  }

  @Test
  public void withAnnotatedFailingProcessorPathResolver() {
    assertThat(resolveProcessorRepresentation("BeanDefinitionFactoryRepresentationTestCase",
                                              fromSingleComponent("flow/processor"),
                                              ComponentMetadataAst.builder()
                                                  .setFileName("muleApp.xml")
                                                  .setStartLine(10)
                                                  .putDocAttribute(NAME_ANNOTATION_KEY.getLocalPart(), "Mock Component")
                                                  .build()),
               is("flow/processor @ BeanDefinitionFactoryRepresentationTestCase:muleApp.xml:10 (Mock Component)"));
  }

  @Test
  public void withAnnotatedFailingProcessorNotPathResolver() {
    assertThat(resolveProcessorRepresentation("BeanDefinitionFactoryRepresentationTestCase", fromSingleComponent("Mock@1"),
                                              ComponentMetadataAst.builder()
                                                  .setFileName("muleApp.xml")
                                                  .setStartLine(10)
                                                  .putDocAttribute(NAME_ANNOTATION_KEY.getLocalPart(), "Mock Component")
                                                  .build()),
               is("Mock@1 @ BeanDefinitionFactoryRepresentationTestCase:muleApp.xml:10 (Mock Component)"));
  }

}
