/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.i18n;

import org.mule.impl.AbstractExceptionListener;
import org.mule.umo.UMOImmutableDescriptor;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.routing.UMOInboundRouterCollection;
import org.mule.umo.routing.UMOOutboundRouter;
import org.mule.umo.routing.UMOResponseRouterCollection;
import org.mule.util.ClassUtils;
import org.mule.util.DateUtils;
import org.mule.util.StringMessageUtils;

import java.util.Date;

public class CoreMessages extends MessageFactory
{
    private static final String BUNDLE_PATH = getBundlePath("core");

    public static Message versionNotSet()
    {
        return createMessage(BUNDLE_PATH, 1);
    }

    public static Message serverStartedAt(long startDate)
    {
        return createMessage(BUNDLE_PATH, 2, new Date(startDate));
    }

    public static Message serverShutdownAt(Date date)
    {
        return createMessage(BUNDLE_PATH, 3, date);
    }

    public static Message agentsRunning()
    {
        return createMessage(BUNDLE_PATH, 4);
    }

    public static Message notSet()
    {
        return createMessage(BUNDLE_PATH, 5);
    }

    public static Message version()
    {
        return createMessage(BUNDLE_PATH, 6);
    }

    public static Message shutdownNormally(Date date)
    {
        return createMessage(BUNDLE_PATH, 7, date);
    }

    public static Message serverWasUpForDuration(long duration)
    {
        String formattedDuration = DateUtils.getFormattedDuration(duration);
        return createMessage(BUNDLE_PATH, 8, formattedDuration);
    }

    public static Message configNotFoundUsage()
    {
        return createMessage(BUNDLE_PATH, 9);
    }

    public static Message fatalErrorWhileRunning()
    {
        return createMessage(BUNDLE_PATH, 10);
    }

    public static Message rootStackTrace()
    {
        return createMessage(BUNDLE_PATH, 11);
    }

    public static Message exceptionStackIs()
    {
        return createMessage(BUNDLE_PATH, 12);
    }

    public static Message messageIsOfType(Class type)
    {
        return createMessage(BUNDLE_PATH, 18, ClassUtils.getSimpleName(type));
    }

    public static Message fatalErrorInShutdown()
    {
        return createMessage(BUNDLE_PATH, 20);
    }

    public static Message normalShutdown()
    {
        return createMessage(BUNDLE_PATH, 21);
    }

    public static Message none()
    {
        return createMessage(BUNDLE_PATH, 22);
    }

    public static Message notClustered()
    {
        return createMessage(BUNDLE_PATH, 23);
    }

    public static Message failedToRouterViaEndpoint(UMOImmutableEndpoint endpoint)
    {
        return createMessage(BUNDLE_PATH, 30, endpoint);
    }

    public static Message cannotUseDisposedConnector()
    {
        return createMessage(BUNDLE_PATH, 32);
    }

    public static Message connectorCausedError()
    {
        return connectorCausedError(null);
    }

    public static Message connectorCausedError(Object connector)
    {
        return createMessage(BUNDLE_PATH, 33, connector);
    }

    public static Message endpointIsNullForListener()
    {
        return createMessage(BUNDLE_PATH, 34);
    }

    public static Message listenerAlreadyRegistered(UMOEndpointURI endpointUri)
    {
        return createMessage(BUNDLE_PATH, 35, endpointUri);
    }

    public static Message objectAlreadyInitialised(String name)
    {
        return createMessage(BUNDLE_PATH, 37, name);
    }

    public static Message componentCausedErrorIs(Object component)
    {
        return createMessage(BUNDLE_PATH, 38, component);
    }

    public static Message objectFailedToInitialise(String string)
    {
        return createMessage(BUNDLE_PATH, 40, string);
    }

    public static Message failedToStop(String string)
    {
        return createMessage(BUNDLE_PATH, 41, string);
    }

    public static Message failedToStart(String string)
    {
        return createMessage(BUNDLE_PATH, 42, string);
    }

    public static Message proxyPoolTimedOut()
    {
        return createMessage(BUNDLE_PATH, 43);
    }

