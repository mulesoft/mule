/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

import java.util.List;

import javax.xml.namespace.QName;

public class CxfMessages extends MessageFactory
{
    private static final CxfMessages factory = new CxfMessages();
    
    private static final String BUNDLE_PATH = getBundlePath("cxf");

    public static Message serviceIsNull(String serviceName)
    {
        return factory.createMessage(BUNDLE_PATH, 8, serviceName);
    }

    public static Message annotationsRequireJava5()
    {
        return factory.createMessage(BUNDLE_PATH, 9);
    }

    public static Message couldNotInitAnnotationProcessor(Object object)
    {
        return factory.createMessage(BUNDLE_PATH, 10, object);
    }

    public static Message unableToInitBindingProvider(String bindingProvider)
    {
        return factory.createMessage(BUNDLE_PATH, 11, bindingProvider);
    }

    public static Message unableToLoadServiceClass(String classname) 
    {
        return factory.createMessage(BUNDLE_PATH,12,classname);
    }

    public static Message unableToConstructAdapterForNullMessage() 
    {
        return factory.createMessage(BUNDLE_PATH,13);
    }

    public static Message inappropriateMessageTypeForAttachments(org.apache.cxf.message.Message message) 
    {
        String className = message.getClass().getName();
        return factory.createMessage(BUNDLE_PATH, 14, className);
    }

    public static Message bothServiceClassAndWsdlUrlAreRequired() 
    {
        return factory.createMessage(BUNDLE_PATH,15);
    }

    public static Message incorrectlyFormattedEndpointUri(String uri) 
    {
        return factory.createMessage(BUNDLE_PATH,16,uri);
    }

    public static Message invalidFrontend(String frontend)
    {
        return factory.createMessage(BUNDLE_PATH,17,frontend);
    }

    public static Message portNotFound(String port) 
    {
        return factory.createMessage(BUNDLE_PATH,18,port);
    }

    public static Message mustSpecifyPort() 
    {
        return factory.createMessage(BUNDLE_PATH,19);
    }

    public static Message wsdlNotFound(String loc) 
    {
        return factory.createMessage(BUNDLE_PATH,20,loc);
    }

    public static Message noOperationWasFoundOrSpecified()
    {
        return factory.createMessage(BUNDLE_PATH,21);
    }
    
    public static Message javaComponentRequiredForInboundEndpoint()
    {
        return factory.createMessage(BUNDLE_PATH,22);
    }

    public static Message serviceClassRequiredWithPassThrough()
    {
        return factory.createMessage(BUNDLE_PATH,23);
    }

    public static Message invalidPayloadToArgumentsParameter(String nullPayloadParameterValue)
    {
        return factory.createMessage(BUNDLE_PATH, 24, nullPayloadParameterValue);
    }

    public static Message invalidOrMissingNamespace(QName serviceQName,
                                                    List<QName> probableServices,
                                                    List<QName> allServices)
    {
        return factory.createMessage(BUNDLE_PATH, 25, String.valueOf(serviceQName),
            String.valueOf(probableServices), String.valueOf(allServices));
    }

    public static Message onlyServiceOrClientClassIsValid()
    {
        return factory.createMessage(BUNDLE_PATH, 26);
    }

    public static Message couldNotFindEndpoint(QName endpointNameThatCannotBeFound,
                                               List<QName> availableEndpoingNames)
    {
        return factory.createMessage(BUNDLE_PATH, 27, String.valueOf(endpointNameThatCannotBeFound),
                                     String.valueOf(availableEndpoingNames));
    }
}


