/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom.event;

import java.util.Date;

import org.apache.abdera.Abdera;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Entry;

public class EntryEventPublisher
{
    public Entry newEvent()
    {
        Factory factory = Abdera.getInstance().getFactory();

        Entry entry = factory.newEntry();
        entry.setTitle("Some Event");
        entry.setContent("Foo bar");
        entry.setUpdated(new Date());
        entry.setId(factory.newUuidUri());
        entry.addAuthor("Dan Diephouse");

        return entry;
    }
}