    public static Message failedToGetPooledObject()
    {
        return createMessage(BUNDLE_PATH, 44);
    }

    public static Message objectIsNull(String string)
    {
        return createMessage(BUNDLE_PATH, 45, string);
    }

    public static Message componentNotRegistered(String name)
    {
        return createMessage(BUNDLE_PATH, 46, name);
    }

    public static Message failedtoRegisterOnEndpoint(String name, Object endpointURI)
    {
        return createMessage(BUNDLE_PATH, 47, name, endpointURI);
    }

    public static Message failedToUnregister(String name, Object endpointURI)
    {
        return createMessage(BUNDLE_PATH, 48, name, endpointURI);
    }

    public static Message endpointIsMalformed(String endpoint)
    {
        return createMessage(BUNDLE_PATH, 51, endpoint);
    }

    public static Message transformFailedBeforeFilter()
    {
        return createMessage(BUNDLE_PATH, 52);
    }

    public static Message transformUnexpectedType(Class class1, Class returnClass)
    {
        return createMessage(BUNDLE_PATH, 53, ClassUtils.getSimpleName(class1),
            ClassUtils.getSimpleName(returnClass));
    }

    public static Message transformOnObjectUnsupportedTypeOfEndpoint(String name, Class class1,
        UMOImmutableEndpoint endpoint)
    {
        return createMessage(BUNDLE_PATH, 54, name, StringMessageUtils.toString(class1),
            (endpoint != null ? endpoint.getEndpointURI() : null));
    }

    public static Message transformFailedFrom(Class clazz)
    {
        return createMessage(BUNDLE_PATH, 55, clazz);
    }

    public static Message encryptionStrategyNotSet()
    {
        return createMessage(BUNDLE_PATH, 56);
    }

    public static Message failedToLoadTransformer(String direction, String transformer)
    {
        return createMessage(BUNDLE_PATH, 57, direction, transformer);
    }

    public static Message failedToLoad(String string)
    {
        return createMessage(BUNDLE_PATH, 58, string);
    }

    public static Message messageNotSupportedByAdapter(String string, String string2)
    {
        return createMessage(BUNDLE_PATH, 59, string, string2);
    }

    public static Message tooManyAcceptableMethodsOnObjectForTypes(Object object, Object types)
    {
        return createMessage(BUNDLE_PATH, 60, StringMessageUtils.toString(object),
            StringMessageUtils.toString(types));
    }

    public static Message cannotSetPropertyOnObjectWithParamType(String property,
        Class class1, Class class2)
    {
        return createMessage(BUNDLE_PATH, 61, property, StringMessageUtils.toString(class1),
            StringMessageUtils.toString(class2));
    }

    public static Message noComponentForEndpoint()
    {
        return createMessage(BUNDLE_PATH, 64);
    }

    public static Message failedToCreate(String string)
    {
        return createMessage(BUNDLE_PATH, 65, string);
    }

    public static Message noCorrelationId()
    {
        return createMessage(BUNDLE_PATH, 66);
    }

    public static Object failedToDispose(String string)
    {
        return createMessage(BUNDLE_PATH, 67, string);
    }

    public static Message failedToInvoke(String string)
    {
        return createMessage(BUNDLE_PATH, 68, string);
    }

    public static Message cannotReadPayloadAsBytes(String type)
    {
        return createMessage(BUNDLE_PATH, 69, type);
    }

    public static Message cannotReadPayloadAsString(String type)
    {
        return createMessage(BUNDLE_PATH, 70, type);
    }

    public static Message routingFailedOnEndpoint(String name, Object endpointURI)
    {
        return createMessage(BUNDLE_PATH, 72, name, endpointURI);
    }

    public static Message cannotInstanciateFinder(String serviceFinder)
    {
        return createMessage(BUNDLE_PATH, 73, serviceFinder);
    }

    public static Message failedToCreateObjectWith(String string, Object arg)
    {
        return createMessage(BUNDLE_PATH, 74, string, arg);
    }

    public static Message objectNotSetInService(Object object, Object service)
    {
        return createMessage(BUNDLE_PATH, 75, object, service);
    }

