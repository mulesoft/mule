package org.mule.registry.impl.store;

import org.mule.persistence.PersistenceHelper;
import org.mule.registry.Registry;
import org.mule.registry.impl.MuleRegistration;
import org.mule.registry.metadata.MetadataStore;
import org.mule.registry.metadata.MissingMetadataException;
import org.mule.registry.metadata.ObjectMetadata;
import org.mule.registry.metadata.PropertyMetadata;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RegistryPersistenceHelper implements PersistenceHelper, Converter 
{
    private Registry registry;
    private boolean persistAll = false;

    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(RegistryPersistenceHelper.class);

    public RegistryPersistenceHelper(Registry registry) 
    {
        this.registry = registry;
    }

    public void setPersistAll(boolean persistAll)
    {
        this.persistAll = persistAll;
    }

    public boolean canConvert(Class clazz) 
    {
        return (clazz.equals(MuleRegistration.class));
    }

    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) 
    {
        MuleRegistration ref = (MuleRegistration) value;
        String className = (String)ref.getProperty("sourceObjectClassName");
        ObjectMetadata metadata = null;

        try 
        {
            metadata = MetadataStore.getObjectMetadata(className);
            logger.warn("EUREKA! Found metadata for " + className);
        }
        catch (MissingMetadataException mme) 
        {
            logger.warn("No metadata found for " + className);
        }

        if (persistAll || (metadata != null && metadata.getPersistable()))
        {
            writer.startNode("bean");
            if (ref.getProperty("name") != null)
                writer.addAttribute("id", (String)ref.getProperty("name"));
            writer.addAttribute("class", className);

            HashMap props = ref.getProperties();
            Iterator iter = props.keySet().iterator();
            while (iter.hasNext()) 
            {
                String key = (String)iter.next();
                Object v = props.get(key);

                if (metadata != null)
                {
                    PropertyMetadata p = metadata.getProperty(key);
                    if (v != null && p != null && p.getIsPersistable()) 
                    {
                        writer.startNode("property");
                        writer.addAttribute("name", key);
                        writer.startNode("value");
                        writer.setValue(v.toString());
                        writer.endNode();
                        writer.endNode();
                    }
                }
                else
                {
                    if (v != null)
                    {
                        writer.startNode("property");
                        writer.addAttribute("name", key);
                        writer.startNode("value");
                        writer.setValue(v.toString());
                        writer.endNode();
                        writer.endNode();
                    }
                }
            }
        }

        HashMap children = ref.retrieveChildren();
        Iterator iter = children.keySet().iterator();
        while (iter.hasNext()) 
        {
            String id = (String)iter.next();
            MuleRegistration child = (MuleRegistration)children.get(id);
            context.convertAnother(child);
        }

        if (persistAll || (metadata != null && metadata.getPersistable())) 
        {
            writer.endNode();
        }

    }

    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) 
    {
        return null;
    }

}
