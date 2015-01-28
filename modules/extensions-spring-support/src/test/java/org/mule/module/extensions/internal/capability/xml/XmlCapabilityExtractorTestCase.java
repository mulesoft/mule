/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.capability.xml;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import org.mule.extensions.annotations.capability.Xml;
import org.mule.extensions.introspection.capability.XmlCapability;
import org.mule.extensions.introspection.declaration.DeclarationConstruct;
import org.mule.module.extensions.internal.introspection.AbstractCapabilitiesExtractorContractTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class XmlCapabilityExtractorTestCase extends AbstractCapabilitiesExtractorContractTestCase
{

    private static final String SCHEMA_VERSION = "SCHEMA_VERSION";
    private static final String NAMESPACE = "NAMESPACE";
    private static final String SCHEMA_LOCATION = "SCHEMA_LOCATION";

    private static final String EXTENSION_NAME = "extension";
    private static final String EXTENSION_VERSION = "3.7";

    private ArgumentCaptor<XmlCapability> captor;

    @Before
    public void before()
    {
        super.before();
        captor = ArgumentCaptor.forClass(XmlCapability.class);
    }

    @Test
    public void capabilityAdded()
    {
        resolver.resolveCapabilities(declarationConstruct, XmlSupport.class, capabilitiesCallback);
        verify(capabilitiesCallback).withCapability(captor.capture());

        XmlCapability capability = captor.getValue();
        assertThat(capability, is(notNullValue()));
        assertThat(capability.getSchemaVersion(), is(SCHEMA_VERSION));
        assertThat(capability.getNamespace(), is(NAMESPACE));
        assertThat(capability.getSchemaLocation(), is(SCHEMA_LOCATION));
    }

    @Test
    public void defaultCapabilityValues()
    {
        declarationConstruct = new DeclarationConstruct(EXTENSION_NAME, EXTENSION_VERSION);
        resolver.resolveCapabilities(declarationConstruct, DefaultXmlExtension.class, capabilitiesCallback);
        verify(capabilitiesCallback).withCapability(captor.capture());

        XmlCapability capability = captor.getValue();
        assertThat(capability, is(notNullValue()));
        assertThat(capability.getSchemaVersion(), is(EXTENSION_VERSION));
        assertThat(capability.getNamespace(), is(NAMESPACE));
        assertThat(capability.getSchemaLocation(), equalTo(String.format(XmlCapabilityExtractor.DEFAULT_SCHEMA_LOCATION_MASK, EXTENSION_NAME)));
    }

    @Xml(schemaVersion = SCHEMA_VERSION, namespace = NAMESPACE, schemaLocation = SCHEMA_LOCATION)
    private static class XmlSupport
    {

    }

    @Xml(namespace = NAMESPACE)
    private static class DefaultXmlExtension
    {

    }
}
