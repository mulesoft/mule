/* 
* $Header$
* $Revision$
* $Date$
* ------------------------------------------------------------------------------------------------------
* 
* Copyright (c) SymphonySoft Limited. All rights reserved.
* http://www.symphonysoft.com
* 
* The software in this package is published under the terms of the BSD
* style license a copy of which has been included with this distribution in
* the LICENSE.txt file. 
*
*/
package org.mule.config.builders;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.config.ConfigurationException;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.security.PasswordBasedEncryptionStrategy;
import org.mule.umo.UMOEncryptionStrategy;
import org.mule.umo.manager.UMOManager;
import org.mule.util.BeanUtils;
import org.mule.util.ClassHelper;
import org.mule.util.PropertiesHelper;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Placeholders are ant-like tags that are embedded in Mule Xml configuration i.e. ${property.name}
 * and are used to swap in property values registered with the Mule container instance when the
 * configuration is loaded.
 *
 * This is a helper class used for parsing these tags.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class PlaceholderProcessor
{
    public static final String MULE_ENCRYPTION_PROPERTIES = "org.mule.config.encryption.properties";
    public static final String DEFAULT_ENCRYPTION_PROPERTIES_FILE = "mule-encryption.properties";
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(PlaceholderProcessor.class);

    private static boolean strategiesLoaded = false;

    private Map types = new HashMap();
    private Map schemes = new HashMap();

    public PlaceholderProcessor()
    {
        types.put("PBE", PasswordBasedEncryptionStrategy.class.getName());
    }

    public PlaceholderProcessor(Map types) {
        this.types = types;
    }

    public Attributes processAttributes(Attributes attributes, String elementName) throws ConfigurationException
    {
        AttributesImpl attribs = new AttributesImpl(attributes);
        String value = null;

        for(int i = 0; i < attribs.getLength(); i++) {
            value = attribs.getValue(i);
            value = processValue(value);
            if(value==null) {
                throw new ConfigurationException(new Message(Messages.PROPERTY_TEMPLATE_MALFORMED_X,
                        "<" + elementName + attribs.getLocalName(i) + "='" + value + "' ...>"));
            }
            attribs.setValue(i, value);
        }
        return attribs;
    }

    public String processValue(String value) throws ConfigurationException {
        String realValue = null;
        String key = null;

        UMOManager manager = MuleManager.getInstance();

        int x = value.indexOf("${");
        while(x > -1) {
            int y = value.indexOf("}", x +1);
            if(y==-1) {
                return null;
            }
            key = value.substring(x+2, y);
            realValue = (String)manager.getProperty(key);
            if(logger.isDebugEnabled()) {
                logger.debug("Param is '" + value + "', Property key is '" + key + "', Property value is '" + realValue + "'");
            }
            if(realValue!=null) {
                realValue = processEncryptedValue(realValue);
                if(realValue==null) {
                    return null;
                }
                value = value.substring(0, x) +  realValue + value.substring(y+1);

                // fix for a bug when realValue.length <= key.length
                y = y - 3 + (realValue.length() - key.length());
            } else {
                logger.info("Property for placeholder: '" + key + "' was not found.  Leaving place holder as is. This is not necessarily a problem as the placeholder may not be a Mule placeholder.");
            }
            x = value.indexOf("${", y);
        }
        return value;
    }

    protected String processEncryptedValue(String value) throws ConfigurationException
    {
        String scheme;
        int x = value.indexOf("{encrypt:");
        if(x > -1) {
            logger.debug("Value contains encrypted data.");
            int y = value.indexOf("}");
            if(y==-1) {
                logger.error("Encryption tag is malformed: " + value);
                return null;
            } else {
                scheme = value.substring((x + 9), y);
                logger.debug("look up encryption scheme: " + scheme);
                try {
                    UMOEncryptionStrategy strategy = getEncryptionStrategy(scheme);
                    String data = value.substring(y+1);
                    byte[] decrypted = strategy.decrypt(data.getBytes(), null);
                    return new String(decrypted);
                } catch (Exception e) {
                    throw new ConfigurationException(e);
                }
            }
        } else {
            return value;
        }
    }

    public UMOEncryptionStrategy getEncryptionStrategy(String scheme) throws Exception {
        if(!strategiesLoaded) {
            loadStrategies();
        }
        return (UMOEncryptionStrategy)schemes.get(scheme);
    }

    private void loadStrategies() throws Exception
    {
        String path = System.getProperty(MULE_ENCRYPTION_PROPERTIES, MuleManager.getConfiguration().getWorkingDirectory()
                + File.separator + DEFAULT_ENCRYPTION_PROPERTIES_FILE);

        logger.info("Attempting to load encryption properties from: " + path);
        Properties props = PropertiesHelper.loadProperties(path);

        Map names = new HashMap();
        PropertiesHelper.getPropertiesWithPrefix(props, "name", names);
        String name;
        for (Iterator iterator = names.values().iterator(); iterator.hasNext();) {
            name = (String) iterator.next();
            Map schemeConfig = new HashMap();
            PropertiesHelper.getPropertiesWithPrefix(props, name + ".", schemeConfig);
            schemeConfig = PropertiesHelper.removeNamspaces(schemeConfig);

            String type = (String)schemeConfig.get("type");
            String clazz = (String)types.get(type);
            if(clazz==null) {
                throw new IllegalArgumentException("Unknown encryption type: " + type);
            }
            logger.debug("Found Class: " + clazz + " for type: " + type);
            UMOEncryptionStrategy strat = (UMOEncryptionStrategy)ClassHelper.instanciateClass(clazz, ClassHelper.NO_ARGS, PlaceholderProcessor.class);
            BeanUtils.populateWithoutFail(strat, schemeConfig, true);
            schemes.put(name, strat);
        }
    }
}
