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

import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.digester.CallMethodRule;
import org.apache.commons.digester.CallParamRule;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.ObjectCreateRule;
import org.apache.commons.digester.Rule;
import org.apache.commons.digester.RuleSetBase;
import org.mule.MuleManager;
import org.mule.config.MuleConfiguration;
import org.mule.config.PropertyFactory;
import org.mule.util.ClassHelper;
import org.mule.util.Utility;
import org.xml.sax.Attributes;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * A digester rule set that loads rules for <properties> tags and its child tags;
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MulePropertiesRuleSet extends RuleSetBase
{
    private String path;
    private PlaceholderProcessor processor;
    private String propertiesSetterName;
    private List objectRefs = null;
    private String parentElement = "properties";


    public MulePropertiesRuleSet(String path, String propertiesSetterName, List objectRefs) {
        this(path, objectRefs);
        this.propertiesSetterName = propertiesSetterName;
    }

    public MulePropertiesRuleSet(String path, String propertiesSetterName, List objectRefs, String parentElement) {
        this(path, objectRefs);
        this.propertiesSetterName = propertiesSetterName;
        this.parentElement = parentElement;
    }

    public MulePropertiesRuleSet(String path, List objectRefs) {
        this.path = path;
        processor = new PlaceholderProcessor();
        this.objectRefs = objectRefs;
    }

    public void addRuleInstances(Digester digester) {

        path += "/" + parentElement;
       // digester.addObjectCreate(path, HashMap.class);
        digester.addRule(path, new ObjectCreateRule(path, HashMap.class) {
            // This will set the properties on the top object as bean setters if
            // the flag is set
            public void end(String string, String string1) throws Exception
            {
                Map props = (Map) digester.peek();
                if (props.containsKey(MuleConfiguration.USE_MANAGER_PROPERTIES)) {
                    props.putAll(MuleManager.getInstance().getProperties());
                    props.remove(MuleConfiguration.USE_MANAGER_PROPERTIES);
                }
                super.end(string, string1);

                if (propertiesSetterName==null) {
                    org.mule.util.BeanUtils.populateWithoutFail(digester.peek(), props, true);
                } else {
                    MethodUtils.invokeMethod(digester.peek(), propertiesSetterName, props);
                    //digester.addSetNext(path + "/properties", "setProperties");
                }
                //todo - is this needed?
                // support for setting transformers as properties
//                String trans = (String) props.remove("transformer");
//                if (trans != null) {
//                    addTransformerReference("transformer", trans, digester.peek());
//                }
            }
        });
        digester.addCallMethod(path + "/property", "put", 2);

        digester.addRule(path + "/property", new ProcessedCallParamRule(0, "name"));
        digester.addRule(path + "/property", new ProcessedCallParamRule(1, "value"));

        addPropertyFactoryRule(digester, path + "/factory-property");
        addSystemPropertyRule(digester, path + "/system-property");
        addFilePropertiesRule(digester, path + "/file-properties");
        addContainerPropertyRule(digester, path + "/container-property", propertiesSetterName==null);
        addTextPropertyRule(digester, path + "/text-property");

        addMapPropertyRules(digester, path);
        addListPropertyRules(digester, path);

        addMapPropertyRules(digester, path + "/map");
        addListPropertyRules(digester, path + "/map");
    }

    protected void addMapPropertyRules(Digester digester, String path)
    {
        digester.addObjectCreate(path + "/map", HashMap.class);
        digester.addCallMethod(path + "/map/property", "put", 2);
        digester.addRule(path + "/map/property", new ProcessedCallParamRule(0, "name"));
        digester.addRule(path + "/map/property", new ProcessedCallParamRule(1, "value"));

        addPropertyFactoryRule(digester, path + "/map/factory-property");
        addSystemPropertyRule(digester, path + "/map/system-property");
        addFilePropertiesRule(digester, path + "/map/file-properties");
        addContainerPropertyRule(digester, path + "/map/container-property", false);

        //  call the put method on top -1 object
        digester.addRule(path + "/map", new CallMethodOnIndexRule("put", 2, 1));
        digester.addCallParam(path + "/map", 0, "name");
        digester.addCallParam(path + "/map", 1, true);
    }

    protected void addListPropertyRules(Digester digester, String path)
    {
        digester.addObjectCreate(path + "/list", ArrayList.class);

        // digester.addCallMethod(path + "/list/entry", "add", 1);
        digester.addRule(path + "/list/entry", new CallMethodRule("add", 1) {
            public void begin(String endpointName, String endpointName1, Attributes attributes) throws Exception
            {
                // Process template tokens
                attributes = processor.processAttributes(attributes, endpointName1);
                super.begin(endpointName, endpointName1, attributes);
            }
        });

        digester.addRule(path + "/list/entry", new ProcessedCallParamRule(0, "value"));

        addPropertyFactoryRule(digester, path + "/list/factory-entry");
        addSystemPropertyRule(digester, path + "/list/system-entry");
        addContainerPropertyRule(digester, path + "/list/container-entry", false);

        // A small hack to call a method on top -1
        digester.addRule(path + "/list", new CallMethodOnIndexRule("put", 2, 1));
        digester.addCallParam(path + "/list", 0, "name");
        digester.addCallParam(path + "/list", 1, true);
    }
    protected void addPropertyFactoryRule(Digester digester, String path)
    {
        digester.addRule(path, new Rule() {

            public void begin(String s, String s1, Attributes attributes) throws Exception
            {
                // Process template tokens
                attributes = processor.processAttributes(attributes, s1);

                String clazz = attributes.getValue("factory");
                String name = attributes.getValue("name");
                Object props = digester.peek();
                Object obj = ClassHelper.instanciateClass(clazz, ClassHelper.NO_ARGS);
                if (obj instanceof PropertyFactory) {
                    if (props instanceof Map) {
                        obj = ((PropertyFactory) obj).create((Map) props);
                    } else {
                        // this must be a list so we'll get the containing
                        // properties map
                        obj = ((PropertyFactory) obj).create((Map) digester.peek(1));
                    }
                }
                if (obj != null) {
                    if (props instanceof Map) {
                        ((Map) props).put(name, obj);
                    } else {
                        ((List) props).add(obj);
                    }
                }
            }
        });
    }

    protected void addSystemPropertyRule(Digester digester, String path)
    {
        digester.addRule(path, new Rule() {
            public void begin(String s, String s1, Attributes attributes) throws Exception
            {
                // Process template tokens
                attributes = processor.processAttributes(attributes, s1);

                String name = attributes.getValue("name");
                String key = attributes.getValue("key");
                String defaultValue = attributes.getValue("defaultValue");
                String value = System.getProperty(key, defaultValue);
                if (value != null) {
                    Object props = digester.peek();
                    if (props instanceof Map) {
                        ((Map) props).put(name, value);
                    } else {
                        ((List) props).add(value);
                    }
                }
            }
        });
    }

    protected void addFilePropertiesRule(Digester digester, String path)
    {
        digester.addRule(path, new Rule() {
            public void begin(String s, String s1, Attributes attributes) throws Exception
            {
                // Process template tokens
                attributes = processor.processAttributes(attributes, s1);

                String location = attributes.getValue("location");
                String temp = attributes.getValue("override");
                boolean override = "true".equalsIgnoreCase(temp);
                InputStream is = Utility.loadResource(location, getClass());
                if (is == null) {
                    throw new FileNotFoundException(location);
                }
                Properties p = new Properties();
                p.load(is);
                Map props = (Map) digester.peek();
                if (override) {
                    props.putAll(p);
                } else {
                    String key;
                    for (Iterator iterator = p.keySet().iterator(); iterator.hasNext();) {
                        key = (String) iterator.next();
                        if (!props.containsKey(key)) {
                            props.put(key, p.getProperty(key));
                        }
                    }
                }
            }
        });
    }

    protected void addContainerPropertyRule(Digester digester, String path, final boolean asBean)
    {
        digester.addRule(path, new Rule() {
            public void begin(String s, String s1, Attributes attributes) throws Exception
            {
                attributes = processor.processAttributes(attributes, s1);

                String name = attributes.getValue("name");
                String value = attributes.getValue("reference");
                String required = attributes.getValue("required");
                String container = attributes.getValue("container");
                if (required == null) {
                    required = "true";
                }
                boolean req = Boolean.valueOf(required).booleanValue();
                // if we're not setting as bean properties we need get the
                // top-most object
                // which will be a list or Map
                Object obj = null;
                if (asBean) {
                    obj = digester.peek(1);
                } else {
                    obj = digester.peek();
                }
                objectRefs.add(new ContainerReference(name, value, obj, req, container));
            }
        });
    }

    protected void addTextPropertyRule(Digester digester, String path)
    {

        digester.addRule(path, new Rule() {
            private String name = null;
            public void begin(String s, String s1, Attributes attributes) throws Exception
            {
                // Process template tokens
                attributes = processor.processAttributes(attributes, s1);
                name = attributes.getValue("name");
            }

            public void body(String string, String string1, String string2) throws Exception {
                Object props = digester.peek();
                if (props instanceof Map) {
                    ((Map) props).put(name, string2);
                } else {
                    ((List) props).add(string2);
                }
            }
        });
    }

    private class ProcessedCallParamRule extends CallParamRule
    {
        public ProcessedCallParamRule(int i) {
            super(i);
        }

        public ProcessedCallParamRule(int i, String s) {
            super(i, s);
        }

        public ProcessedCallParamRule(int i, boolean b) {
            super(i, b);
        }

        public ProcessedCallParamRule(int i, int i1) {
            super(i, i1);
        }

        public void begin(String endpointName, String endpointName1, Attributes attributes) throws Exception
        {
            // Process template tokens
            attributes = processor.processAttributes(attributes, endpointName1);
            super.begin(endpointName, endpointName1, attributes);
        }
    }

    public static interface PropertiesCallback {
        public void setProperties(Map props, Digester digester) throws Exception;
    }
}
