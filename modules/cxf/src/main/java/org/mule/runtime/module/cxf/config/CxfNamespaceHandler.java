/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf.config;

import org.mule.runtime.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.runtime.config.spring.parsers.collection.ChildListDefinitionParser;
import org.mule.runtime.config.spring.parsers.collection.ChildMapDefinitionParser;
import org.mule.runtime.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.runtime.config.spring.parsers.processors.AddAttribute;
import org.mule.runtime.config.spring.parsers.specific.ComponentDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.module.cxf.CxfConfiguration;
import org.mule.runtime.module.cxf.CxfConstants;
import org.mule.runtime.module.cxf.component.WebServiceWrapperComponent;
import org.mule.runtime.module.cxf.support.MuleSecurityManagerValidator;
import org.mule.runtime.module.cxf.support.StaxFeature;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.configuration.spring.SimpleBeanDefinitionParser;
import org.apache.cxf.configuration.spring.StringBeanDefinitionParser;
import org.apache.cxf.databinding.source.SourceDataBinding;
import org.apache.cxf.databinding.stax.StaxDataBinding;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jibx.JibxDataBinding;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class CxfNamespaceHandler extends AbstractMuleNamespaceHandler
{

    @Override
    public void init()
    {
        MuleOrphanDefinitionParser configParser = new MuleOrphanDefinitionParser(CxfConfiguration.class, true) {

            @Override
            protected String resolveId(Element element,
                                       AbstractBeanDefinition definition,
                                       ParserContext parserContext) throws BeanDefinitionStoreException
            {
                return CxfConstants.DEFAULT_CXF_CONFIGURATION;
            }

            @Override
            protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext)
            {
                // We don't want spring managing lifecycle anymore, but since this module
                // is going to be rewritten, it doesn't make sense to spend time in removing this
                // init method
                AbstractBeanDefinition beanDefinition = super.parseInternal(element, parserContext);
                beanDefinition.setInitMethodName(Initialisable.PHASE_NAME);

                return beanDefinition;
            }
        };
        configParser.addIgnored("name");
        registerMuleBeanDefinitionParser("configuration", configParser);
        
        MessageProcessorDefinitionParser jsParser = new MessageProcessorDefinitionParser(WebServiceFactoryBean.class);
        jsParser.registerPreProcessor(new AddAttribute("frontend", CxfConstants.JAX_WS_FRONTEND));
        registerBeanDefinitionParser("jaxws-service", jsParser);
        
        MessageProcessorDefinitionParser ssParser = new MessageProcessorDefinitionParser(WebServiceFactoryBean.class);
        ssParser.registerPreProcessor(new AddAttribute("frontend", CxfConstants.SIMPLE_FRONTEND));
        registerBeanDefinitionParser("simple-service", ssParser);

        registerBeanDefinitionParser("proxy-service", new MessageProcessorDefinitionParser(ProxyServiceFactoryBean.class));
        
        registerBeanDefinitionParser("simple-client", new MessageProcessorDefinitionParser(SimpleClientFactoryBean.class));
        registerBeanDefinitionParser("jaxws-client", new MessageProcessorDefinitionParser(JaxWsClientFactoryBean.class));
        registerBeanDefinitionParser("proxy-client", new MessageProcessorDefinitionParser(ProxyClientFactoryBean.class));

        registerBeanDefinitionParser(CxfConstants.FEATURES, new ChildListDefinitionParser(CxfConstants.FEATURES));
        registerBeanDefinitionParser("schemaLocations", new ChildListDefinitionParser("schemaLocations"));
        registerBeanDefinitionParser("schemaLocation", new StringBeanDefinitionParser());

        registerBeanDefinitionParser("jaxb-databinding", new ChildDefinitionParser(CxfConstants.DATA_BINDING, JAXBDataBinding.class));
        registerBeanDefinitionParser("jibx-databinding", new ChildDefinitionParser(CxfConstants.DATA_BINDING, JibxDataBinding.class));
        registerBeanDefinitionParser("stax-databinding", new ChildDefinitionParser(CxfConstants.DATA_BINDING, StaxDataBinding.class));
        registerBeanDefinitionParser("source-databinding", new ChildDefinitionParser(CxfConstants.DATA_BINDING, SourceDataBinding.class));
        registerBeanDefinitionParser("aegis-databinding", new ChildDefinitionParser(CxfConstants.DATA_BINDING, AegisDatabinding.class));
        registerBeanDefinitionParser("custom-databinding", new ChildDefinitionParser(CxfConstants.DATA_BINDING));
        
        registerBeanDefinitionParser(CxfConstants.IN_INTERCEPTORS, new ChildListDefinitionParser(CxfConstants.IN_INTERCEPTORS));
        registerBeanDefinitionParser(CxfConstants.IN_FAULT_INTERCEPTORS, new ChildListDefinitionParser(CxfConstants.IN_FAULT_INTERCEPTORS));
        registerBeanDefinitionParser(CxfConstants.OUT_INTERCEPTORS, new ChildListDefinitionParser(CxfConstants.OUT_INTERCEPTORS));
        registerBeanDefinitionParser(CxfConstants.OUT_FAULT_INTERCEPTORS, new ChildListDefinitionParser(CxfConstants.OUT_FAULT_INTERCEPTORS));
        
        registerBeanDefinitionParser("stax", new SimpleBeanDefinitionParser(StaxFeature.class));

        registerBeanDefinitionParser("wrapper-component", new ComponentDefinitionParser(WebServiceWrapperComponent.class));

        registerMuleBeanDefinitionParser("properties", new ChildMapDefinitionParser("addProperties")).addCollection("addProperties");
        
        registerBeanDefinitionParser("ws-security", new WsSecurityDefinitionParser(WsSecurity.class));

        ChildDefinitionParser msmvParser = new ChildDefinitionParser("securityManager", MuleSecurityManagerValidator.class);
        msmvParser.registerPreProcessor(new AddAttribute("securityManager-ref", "_muleSecurityManager"));
        registerBeanDefinitionParser("mule-security-manager", msmvParser);

        registerBeanDefinitionParser("ws-config", new ChildDefinitionParser("wsConfig", WsConfig.class));
        registerMuleBeanDefinitionParser("property", new ChildMapEntryDefinitionParser("configProperties"));

        registerBeanDefinitionParser("ws-custom-validator", new WsCustomValidatorDefinitionParser("customValidator"));

    }
}
