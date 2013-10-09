/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
