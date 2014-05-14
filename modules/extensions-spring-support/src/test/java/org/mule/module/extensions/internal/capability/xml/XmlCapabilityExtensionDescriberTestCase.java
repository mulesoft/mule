/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.capability.xml;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.mule.extensions.introspection.capability.XmlCapability;
import org.mule.extensions.introspection.declaration.Declaration;
import org.mule.module.extensions.HeisenbergExtension;
import org.mule.module.extensions.internal.AnnotationsBasedDescriberTestCase;
import org.mule.tck.size.SmallTest;

@SmallTest
public class XmlCapabilityExtensionDescriberTestCase extends AnnotationsBasedDescriberTestCase
{

    @Override
    protected void assertCapabilities(Declaration declaration)
    {
        assertXmlCapability(declaration);
    }

    private void assertXmlCapability(Declaration declaration)
    {
        assertThat(declaration.getCapabilities().isEmpty(), is(false));
        Object capability = declaration.getCapabilities().iterator().next();
        assertThat(capability, instanceOf(XmlCapability.class));

        XmlCapability xml = (XmlCapability) capability;
        assertThat(xml.getSchemaLocation(), is(HeisenbergExtension.SCHEMA_LOCATION));
        assertThat(xml.getSchemaVersion(), is(HeisenbergExtension.SCHEMA_VERSION));
        assertThat(xml.getNamespace(), is(HeisenbergExtension.NAMESPACE));
    }
}