    public static Message objectNotFound(String object)
    {
        return createMessage(BUNDLE_PATH, 76, object);
    }

    public static Message transactionMarkedForRollback()
    {
        return createMessage(BUNDLE_PATH, 77);
    }

    public static Message transactionCannotBindToNullKey()
    {
        return createMessage(BUNDLE_PATH, 78);
    }

    public static Message transactionCannotBindNullResource()
    {
        return createMessage(BUNDLE_PATH, 79);
    }

    public static Message transactionSingleResourceOnly()
    {
        return createMessage(BUNDLE_PATH, 80);
    }

    public static Message noCurrentEventForTransformer()
    {
        return createMessage(BUNDLE_PATH, 81);
    }

    public static Message objectNotRegistered(String type, String name)
    {
        return createMessage(BUNDLE_PATH, 82, type, name);
    }

    public static Message failedToSetPropertiesOn(String string)
    {
        return createMessage(BUNDLE_PATH, 83, string);
    }

    public static Message failedToCreateConnectorFromUri(UMOEndpointURI uri)
    {
        return createMessage(BUNDLE_PATH, 84, uri);
    }

    public static Message initialisationFailure(String string)
    {
        return createMessage(BUNDLE_PATH, 85, string);
    }

    public static Message failedToCreateEndpointFromLocation(String string)
    {
        return createMessage(BUNDLE_PATH, 87, string);
    }

    public static Message managerAlreadyStarted()
    {
        return createMessage(BUNDLE_PATH, 88);
    }

    public static Message noEndpointsForRouter()
    {
        return createMessage(BUNDLE_PATH, 89);
    }

    public static Message responseTimedOutWaitingForId(int timeout, Object id)
    {
        return createMessage(BUNDLE_PATH, 90, String.valueOf(timeout), id);
    }

    public static Message failedToRecevieWithTimeout(Object endpoint, long timeout)
    {
        return createMessage(BUNDLE_PATH, 93, endpoint, String.valueOf(timeout));
    }

    public static Message failedToWriteMessageToStore(Object id, String path)
    {
        return createMessage(BUNDLE_PATH, 94, id, path);
    }

    public static Message failedToReadFromStore(String absolutePath)
    {
        return createMessage(BUNDLE_PATH, 95, absolutePath);
    }

    public static Message cannotStartTransaction(String string)
    {
        return createMessage(BUNDLE_PATH, 96, string);
    }

    public static Message transactionCommitFailed()
    {
        return createMessage(BUNDLE_PATH, 97);
    }

    public static Message transactionRollbackFailed()
    {
        return createMessage(BUNDLE_PATH, 98);
    }

    public static Message transactionCannotReadState()
    {
        return createMessage(BUNDLE_PATH, 99);
    }

    public static Message transactionResourceAlreadyListedForKey(Object key)
    {
        return createMessage(BUNDLE_PATH, 100, key);
    }

    public static Message noOutboundRouterSetOn(String string)
    {
        return createMessage(BUNDLE_PATH, 101, string);
    }

    public static Message transactionAvailableButActionIs(String string)
    {
        return createMessage(BUNDLE_PATH, 103, string);
    }

    public static Message transactionNotAvailableButActionIs(String string)
    {
        return createMessage(BUNDLE_PATH, 104, string);
    }

    public static Message noCatchAllEndpointSet()
    {
        return createMessage(BUNDLE_PATH, 105);
    }

    public static Message interruptedQueuingEventFor(Object object)
    {
        return createMessage(BUNDLE_PATH, 106, object);
    }

    public static Message transactionCannotUnbind()
    {
        return createMessage(BUNDLE_PATH, 107);
    }

    public static Message transactionAlreadyBound()
    {
        return createMessage(BUNDLE_PATH, 108);
    }

    public static Message methodWithParamsNotFoundOnObject(String method, Object class1, Class class2)
    {
        return createMessage(BUNDLE_PATH, 109, method, StringMessageUtils.toString(class1),
            StringMessageUtils.toString(class2));
    }

    public static Message transformFailed(String from, String to)
    {
        return createMessage(BUNDLE_PATH, 110, from, to);
    }

