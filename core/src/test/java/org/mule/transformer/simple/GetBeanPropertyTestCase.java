/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class GetBeanPropertyTestCase extends AbstractMuleTestCase
{
    private CustomBean bean = new CustomBean();
    private GetBeanProperty transformer = new GetBeanProperty();

    @Test(expected = TransformerException.class)
    public void classPropertySuppressed() throws Exception
    {
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
