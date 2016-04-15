/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.domain.query;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.module.db.internal.util.FileReader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.IOException;

import org.junit.Test;

@SmallTest
public class QueryFileFactoryBeanTestCase extends AbstractMuleTestCase
{

    @Test
    public void createsQueryFromFile() throws Exception
    {
        String fileName = "file";
        String fileContent = "select * from test";

        FileReader fileReader = mock(FileReader.class);
        when(fileReader.getResourceAsString(fileName)).thenReturn(fileContent);

        QueryFileFactoryBean factoryBean = new QueryFileFactoryBean(fileName, fileReader);

        String query = factoryBean.getObject();

        assertThat(query, equalTo(fileContent));
    }

    @Test(expected = IllegalStateException.class)
    public void failsToReadFile() throws Exception
    {
        String fileName = "file";

        FileReader fileReader = mock(FileReader.class);
        when(fileReader.getResourceAsString(fileName)).thenThrow(new IOException());

        QueryFileFactoryBean factoryBean = new QueryFileFactoryBean(fileName, fileReader);

        factoryBean.getObject();
    }
}
