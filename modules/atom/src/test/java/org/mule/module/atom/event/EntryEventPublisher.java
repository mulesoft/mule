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
