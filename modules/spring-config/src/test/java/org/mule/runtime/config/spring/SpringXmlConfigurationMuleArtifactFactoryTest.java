/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static org.dom4j.DocumentHelper.parseText;
import static org.dom4j.DocumentHelper.selectNodes;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.common.MuleArtifactFactoryException;
import org.mule.common.config.XmlConfigurationCallback;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.tck.config.RegisterServicesConfigurationBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.io.DOMWriter;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;

public class SpringXmlConfigurationMuleArtifactFactoryTest {

  public static final String BEAN_PROPERTY_PLACEHOLDER_CLASS =
      "org.springframework.beans.factory.config.PropertyPlaceholderConfigurer";

  protected static Element createBeanPropertyPlaceholder(String location, String ignoreUnresolvable) throws DocumentException {
    Element propertyPlaceholderBean = createElement("bean", "http://www.springframework.org/schema/beans", "spring");
    propertyPlaceholderBean.setAttribute("id", "props");
    propertyPlaceholderBean.setAttribute("class", BEAN_PROPERTY_PLACEHOLDER_CLASS);
    if (location != null) {
      Element locationProperty = createElement("property", "http://www.springframework.org/schema/beans", "spring");
      locationProperty.setAttribute("name", "location");
      locationProperty.setAttribute("value", location);

      propertyPlaceholderBean.appendChild(propertyPlaceholderBean.getOwnerDocument().importNode(locationProperty, true));
    }

    if (ignoreUnresolvable != null) {
      Element ignoreUnresolvableProperty = createElement("property", "http://www.springframework.org/schema/beans", "spring");
      ignoreUnresolvableProperty.setAttribute("name", "ignoreUnresolvablePlaceholders");
      ignoreUnresolvableProperty.setAttribute("value", ignoreUnresolvable);
      propertyPlaceholderBean
          .appendChild(propertyPlaceholderBean.getOwnerDocument().importNode(ignoreUnresolvableProperty, true));
    }

    return propertyPlaceholderBean;
  }

  protected static Element createSecurePropertyPlaceholder(String location, String key) throws DocumentException {
    Element propertyPlaceholder =
        createElement("config", "http://www.mulesoft.org/schema/mule/secure-property-placeholder", "secure-property-placeholder");
    if (location != null) {
      propertyPlaceholder.setAttribute("location", location);
    }
    if (key != null) {
      propertyPlaceholder.setAttribute("key", key);
    }
    return propertyPlaceholder;
  }

  protected static Element createPropertyPlaceholder(String location, String ignoreUnresolvable) throws DocumentException {
    Element propertyPlaceholder =
        createElement("property-placeholder", "http://www.springframework.org/schema/context", "context");
    if (location != null) {
      propertyPlaceholder.setAttribute("location", location);
    }
    if (ignoreUnresolvable != null) {
      propertyPlaceholder.setAttribute("ignore-unresolvable", ignoreUnresolvable);
    }
    return propertyPlaceholder;
  }

  public static Element createElement(String name, String namespace) throws DocumentException {
    return createElement(name, namespace, null);
  }

  public static Element createElement(String name, String namespace, String prefix) throws DocumentException {
    org.dom4j.Element dom4jElement = DocumentHelper.createElement(new QName(name, new Namespace(prefix, namespace)));
    Document document = dom4jElement.getDocument();
    if (document == null) {
      document = DocumentHelper.createDocument();
      document.setRootElement(dom4jElement);
    }

    final DOMWriter writer = new DOMWriter();
    org.w3c.dom.Document w3cDocument = writer.write(document);
    Element w3cElement = w3cDocument.getDocumentElement();


    return w3cElement;
  }

  private XmlConfigurationCallback getXmlConfigurationCallbackMock() {
    XmlConfigurationCallback callback = mock(XmlConfigurationCallback.class);
    Map<String, String> map = new HashMap<String, String>();
    when(callback.getEnvironmentProperties()).thenReturn(map);
    when(callback.getSchemaLocation("http://www.mulesoft.org/schema/mule/core"))
        .thenReturn("http://www.mulesoft.org/schema/mule/core/current/mule.xsd");
    when(callback.getSchemaLocation("http://www.springframework.org/schema/beans"))
        .thenReturn("http://www.springframework.org/schema/beans/spring-beans-3.0.xsd");
    when(callback.getSchemaLocation("http://www.springframework.org/schema/context"))
        .thenReturn("http://www.springframework.org/schema/context/spring-context-3.0.xsd");
    when(callback.getSchemaLocation("http://www.mulesoft.org/schema/mule/secure-property-placeholder"))
        .thenReturn("http://www.mulesoft.org/schema/mule/secure-property-placeholder/1.0/mule-secure-property-placeholder.xsd");


    return callback;
  }

  private SpringXmlConfigurationMuleArtifactFactory factoryTest;

