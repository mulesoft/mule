/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp.reliability;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.config.builders.SimpleConfigurationBuilder;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.transport.sftp.AbstractSftpFunctionalTestCase;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public abstract class AbstractSftpRedeliveryTestCase extends AbstractSftpFunctionalTestCase
{

    protected static final String FILENAME = "file.txt";
    protected static final int TIMEOUT = 10000;
    protected static final int MAX_REDELIVERY_ATTEMPS = 2;

    @Rule
    public SystemProperty maxRedeliveryAttemptSystemProperty = new SystemProperty("maxRedelivery", "2");

    protected static List<Object[]> getParameters()
    {
        return Arrays.asList(new Object[][]
                                     {
                                             {"Without Archiving", false},
                                             {"With Archiving", true}
                                     });
    }

    private final String name;
    private final boolean archive;

    @Rule
    public TemporaryFolder archiveFolder = new TemporaryFolder();

    @Rule
    public SystemProperty archiveProperty;

    public AbstractSftpRedeliveryTestCase(String name, boolean archive)
    {
        this.name = name;
        this.archive = archive;
    }

    @Override
    protected void addBuilders(List<ConfigurationBuilder> builders)
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("archiveDir", archive ? archiveFolder.getRoot().getAbsolutePath() : "");

        builders.add(0, new SimpleConfigurationBuilder(properties));
    }


    @Test
    public void testSuccessfulDelivery() throws Exception
    {
        sftpClient.storeFile(FILENAME, new ByteArrayInputStream(TEST_PAYLOAD.getBytes()));
        MuleClient muleClient = muleContext.getClient();
        MuleMessage response = muleClient.request("vm://out", TIMEOUT);

        assertThat(response, notNullValue());
        assertThat(response.getPayloadAsString(), is(TEST_PAYLOAD));
        assertFilesDeleted();
    }

    protected void assertFilesDeleted() throws Exception
    {
        PollingProber pollingProber = new PollingProber(TIMEOUT, 100);
        pollingProber.check(new JUnitProbe()
        {
            @Override
            protected boolean test() throws Exception
            {
                assertThat(Arrays.asList(sftpClient.listFiles()), is(empty()));
                return true;
            }

            @Override
            public String describeFailure()
            {
                return "files were not deleted";
            }
        });
    }
}
