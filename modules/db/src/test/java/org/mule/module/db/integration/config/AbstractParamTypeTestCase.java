/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.config;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.module.db.domain.param.QueryParam;
import org.mule.module.db.domain.query.QueryTemplate;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.FileUtils;
import org.mule.util.IOUtils;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public abstract class AbstractParamTypeTestCase extends FunctionalTestCase
{

    private String dbTypeName;

    @Rule
    public TemporaryFolder resourceFolder = new TemporaryFolder();

    public AbstractParamTypeTestCase(String dbTypeName)
    {
        this.dbTypeName = dbTypeName;
        setStartContext(false);
    }

    @Override
    protected String getConfigFile()
    {
        try
        {
            File file = generateConfigFile();

            return file.getAbsolutePath();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private File generateConfigFile() throws IOException
    {
        String originalXml = IOUtils.getResourceAsString("integration/config/jdbc-param-type-template-config.xml", getClass());
        String replacedXml = originalXml.replace("${typeName}", dbTypeName);
        File file = new File(resourceFolder.getRoot(), "mule-config.xml");
        FileUtils.writeStringToFile(file, replacedXml);

        return file;
    }

    @Test
    public void usesDefinedParamType() throws Exception
    {
        QueryTemplate parameterizedQueryTemplate = muleContext.getRegistry().lookupObject("parameterizedQuery");
        QueryParam queryParam = parameterizedQueryTemplate.getParams().get(0);

        assertThat(queryParam.getType().getName(), equalTo(dbTypeName));
    }
}