  @Before
  public void before() {
    factoryTest = new SpringXmlConfigurationMuleArtifactFactory() {

      @Override
      protected ConfigurationBuilder wrapConfigurationBuilder(ConfigurationBuilder configBuilder) {
        return new AbstractConfigurationBuilder() {

          @Override
          protected void doConfigure(MuleContext muleContext) throws Exception {
            configBuilder.configure(muleContext);
            new RegisterServicesConfigurationBuilder().configure(muleContext);
          }
        };
      }
    };
  }

  @Test
  public void whenCallingGetArtifactForMessageProcessorSystemPropertiesShouldRemain()
      throws MuleArtifactFactoryException, DocumentException {
    Properties properties = System.getProperties();

    XmlConfigurationCallback callback = mock(XmlConfigurationCallback.class);
    Map<String, String> map = new HashMap<String, String>();
    map.put("test", "test1");
    when(callback.getEnvironmentProperties()).thenReturn(map);
    when(callback.getPropertyPlaceholders()).thenReturn(new Element[] {});
    Element element = createElement("logger", "http://www.mulesoft.org/schema/mule/core");

    factoryTest.getArtifactForMessageProcessor(element, callback);

    assertThat("System properties where modified", properties, is(System.getProperties()));

  }

  @Test
  public void whenMultiplePropertyPlaceholdersWithIgnoreUnresolvableInAllExceptOneDoNotFail()
      throws MuleArtifactFactoryException, DocumentException {
    XmlConfigurationCallback callback = getXmlConfigurationCallbackMock();
    Element[] propertyPlaceholders = new Element[3];
    propertyPlaceholders[0] = createPropertyPlaceholder("test1.properties", null);
    propertyPlaceholders[1] = createPropertyPlaceholder("test2.properties", "true");
    propertyPlaceholders[2] = createPropertyPlaceholder("test3.properties", "true");

    when(callback.getPropertyPlaceholders()).thenReturn(propertyPlaceholders);
    Element element = createElement("logger", "http://www.mulesoft.org/schema/mule/core");

    String muleConfigTxt = factoryTest.getArtifactMuleConfig("test-flow", element, callback, false);
    Document muleConfig = parseText(muleConfigTxt);

    List result =
        selectNodes("/mule/*[local-name()='property-placeholder' and @location='test1.properties' and not(@ignore-unresolvable)]",
                    muleConfig);
    assertThat("Property placeholder for test1.properties is present", result.size(), is(1));

    result =
        selectNodes("/mule/*[local-name()='property-placeholder' and @location='test2.properties' and @ignore-unresolvable='true']",
                    muleConfig);
    assertThat("Property placeholder for test2.properties is present", result.size(), is(1));

    result =
        selectNodes("/mule/*[local-name()='property-placeholder' and @location='test3.properties' and @ignore-unresolvable='true']",
                    muleConfig);
    assertThat("Property placeholder for test3.properties is present", result.size(), is(1));
  }

  @Test
  public void whenOnePropertyPlaceholdersWithoutIgnoreUnresolvableDoNotFail()
      throws MuleArtifactFactoryException, DocumentException {
    XmlConfigurationCallback callback = getXmlConfigurationCallbackMock();
    Element[] propertyPlaceholders = new Element[1];
    propertyPlaceholders[0] = createPropertyPlaceholder("test1.properties", null);

    when(callback.getPropertyPlaceholders()).thenReturn(propertyPlaceholders);
    Element element = createElement("logger", "http://www.mulesoft.org/schema/mule/core");

    String muleConfigTxt = factoryTest.getArtifactMuleConfig("test-flow", element, callback, false);
    Document muleConfig = parseText(muleConfigTxt);

    List result =
        selectNodes("/mule/*[local-name()='property-placeholder' and @location='test1.properties' and not(@ignore-unresolvable)]",
                    muleConfig);
    assertThat("Property placeholder for test1.properties is present", result.size(), is(1));
  }

  @Test
  public void whenOnePropertyPlaceholdersWithIgnoreUnresolvableTrueDoNotFail()
      throws MuleArtifactFactoryException, DocumentException {
    XmlConfigurationCallback callback = getXmlConfigurationCallbackMock();
    Element[] propertyPlaceholders = new Element[1];
    propertyPlaceholders[0] = createPropertyPlaceholder("test1.properties", "true");

    when(callback.getPropertyPlaceholders()).thenReturn(propertyPlaceholders);
    Element element = createElement("logger", "http://www.mulesoft.org/schema/mule/core");

    String muleConfigTxt = factoryTest.getArtifactMuleConfig("test-flow", element, callback, false);
    Document muleConfig = parseText(muleConfigTxt);

    List result =
        selectNodes("/mule/*[local-name()='property-placeholder' and @location='test1.properties' and @ignore-unresolvable='true']",
                    muleConfig);
    assertThat("Property placeholder for test1.properties is present", result.size(), is(1));
  }

