/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.store;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.mule.api.MuleContext;
import org.mule.api.store.ObjectDoesNotExistException;
import org.mule.api.store.ObjectStoreException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

@SmallTest
public class PersistentObjectStorePartitionTestCase extends AbstractMuleTestCase
{

    @Rule
    public TemporaryFolder objectStoreFolder = new TemporaryFolder();

    @Test
    public void indicatesUnexistentKeyOnRetrieveError() throws ObjectStoreException
    {

        MuleContext muleContext = Mockito.mock(MuleContext.class);
        PersistentObjectStorePartition partition = new PersistentObjectStorePartition(muleContext, "test", objectStoreFolder.getRoot());
        partition.open();

        final String nonExistentKey = "nonExistentKey";

        try
        {
            partition.retrieve(nonExistentKey);
            fail("Supposed to thrown an exception as key is not valid");
        }
        catch (ObjectDoesNotExistException e)
        {
            assertTrue(e.getMessage().contains(nonExistentKey));
        }
    }
}