    public static Message cryptoFailure()
    {
        return createMessage(BUNDLE_PATH, 112);
    }

    public static Message schemeNotCompatibleWithConnector(String scheme, Class expectedClass)
    {
        return createMessage(BUNDLE_PATH, 115, scheme, expectedClass);
    }

    public static Message noEntryPointFoundWithArgs(Object object, Object args)
    {
        return createMessage(BUNDLE_PATH, 116, StringMessageUtils.toString(object),
            StringMessageUtils.toString(args));
    }

    public static Message authNoSecurityProvider(String providerName)
    {
        return createMessage(BUNDLE_PATH, 117, providerName);
    }

    public static Message transactionCanOnlyBindToResources(String string)
    {
        return createMessage(BUNDLE_PATH, 120, string);
    }

    public static Message cannotLoadFromClasspath(String string)
    {
        return createMessage(BUNDLE_PATH, 122, string);
    }

    public static Message failedToReadPayload()
    {
        return createMessage(BUNDLE_PATH, 124);
    }

    public static Message endpointNotFound(String endpoint)
    {
        return createMessage(BUNDLE_PATH, 126, endpoint);
    }

    public static Message eventProcessingFailedFor(String name)
    {
        return createMessage(BUNDLE_PATH, 127, name);
    }

    public static Message failedToDispatchToReplyto(UMOEndpoint endpoint)
    {
        return createMessage(BUNDLE_PATH, 128, endpoint);
    }

    public static Message authTypeNotRecognised(String string)
    {
        return createMessage(BUNDLE_PATH, 131, string);
    }

    public static Message authSecurityManagerNotSet()
    {
        return createMessage(BUNDLE_PATH, 132);
    }

    public static Message authSetButNoContext(String name)
    {
        return createMessage(BUNDLE_PATH, 133, name);
    }

    public static Message authDeniedOnEndpoint(UMOEndpointURI endpointURI)
    {
        return createMessage(BUNDLE_PATH, 134, endpointURI);
    }

    public static Message authFailedForUser(Object user)
    {
        return createMessage(BUNDLE_PATH, 135, user);
    }

    public static Message authEndpointTypeForFilterMustBe(String expected, String actual)
    {
        return createMessage(BUNDLE_PATH, 136, expected, actual);
    }

    public static Message transactionManagerAlreadySet()
    {
        return createMessage(BUNDLE_PATH, 140);
    }

    public static Message failedToCreateManagerInstance(String className)
    {
        return createMessage(BUNDLE_PATH, 144, className);
    }

    public static Message failedToClone(String string)
    {
        return createMessage(BUNDLE_PATH, 145, string);
    }

    public static Message exceptionOnConnectorNotExceptionListener(String name)
    {
        return createMessage(BUNDLE_PATH, 146, name);
    }

    public static Message uniqueIdNotSupportedByAdapter(String name)
    {
        return createMessage(BUNDLE_PATH, 147, name);
    }

    public static Message serverNotificationManagerNotEnabled()
    {
        return createMessage(BUNDLE_PATH, 150);
    }

    public static Message failedToScheduleWork()
    {
        return createMessage(BUNDLE_PATH, 151);
    }

    public static Message authNoCredentials()
    {
        return createMessage(BUNDLE_PATH, 152);
    }

    public static Message valueIsInvalidFor(String value, String parameter)
    {
        return createMessage(BUNDLE_PATH, 154, value, parameter);
    }

    public static Message connectorWithProtocolNotRegistered(String scheme)
    {
        return createMessage(BUNDLE_PATH, 156, scheme);
    }

    public static Message propertyIsNotSupportedType(String property, Class expected,
        Class actual)
    {
        return createMessage(BUNDLE_PATH, 157, property, StringMessageUtils.toString(expected),
            StringMessageUtils.toString(actual));
    }

    public static Message containerAlreadyRegistered(String name)
    {
        return createMessage(BUNDLE_PATH, 155, name);
    }

    public static Message resourceManagerNotStarted()
    {
        return createMessage(BUNDLE_PATH, 161);
    }

    public static Message resourceManagerDirty()
    {
        return createMessage(BUNDLE_PATH, 162);
    }

