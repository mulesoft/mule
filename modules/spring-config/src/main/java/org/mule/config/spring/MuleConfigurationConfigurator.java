package org.mule.config.spring;

import org.mule.config.MuleConfiguration;
import org.mule.impl.ManagementContextAware;
import org.mule.umo.UMOManagementContext;

import org.springframework.beans.factory.SmartFactoryBean;

public class MuleConfigurationConfigurator extends MuleConfiguration implements ManagementContextAware, SmartFactoryBean
{
    
    private UMOManagementContext managementContext;

    public void setManagementContext(UMOManagementContext context)
    {
        this.managementContext = context;
    }

    public boolean isEagerInit()
    {
        return true;
    }

    public boolean isPrototype()
    {
        return false;
    }

    public Object getObject() throws Exception
    {
        MuleConfiguration configuration = managementContext.getConfiguration();
        configuration.setDefaultSynchronousEndpoints(isDefaultSynchronousEndpoints());
        configuration.setWorkingDirectory(getWorkingDirectory());
        configuration.setDefaultSynchronousEventTimeout(getDefaultSynchronousEventTimeout());
        configuration.setDefaultEncoding(getDefaultEncoding());
        configuration.setDefaultTransactionTimeout(getDefaultTransactionTimeout());
        configuration.setDefaultRemoteSync(isDefaultRemoteSync());
        configuration.setClusterId(getClusterId());
        configuration.setDomainId(getDomainId());
        configuration.setId(getId());
        
        //configuration.setD
        configuration.setDefaultConnectionStrategy(getDefaultConnectionStrategy());
        
        return configuration;
//        
//        
//        <xsd:element name="default-threading-profile" type="threadingProfileType" minOccurs="0"/>
//        <xsd:element name="default-dispatcher-threading-profile" type="threadingProfileType" minOccurs="0"/>
//        <xsd:element name="default-receiver-threading-profile" type="threadingProfileType" minOccurs="0"/>
//        <xsd:element name="default-component-threading-profile" type="threadingProfileType" minOccurs="0"/>
//        <xsd:element ref="abstract-storage" minOccurs="0"/>

    }

    public Class getObjectType()
    {
        return MuleConfiguration.class;
    }

    public boolean isSingleton()
    {
        return true;
    }

}