  @Test
  public void whenOnePropertyPlaceholdersWithIgnoreUnresolvableFalseDoNotFail()
      throws MuleArtifactFactoryException, DocumentException {
    XmlConfigurationCallback callback = getXmlConfigurationCallbackMock();
    Element[] propertyPlaceholders = new Element[1];
    propertyPlaceholders[0] = createPropertyPlaceholder("test1.properties", "false");

    when(callback.getPropertyPlaceholders()).thenReturn(propertyPlaceholders);
    Element element = createElement("logger", "http://www.mulesoft.org/schema/mule/core");

    String muleConfigTxt = factoryTest.getArtifactMuleConfig("test-flow", element, callback, false);
    Document muleConfig = parseText(muleConfigTxt);

    List result =
        selectNodes("/mule/*[local-name()='property-placeholder' and @location='test1.properties' and @ignore-unresolvable='false']",
                    muleConfig);
    assertThat("Property placeholder for test1.properties is present", result.size(), is(1));
  }

  @Test
  public void whenOneBeanPropertyPlaceholdersWithIgnoreUnresolvableFalseDoNotFail()
      throws MuleArtifactFactoryException, DocumentException {
    XmlConfigurationCallback callback = getXmlConfigurationCallbackMock();
    Element[] propertyPlaceholders = new Element[1];
    propertyPlaceholders[0] = createBeanPropertyPlaceholder("test1.properties", "false");
    when(callback.getPropertyPlaceholders()).thenReturn(propertyPlaceholders);

    Element element = createElement("logger", "http://www.mulesoft.org/schema/mule/core");

    String muleConfigTxt = factoryTest.getArtifactMuleConfig("test-flow", element, callback, false);
    Document muleConfig = parseText(muleConfigTxt);

    List result = selectNodes("/mule/*[local-name()='bean' and @class='" + BEAN_PROPERTY_PLACEHOLDER_CLASS + "']",
                              muleConfig);
    assertThat("Property placeholder bean is present", result.size(), is(1));

    result = selectNodes("//*[local-name()='property' and @name='location' and @value='test1.properties']", muleConfig);
    assertThat("Property placeholder prop for location is test1.properties", result.size(), is(1));

    result =
        selectNodes("//*[local-name()='property' and @name='ignoreUnresolvablePlaceholders' and @value='false']", muleConfig);
    assertThat("Property placeholder prop for ignoreUnresolvablePlaceholders is false", result.size(), is(1));
  }

  @Test
  public void whenOneBeanPropertyPlaceholdersAndOnePropertyPlaceholderWithIgnoreUnresolvableFalseDoNotFail()
      throws MuleArtifactFactoryException, DocumentException {
    XmlConfigurationCallback callback = getXmlConfigurationCallbackMock();
    Element[] propertyPlaceholders = new Element[2];
    propertyPlaceholders[0] = createBeanPropertyPlaceholder("test1.properties", "true");
    propertyPlaceholders[1] = createPropertyPlaceholder("test2.properties", "true");
    when(callback.getPropertyPlaceholders()).thenReturn(propertyPlaceholders);

    Element element = createElement("logger", "http://www.mulesoft.org/schema/mule/core");

    String muleConfigTxt = factoryTest.getArtifactMuleConfig("test-flow", element, callback, false);
    Document muleConfig = parseText(muleConfigTxt);

    List result = selectNodes("/mule/*[local-name()='bean' and @class='" + BEAN_PROPERTY_PLACEHOLDER_CLASS + "']",
                              muleConfig);
    assertThat("Property placeholder bean is present", result.size(), is(1));

    result = selectNodes("//*[local-name()='property' and @name='location' and @value='test1.properties']", muleConfig);
    assertThat("Property placeholder prop for location is test1.properties", result.size(), is(1));

    result = selectNodes("//*[local-name()='property' and @name='ignoreUnresolvablePlaceholders' and @value='true']", muleConfig);
    assertThat("Property placeholder prop for ignoreUnresolvablePlaceholders is true", result.size(), is(1));

    result =
        selectNodes("/mule/*[local-name()='property-placeholder' and @location='test2.properties' and @ignore-unresolvable='true']",
                    muleConfig);
    assertThat("Property placeholder for test2.properties is present", result.size(), is(1));
  }

  @Test
  public void whenOneSecurePropertyPlaceholderDoNotFail() throws MuleArtifactFactoryException, DocumentException {
    XmlConfigurationCallback callback = getXmlConfigurationCallbackMock();
    Element[] propertyPlaceholders = new Element[1];
    propertyPlaceholders[0] = createSecurePropertyPlaceholder("test1.properties", "secretKey");
    when(callback.getPropertyPlaceholders()).thenReturn(propertyPlaceholders);

    Element element = createElement("logger", "http://www.mulesoft.org/schema/mule/core");

    String muleConfigTxt = factoryTest.getArtifactMuleConfig("test-flow", element, callback, false);
    Document muleConfig = parseText(muleConfigTxt);

    List result = selectNodes("/mule/*[local-name()='config' and @location='test1.properties' and @key='secretKey']", muleConfig);
    assertThat("Property placeholder for test2.properties is present", result.size(), is(1));
  }
}