    public static Message resourceManagerNotReady()
    {
        return createMessage(BUNDLE_PATH, 163);
    }

    public static Message reconnectStrategyFailed(Class strategy, String description)
    {
        return createMessage(BUNDLE_PATH, 164, StringMessageUtils.toString(strategy), description);
    }

    public static Message cannotSetObjectOnceItHasBeenSet(String string)
    {
        return createMessage(BUNDLE_PATH, 165, string);
    }

    public static Message eventTypeNotRecognised(String string)
    {
        return createMessage(BUNDLE_PATH, 166, string);
    }

    public static Message componentIsStopped(String name)
    {
        return createMessage(BUNDLE_PATH, 167, name);
    }

    public static Object propertyIsNotSetOnEvent(String property)
    {
        return createMessage(BUNDLE_PATH, 168, property);
    }

    public static Message descriptorAlreadyExists(String name)
    {
        return createMessage(BUNDLE_PATH, 171, name);
    }

    public static Message failedToInvokeRestService(String service)
    {
        return createMessage(BUNDLE_PATH, 172, service);
    }

    public static Message authNoEncryptionStrategy(String strategyName)
    {
        return createMessage(BUNDLE_PATH, 174, strategyName);
    }

    public static Message headerMalformedValueIs(String header, String value)
    {
        return createMessage(BUNDLE_PATH, 175, header, value);
    }

    public static Message transformOnObjectNotOfSpecifiedType(String name, Object expectedType)
    {
        return createMessage(BUNDLE_PATH, 177, name, expectedType);
    }

    public static Message cannotUseTxAndRemoteSync()
    {
        return createMessage(BUNDLE_PATH, 178);
    }

    public static Message failedToBuildMessage()
    {
        return createMessage(BUNDLE_PATH, 180);
    }

    public static Message propertiesNotSet(String string)
    {
        return createMessage(BUNDLE_PATH, 183, string);
    }

    public static Message objectNotOfCorrectType(Class actualClass, Class expectedClass)
    {
        return createMessage(BUNDLE_PATH, 185, StringMessageUtils.toString(actualClass),
            StringMessageUtils.toString(expectedClass));
    }

    public static Message failedToConvertStringUsingEncoding(String encoding)
    {
        return createMessage(BUNDLE_PATH, 188, encoding);
    }

    public static Message propertyHasInvalidValue(String property, Object value)
    {
        return createMessage(BUNDLE_PATH, 189, property, value);
    }

    public static Message schemeCannotChangeForRouter(String scheme, String scheme2)
    {
        return createMessage(BUNDLE_PATH, 192, scheme, scheme2);
    }

    public static Message days()
    {
        return createMessage(BUNDLE_PATH, 193);
    }

    public static Message hours()
    {
        return createMessage(BUNDLE_PATH, 194);
    }

    public static Message minutes()
    {
        return createMessage(BUNDLE_PATH, 195);
    }

    public static Message seconds()
    {
        return createMessage(BUNDLE_PATH, 196);
    }

    public static Message templateCausedMalformedEndpoint(String uri, String newUri)
    {
        return createMessage(BUNDLE_PATH, 197, uri, newUri);
    }

    public static Message couldNotDetermineDestinationComponentFromEndpoint(String endpoint)
    {
        return createMessage(BUNDLE_PATH, 198, endpoint);
    }

    public static Message sessionValueIsMalformed(String string)
    {
        return createMessage(BUNDLE_PATH, 201, string);
    }

    public static Message streamingFailedNoStream()
    {
        return createMessage(BUNDLE_PATH, 205);
    }

    public static Message connectorSchemeIncompatibleWithEndpointScheme(Object expected, Object actual)
    {
        return createMessage(BUNDLE_PATH, 206, expected, actual);
    }

    public static Message failedToReadAttachment(String string)
    {
        return createMessage(BUNDLE_PATH, 207, string);
    }

    public static Message failedToInitSecurityProvider(String providerClass)
    {
        return createMessage(BUNDLE_PATH, 208, providerClass);
    }

