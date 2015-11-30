/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import javax.xml.namespace.QName;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class LocatedMuleExceptionTestCase extends AbstractMuleContextTestCase
{

    private static QName docNameAttrName = new QName("http://www.mulesoft.org/schema/mule/documentation", "name");
    private static QName sourceFileNameAttrName = new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceFileName");
    private static QName sourceFileLineAttrName = new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceFileLine");

    @Test
    public void namedComponent()
    {
        NamedObject named = mock(NamedObject.class);
        when(named.getName()).thenReturn("mockComponent");
        LocatedMuleException lme = new LocatedMuleException(named);
        assertThat(lme.getInfo().get(LocatedMuleException.INFO_LOCATION_KEY).toString(), is("/mockComponent @ app"));
    }

    @Test
    public void annotatedComponent()
    {
        AnnotatedObject annotated = mock(AnnotatedObject.class);
        when(annotated.getAnnotation(eq(docNameAttrName))).thenReturn("Mock Component");
        when(annotated.getAnnotation(eq(sourceFileNameAttrName))).thenReturn("muleApp.xml");
        when(annotated.getAnnotation(eq(sourceFileLineAttrName))).thenReturn(10);
        when(annotated.toString()).thenReturn("Mock@1");

        LocatedMuleException lme = new LocatedMuleException(annotated);
        assertThat(lme.getInfo().get(LocatedMuleException.INFO_LOCATION_KEY).toString(), is("Mock@1 @ app:muleApp.xml:10 (Mock Component)"));
    }

    @Test
    public void namedAnnotatedComponent()
    {
        AnnotatedObject namedAnnotated = mock(AnnotatedObject.class, withSettings().extraInterfaces(NamedObject.class));
        when(((NamedObject) namedAnnotated).getName()).thenReturn("mockComponent");
        when(namedAnnotated.getAnnotation(eq(docNameAttrName))).thenReturn("Mock Component");
        when(namedAnnotated.getAnnotation(eq(sourceFileNameAttrName))).thenReturn("muleConfig.xml");
        when(namedAnnotated.getAnnotation(eq(sourceFileLineAttrName))).thenReturn(6);
        when(namedAnnotated.toString()).thenReturn("Mock@1");

        LocatedMuleException lme = new LocatedMuleException(namedAnnotated);
        assertThat(lme.getInfo().get(LocatedMuleException.INFO_LOCATION_KEY).toString(), is("/mockComponent @ app:muleConfig.xml:6 (Mock Component)"));
    }

    @Test
    public void rawComponent()
    {
        Object raw = mock(Object.class);
        when(raw.toString()).thenReturn("Mock@1");

        LocatedMuleException lme = new LocatedMuleException(raw);
        assertThat(lme.getInfo().get(LocatedMuleException.INFO_LOCATION_KEY).toString(), is("Mock@1 @ app"));
    }
}
