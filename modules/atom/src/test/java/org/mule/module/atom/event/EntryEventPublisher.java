/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
