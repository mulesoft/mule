/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mule.runtime.api.exception.MuleException.INFO_LOCATION_KEY;

import static org.hamcrest.CoreMatchers.is;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.LocatedMuleException;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import javax.xml.namespace.QName;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class LocatedMuleExceptionTestCase extends AbstractMuleContextTestCase {

  private static QName docNameAttrName = new QName("http://www.mulesoft.org/schema/mule/documentation", "name");

  private ComponentLocation mockComponentLocation = mock(ComponentLocation.class);

  @Test
  public void namedComponent() {
    NamedObject named = mock(NamedObject.class, withSettings().extraInterfaces(Component.class));
    when(named.getName()).thenReturn("mockComponent");
    LocatedMuleException lme = new LocatedMuleException(named);
    assertThat(lme.getInfo().get(INFO_LOCATION_KEY).toString(), is("/mockComponent @ app:internal:-1"));
  }

  @Test
  public void annotatedComponent() {
    Component annotated = mock(Component.class);
    when(annotated.getAnnotation(eq(docNameAttrName))).thenReturn("Mock Component");
    when(annotated.toString()).thenReturn("Mock@1");
    configureProcessorLocation(annotated);

    LocatedMuleException lme = new LocatedMuleException(annotated);
    assertThat(lme.getInfo().get(INFO_LOCATION_KEY).toString(),
               is("Mock@1 @ app:muleApp.xml:10 (Mock Component)"));
  }

  @Test
  public void namedAnnotatedComponent() {
    Component namedAnnotated = mock(Component.class, withSettings().extraInterfaces(NamedObject.class));
    when(((NamedObject) namedAnnotated).getName()).thenReturn("mockComponent");
    when(namedAnnotated.getAnnotation(eq(docNameAttrName))).thenReturn("Mock Component");
    when(namedAnnotated.toString()).thenReturn("Mock@1");
    configureProcessorLocation(namedAnnotated);

    LocatedMuleException lme = new LocatedMuleException(namedAnnotated);
    assertThat(lme.getInfo().get(INFO_LOCATION_KEY).toString(),
               is("/mockComponent @ app:muleApp.xml:10 (Mock Component)"));
  }

  @Test
  public void rawComponent() {
    Object raw = mock(Object.class, withSettings().extraInterfaces(Component.class));
    when(raw.toString()).thenReturn("Mock@1");

    LocatedMuleException lme = new LocatedMuleException(raw);
    assertThat(lme.getInfo().get(INFO_LOCATION_KEY).toString(), is("Mock@1 @ app:internal:-1"));
  }

  private void configureProcessorLocation(Component component) {
    when(component.getLocation()).thenReturn(mockComponentLocation);
    when(mockComponentLocation.getFileName()).thenReturn(Optional.of("muleApp.xml"));
    when(mockComponentLocation.getLineInFile()).thenReturn(Optional.of(10));
    when(mockComponentLocation.getLocation()).thenReturn("Mock@1");
  }
}
