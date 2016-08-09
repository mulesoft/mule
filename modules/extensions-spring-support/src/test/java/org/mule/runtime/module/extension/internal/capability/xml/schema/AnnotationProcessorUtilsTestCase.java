/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.junit.Test;

@SmallTest
public class AnnotationProcessorUtilsTestCase extends AbstractMuleTestCase {

  @Test
  public void getOperationMethodsOnNotAnnotatedClass() {
    final TypeElement element = mock(TypeElement.class);

    RoundEnvironment roundEnvironment = mock(RoundEnvironment.class, RETURNS_DEEP_STUBS);
    when(roundEnvironment.getElementsAnnotatedWith(Extension.class)).thenReturn((Set) ImmutableSet.of(element));

    ProcessingEnvironment processingEnvironment = mock(ProcessingEnvironment.class, RETURNS_DEEP_STUBS);
    when(processingEnvironment.getElementUtils().getBinaryName(any()).toString()).thenReturn(Object.class.getName());

    Map<String, Element> result = AnnotationProcessorUtils.getOperationMethods(roundEnvironment, processingEnvironment);
    assertThat(result.isEmpty(), is(true));
  }
}