    public static Message streamingNotSupported(String protocol)
    {
        return createMessage(BUNDLE_PATH, 209, protocol);
    }

    public static Message streamingComponentMustHaveOneEndpoint(String name)
    {
        return createMessage(BUNDLE_PATH, 210, name);
    }

    public static Message streamingFailedForEndpoint(String string)
    {
        return createMessage(BUNDLE_PATH, 212, string);
    }

    public static Message streamingEndpointsDoNotSupportTransformers()
    {
        return createMessage(BUNDLE_PATH, 213);
    }

    public static Message streamingEndpointsMustBeUsedWithStreamingModel()
    {
        return createMessage(BUNDLE_PATH, 214);
    }

    public static Message tooManyMatchingMethodsOnObjectWhichReturn(Object object, Object returnType)
    {
        return createMessage(BUNDLE_PATH, 216, StringMessageUtils.toString(object),
            StringMessageUtils.toString(returnType));
    }

    public static Message failedToSetProxyOnService(Object proxy, Class routerClass)
    {
        return createMessage(BUNDLE_PATH, 217, proxy, routerClass);
    }

    public static Message mustSetMethodNamesOnBinding()
    {
        return createMessage(BUNDLE_PATH, 218);
    }

    public static Message cannotFindBindingForMethod(String name)
    {
        return createMessage(BUNDLE_PATH, 219, name);
    }

    public static Message noMatchingMethodsOnObjectReturning(Object object, Class returnType)
    {
        return createMessage(BUNDLE_PATH, 220, StringMessageUtils.toString(object),
            StringMessageUtils.toString(returnType));
    }

    public static Message moreThanOneConnectorWithProtocol(String protocol)
    {
        return createMessage(BUNDLE_PATH, 221, protocol);
    }

    public static Message failedToGetOutputStream()
    {
        return createMessage(BUNDLE_PATH, 223);
    }

    public static Message noEntryPointFoundForNoArgsMethod(final Object component, final String methodName)
    {
        return createMessage(BUNDLE_PATH, 224, component, methodName);
    }

    public static Message noDelegateClassAndMethodProvidedForNoArgsWrapper()
    {
        return createMessage(BUNDLE_PATH, 225);
    }

    public static Message noDelegateClassIfDelegateInstanceSpecified()
    {
        return createMessage(BUNDLE_PATH, 226);
    }

    public static Message noServiceTransportDescriptor(String protocol)
    {
        return createMessage(BUNDLE_PATH, 227, protocol);
    }

    public static Message failedToInvokeLifecycle(String phaseName, Object object)
    {
        return createMessage(BUNDLE_PATH, 228, phaseName, object);
    }

    public static Message unrecognisedServiceType(String type)
    {
        return createMessage(BUNDLE_PATH, 229, type);
    }

    public static Message serviceFinderCantFindService(String name)
    {
        return createMessage(BUNDLE_PATH, 230, name);
    }

    public static Message modelNameDoesNotMatchModel(UMOImmutableDescriptor descriptor, String modelName)
    {
        return createMessage(BUNDLE_PATH, 231, descriptor.getName(), descriptor.getModelName(), modelName);
    }
    
    //These endpoint errors should go away once we make setting endpoints on routers typesafe
    
    public static Message inboundRouterMustUseInboundEndpoints(UMOInboundRouterCollection router, UMOImmutableEndpoint endpoint)
    {
        return createMessage(BUNDLE_PATH, 232, endpoint, router);
    }
  
    public static Message outboundRouterMustUseOutboudEndpoints(UMOOutboundRouter router, UMOImmutableEndpoint endpoint)
    {
        return createMessage(BUNDLE_PATH, 233, endpoint, router);
    }
    
    public static Message responseRouterMustUseResponseEndpoints(UMOResponseRouterCollection router, UMOImmutableEndpoint endpoint)
    {
        return createMessage(BUNDLE_PATH, 234, endpoint, router);
    }

    public static Message exceptionListenerMustUseOutboundEndpoint(AbstractExceptionListener exceptionListener, UMOImmutableEndpoint endpoint)
    {
        return createMessage(BUNDLE_PATH, 235, endpoint, exceptionListener);
    }

}