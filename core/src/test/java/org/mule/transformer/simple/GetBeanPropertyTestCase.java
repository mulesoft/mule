/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class GetBeanPropertyTestCase extends AbstractMuleTestCase
{
    private CustomBean bean = new CustomBean();
    private GetBeanProperty transformer = new GetBeanProperty();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void classPropertySuppressed() throws Exception
    {
        thrown.expect(TransformerException.class);
        thrown.expectCause(isA(NoSuchMethodException.class));
        thrown.expectMessage(startsWith("Unknown property 'class'"));
        transformer.setPropertyName("class");
        transformer.transform(bean);
    }

    @Test
    public void commonPropertyAccessed() throws Exception
    {
        transformer.setPropertyName("name");
        assertThat(transformer.transform(bean).toString(), is("dangerousBean"));
    }

    public class CustomBean
    {
        int id;
        String name;

        private CustomBean()
        {
            id = 1337;
            name = "dangerousBean";
        }

        public int getId()
        {
            return id;
        }

        public String getName()
        {
            return name;
        }
    }
}
