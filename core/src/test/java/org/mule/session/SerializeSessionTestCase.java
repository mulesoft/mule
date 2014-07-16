/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.session;

import static org.junit.Assert.assertEquals;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.util.FileUtils;
import org.mule.util.SerializationUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Test;

public class SerializeSessionTestCase extends AbstractMuleTestCase
{

    @Test
    public void readsMule_3_2_Session() throws IOException, URISyntaxException
    {
        URL resource = getClass().getClassLoader().getResource("muleSession-3.2");

        File sessionFile = new File(resource.toURI());

        byte[] bytes = FileUtils.readFileToByteArray(sessionFile);

        Object deserialize = SerializationUtils.deserialize(bytes);

        assertEquals(DefaultMuleSession.class, deserialize.getClass());
    }
}
