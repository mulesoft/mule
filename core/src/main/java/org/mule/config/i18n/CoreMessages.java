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

import org.mule.AbstractExceptionListener;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.model.EntryPointResolver;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.routing.InboundRouterCollection;
import org.mule.api.routing.OutboundRouter;
import org.mule.api.routing.ResponseRouterCollection;
import org.mule.api.service.Service;
import org.mule.api.transaction.Transaction;
import org.mule.api.transformer.Transformer;
import org.mule.config.MuleManifest;
import org.mule.util.ClassUtils;
import org.mule.util.DateUtils;
import org.mule.util.StringMessageUtils;
import org.mule.util.StringUtils;

import java.util.Date;
import java.util.List;

public class CoreMessages extends MessageFactory
{
    private static final CoreMessages factory = new CoreMessages();

    private static final String BUNDLE_PATH = getBundlePath("core");

    public static Message versionNotSet()
    {
        return factory.createMessage(BUNDLE_PATH, 1);
    }

    public static Message serverStartedAt(long startDate)
    {
        return factory.createMessage(BUNDLE_PATH, 2, new Date(startDate));
    }

    public static Message serverShutdownAt(Date date)
    {
        return factory.createMessage(BUNDLE_PATH, 3, date);
    }

    public static Message agentsRunning()
    {
        return factory.createMessage(BUNDLE_PATH, 4);
    }

    public static Message notSet()
    {
        return factory.createMessage(BUNDLE_PATH, 5);
    }

    public static Message version()
    {
        String version = StringUtils.defaultString(MuleManifest.getProductVersion(), notSet().getMessage());
        return factory.createMessage(BUNDLE_PATH, 6, version);
    }

    public static Message shutdownNormally(Date date)
    {
        return factory.createMessage(BUNDLE_PATH, 7, date);
    }

    public static Message serverWasUpForDuration(long duration)
    {
        String formattedDuration = DateUtils.getFormattedDuration(duration);
        return factory.createMessage(BUNDLE_PATH, 8, formattedDuration);
    }

    public static Message configNotFoundUsage()
    {
        return factory.createMessage(BUNDLE_PATH, 9);
    }

    public static Message fatalErrorWhileRunning()
    {
        return factory.createMessage(BUNDLE_PATH, 10);
    }

    public static Message rootStackTrace()
    {
        return factory.createMessage(BUNDLE_PATH, 11);
    }

    public static Message exceptionStackIs()
    {
        return factory.createMessage(BUNDLE_PATH, 12);
    }

    public static Message messageIsOfType(Class type)
    {
        return factory.createMessage(BUNDLE_PATH, 18, ClassUtils.getSimpleName(type));
    }

    public static Message fatalErrorInShutdown()
    {
        return factory.createMessage(BUNDLE_PATH, 20);
    }

    public static Message normalShutdown()
    {
        return factory.createMessage(BUNDLE_PATH, 21);
    }

    public static Message none()
    {
        return factory.createMessage(BUNDLE_PATH, 22);
    }

    public static Message notClustered()
    {
        return factory.createMessage(BUNDLE_PATH, 23);
    }

    public static Message failedToRouterViaEndpoint(ImmutableEndpoint endpoint)
    {
        return factory.createMessage(BUNDLE_PATH, 30, endpoint);
    }

    public static Message cannotUseDisposedConnector()
    {
        return factory.createMessage(BUNDLE_PATH, 32);
    }

    public static Message connectorCausedError()
    {
        return connectorCausedError(null);
    }

    public static Message connectorCausedError(Object connector)
    {
        return factory.createMessage(BUNDLE_PATH, 33, connector);
    }

    public static Message endpointIsNullForListener()
    {
        return factory.createMessage(BUNDLE_PATH, 34);
    }

    public static Message listenerAlreadyRegistered(EndpointURI endpointUri)
    {
        return factory.createMessage(BUNDLE_PATH, 35, endpointUri);
    }

    public static Message objectAlreadyInitialised(String name)
    {
        return factory.createMessage(BUNDLE_PATH, 37, name);
    }

    public static Message componentCausedErrorIs(Object component)
    {
        return factory.createMessage(BUNDLE_PATH, 38, component);
    }

    public static Message objectFailedToInitialise(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 40, string);
    }

    public static Message failedToStop(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 41, string);
    }

    public static Message failedToStart(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 42, string);
    }

    public static Message proxyPoolTimedOut()
    {
        return factory.createMessage(BUNDLE_PATH, 43);
    }

    public static Message failedToGetPooledObject()
    {
        return factory.createMessage(BUNDLE_PATH, 44);
    }

    public static Message objectIsNull(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 45, string);
    }

    public static Message componentNotRegistered(String name)
    {
        return factory.createMessage(BUNDLE_PATH, 46, name);
    }

    public static Message failedtoRegisterOnEndpoint(String name, Object endpointURI)
    {
        return factory.createMessage(BUNDLE_PATH, 47, name, endpointURI);
    }

    public static Message failedToUnregister(String name, Object endpointURI)
    {
        return factory.createMessage(BUNDLE_PATH, 48, name, endpointURI);
    }

    public static Message endpointIsMalformed(String endpoint)
    {
        return factory.createMessage(BUNDLE_PATH, 51, endpoint);
    }

    public static Message transformFailedBeforeFilter()
    {
        return factory.createMessage(BUNDLE_PATH, 52);
    }

    public static Message transformUnexpectedType(Class class1, Class returnClass)
    {
        return factory.createMessage(BUNDLE_PATH, 53, ClassUtils.getSimpleName(class1), ClassUtils.getSimpleName(returnClass));
    }

    public static Message transformOnObjectUnsupportedTypeOfEndpoint(String name,
                                                                     Class class1,
                                                                     ImmutableEndpoint endpoint)
    {
        return factory.createMessage(BUNDLE_PATH, 54, name, StringMessageUtils.toString(class1),
                (endpoint != null ? endpoint.getEndpointURI() : null));
    }

    public static Message transformFailedFrom(Class clazz)
    {
        return factory.createMessage(BUNDLE_PATH, 55, clazz);
    }

    public static Message encryptionStrategyNotSet()
    {
        return factory.createMessage(BUNDLE_PATH, 56);
    }

    public static Message failedToLoadTransformer(String direction, String transformer)
    {
        return factory.createMessage(BUNDLE_PATH, 57, direction, transformer);
    }

    public static Message failedToLoad(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 58, string);
    }

    public static Message messageNotSupportedByAdapter(String string, String string2)
    {
        return factory.createMessage(BUNDLE_PATH, 59, string, string2);
    }

    public static Message tooManyAcceptableMethodsOnObjectForTypes(Object object, Object types)
    {
        return factory.createMessage(BUNDLE_PATH, 60, StringMessageUtils.toString(object), StringMessageUtils.toString(types));
    }

    public static Message cannotSetPropertyOnObjectWithParamType(String property, Class class1, Class class2)
    {
        return factory.createMessage(BUNDLE_PATH, 61, property, StringMessageUtils.toString(class1),
                StringMessageUtils.toString(class2));
    }

    public static Message noComponentForEndpoint()
    {
        return factory.createMessage(BUNDLE_PATH, 64);
    }

    public static Message failedToCreate(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 65, string);
    }

    public static Message noCorrelationId()
    {
        return factory.createMessage(BUNDLE_PATH, 66);
    }

    public static Object failedToDispose(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 67, string);
    }

    public static Message failedToInvoke(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 68, string);
    }

    public static Message cannotReadPayloadAsBytes(String type)
    {
        return factory.createMessage(BUNDLE_PATH, 69, type);
    }

    public static Message cannotReadPayloadAsString(String type)
    {
        return factory.createMessage(BUNDLE_PATH, 70, type);
    }

    public static Message routingFailedOnEndpoint(Service service, ImmutableEndpoint endpoint)
    {
        EndpointURI endpointURI = null;
        if (endpoint != null)
        {
            endpointURI = endpoint.getEndpointURI();
        }
        return factory.createMessage(BUNDLE_PATH, 72, service.getName(), endpointURI);
    }

    public static Message cannotInstanciateFinder(String serviceFinder)
    {
        return factory.createMessage(BUNDLE_PATH, 73, serviceFinder);
    }

    public static Message failedToCreateObjectWith(String string, Object arg)
    {
        return factory.createMessage(BUNDLE_PATH, 74, string, arg);
    }

    public static Message objectNotSetInService(Object object, Object service)
    {
        return factory.createMessage(BUNDLE_PATH, 75, object, service);
    }

    public static Message objectNotFound(String object)
    {
        return factory.createMessage(BUNDLE_PATH, 76, object);
    }

    public static Message objectNotFound(String type, String object)
    {
        return factory.createMessage(BUNDLE_PATH, 76, type + ": " + object);
    }

    public static Message transactionMarkedForRollback()
    {
        return factory.createMessage(BUNDLE_PATH, 77);
    }

    public static Message transactionCannotBindToNullKey()
    {
        return factory.createMessage(BUNDLE_PATH, 78);
    }

    public static Message transactionCannotBindNullResource()
    {
        return factory.createMessage(BUNDLE_PATH, 79);
    }

    public static Message transactionSingleResourceOnly()
    {
        return factory.createMessage(BUNDLE_PATH, 80);
    }

    public static Message noCurrentEventForTransformer()
    {
        return factory.createMessage(BUNDLE_PATH, 81);
    }

    public static Message objectNotRegistered(String type, String name)
    {
        return factory.createMessage(BUNDLE_PATH, 82, type, name);
    }

    public static Message failedToSetPropertiesOn(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 83, string);
    }

    public static Message failedToCreateConnectorFromUri(EndpointURI uri)
    {
        return factory.createMessage(BUNDLE_PATH, 84, uri);
    }

    public static Message initialisationFailure(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 85, string);
    }

    public static Message failedToCreateEndpointFromLocation(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 87, string);
    }

    public static Message managerAlreadyStarted()
    {
        return factory.createMessage(BUNDLE_PATH, 88);
    }

    public static Message noEndpointsForRouter()
    {
        return factory.createMessage(BUNDLE_PATH, 89);
    }

    public static Message responseTimedOutWaitingForId(int timeout, Object id)
    {
        return factory.createMessage(BUNDLE_PATH, 90, String.valueOf(timeout), id);
    }

    public static Message failedToRecevieWithTimeout(Object endpoint, long timeout)
    {
        return factory.createMessage(BUNDLE_PATH, 93, endpoint, String.valueOf(timeout));
    }

    public static Message failedToWriteMessageToStore(Object id, String storeName)
    {
        return factory.createMessage(BUNDLE_PATH, 94, id, storeName);
    }

    public static Message failedToReadFromStore(String absolutePath)
    {
        return factory.createMessage(BUNDLE_PATH, 95, absolutePath);
    }

    public static Message cannotStartTransaction(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 96, string);
    }

    public static Message transactionCommitFailed()
    {
        return factory.createMessage(BUNDLE_PATH, 97);
    }

    public static Message transactionRollbackFailed()
    {
        return factory.createMessage(BUNDLE_PATH, 98);
    }

    public static Message transactionCannotReadState()
    {
        return factory.createMessage(BUNDLE_PATH, 99);
    }

    public static Message transactionResourceAlreadyListedForKey(Object key)
    {
        return factory.createMessage(BUNDLE_PATH, 100, key);
    }

    public static Message noOutboundRouterSetOn(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 101, string);
    }

    public static Message transactionAvailableButActionIs(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 103, string);
    }

    public static Message transactionNotAvailableButActionIs(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 104, string);
    }

    public static Message noCatchAllEndpointSet()
    {
        return factory.createMessage(BUNDLE_PATH, 105);
    }

    public static Message interruptedQueuingEventFor(Object object)
    {
        return factory.createMessage(BUNDLE_PATH, 106, object);
    }

    public static Message transactionCannotUnbind()
    {
        return factory.createMessage(BUNDLE_PATH, 107);
    }

    public static Message transactionAlreadyBound()
    {
        return factory.createMessage(BUNDLE_PATH, 108);
    }

    public static Message methodWithParamsNotFoundOnObject(String method, Object class1, Class class2)
    {
        return factory.createMessage(BUNDLE_PATH, 109, method, StringMessageUtils.toString(class1),
                StringMessageUtils.toString(class2));
    }

    public static Message transformFailed(String from, String to)
    {
        return factory.createMessage(BUNDLE_PATH, 110, from, to);
    }

    public static Message cryptoFailure()
    {
        return factory.createMessage(BUNDLE_PATH, 112);
    }

    public static Message schemeNotCompatibleWithConnector(String scheme, Class expectedClass)
    {
        return factory.createMessage(BUNDLE_PATH, 115, scheme, expectedClass);
    }

    public static Message noEntryPointFoundWithArgs(Object object, Object args)
    {
        return factory.createMessage(BUNDLE_PATH, 116, StringMessageUtils.toString(object), StringMessageUtils.toString(args));
    }

    public static Message authNoSecurityProvider(String providerName)
    {
        return factory.createMessage(BUNDLE_PATH, 117, providerName);
    }

    public static Message transactionCanOnlyBindToResources(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 120, string);
    }

    public static Message cannotLoadFromClasspath(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 122, string);
    }

    public static Message failedToReadPayload()
    {
        return factory.createMessage(BUNDLE_PATH, 124);
    }

    public static Message endpointNotFound(String endpoint)
    {
        return factory.createMessage(BUNDLE_PATH, 126, endpoint);
    }

    public static Message eventProcessingFailedFor(String name)
    {
        return factory.createMessage(BUNDLE_PATH, 127, name);
    }

    public static Message failedToDispatchToReplyto(ImmutableEndpoint endpoint)
    {
        return factory.createMessage(BUNDLE_PATH, 128, endpoint);
    }

    public static Message authTypeNotRecognised(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 131, string);
    }

    public static Message authSecurityManagerNotSet()
    {
        return factory.createMessage(BUNDLE_PATH, 132);
    }

    public static Message authSetButNoContext(String name)
    {
        return factory.createMessage(BUNDLE_PATH, 133, name);
    }

    public static Message authDeniedOnEndpoint(EndpointURI endpointURI)
    {
        return factory.createMessage(BUNDLE_PATH, 134, endpointURI);
    }

    public static Message authFailedForUser(Object user)
    {
        return factory.createMessage(BUNDLE_PATH, 135, user);
    }

    public static Message authEndpointMustSendOrReceive()
    {
        return factory.createMessage(BUNDLE_PATH, 136);
    }

    public static Message transactionManagerAlreadySet()
    {
        return factory.createMessage(BUNDLE_PATH, 140);
    }

    public static Message failedToCreateManagerInstance(String className)
    {
        return factory.createMessage(BUNDLE_PATH, 144, className);
    }

    public static Message failedToClone(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 145, string);
    }

    public static Message exceptionOnConnectorNoExceptionListener(String name)
    {
        return factory.createMessage(BUNDLE_PATH, 146, name);
    }

    public static Message uniqueIdNotSupportedByAdapter(String name)
    {
        return factory.createMessage(BUNDLE_PATH, 147, name);
    }

    public static Message serverNotificationManagerNotEnabled()
    {
        return factory.createMessage(BUNDLE_PATH, 150);
    }

    public static Message failedToScheduleWork()
    {
        return factory.createMessage(BUNDLE_PATH, 151);
    }

    public static Message authNoCredentials()
    {
        return factory.createMessage(BUNDLE_PATH, 152);
    }

    public static Message valueIsInvalidFor(String value, String parameter)
    {
        return factory.createMessage(BUNDLE_PATH, 154, value, parameter);
    }

    public static Message connectorWithProtocolNotRegistered(String scheme)
    {
        return factory.createMessage(BUNDLE_PATH, 156, scheme);
    }

    public static Message propertyIsNotSupportedType(String property, Class expected, Class actual)
    {
        return factory.createMessage(BUNDLE_PATH, 157, property, StringMessageUtils.toString(expected),
                StringMessageUtils.toString(actual));
    }

    public static Message propertyIsNotSupportedType(String property, Class[] expected, Class actual)
    {
        return factory.createMessage(BUNDLE_PATH, 157, property, StringMessageUtils.toString(expected),
                StringMessageUtils.toString(actual));
    }

    public static Message containerAlreadyRegistered(String name)
    {
        return factory.createMessage(BUNDLE_PATH, 155, name);
    }

    public static Message resourceManagerNotStarted()
    {
        return factory.createMessage(BUNDLE_PATH, 161);
    }

    public static Message resourceManagerDirty()
    {
        return factory.createMessage(BUNDLE_PATH, 162);
    }

    public static Message resourceManagerNotReady()
    {
        return factory.createMessage(BUNDLE_PATH, 163);
    }

    public static Message reconnectStrategyFailed(Class strategy, String description)
    {
        return factory.createMessage(BUNDLE_PATH, 164, StringMessageUtils.toString(strategy), description);
    }

    public static Message cannotSetObjectOnceItHasBeenSet(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 165, string);
    }

    public static Message eventTypeNotRecognised(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 166, string);
    }

    public static Message componentIsStopped(String name)
    {
        return factory.createMessage(BUNDLE_PATH, 167, name);
    }

    public static Message propertyIsNotSetOnEvent(String property)
    {
        return factory.createMessage(BUNDLE_PATH, 168, property);
    }

    public static Message descriptorAlreadyExists(String name)
    {
        return factory.createMessage(BUNDLE_PATH, 171, name);
    }

    public static Message failedToInvokeRestService(String service)
    {
        return factory.createMessage(BUNDLE_PATH, 172, service);
    }

    public static Message authNoEncryptionStrategy(String strategyName)
    {
        return factory.createMessage(BUNDLE_PATH, 174, strategyName);
    }

    public static Message headerMalformedValueIs(String header, String value)
    {
        return factory.createMessage(BUNDLE_PATH, 175, header, value);
    }

    public static Message transformOnObjectNotOfSpecifiedType(String name, Object expectedType)
    {
        return factory.createMessage(BUNDLE_PATH, 177, name, expectedType);
    }

    public static Message cannotUseTxAndRemoteSync()
    {
        return factory.createMessage(BUNDLE_PATH, 178);
    }

    public static Message failedToBuildMessage()
    {
        return factory.createMessage(BUNDLE_PATH, 180);
    }

    public static Message propertiesNotSet(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 183, string);
    }

    public static Message objectNotOfCorrectType(Class actualClass, Class[] expectedClass)
    {
        return factory.createMessage(BUNDLE_PATH, 185, StringMessageUtils.toString(actualClass),
                StringMessageUtils.toString(expectedClass));
    }

    public static Message objectNotOfCorrectType(Class actualClass, Class expectedClass)
    {
        return factory.createMessage(BUNDLE_PATH, 185, StringMessageUtils.toString(actualClass),
                StringMessageUtils.toString(expectedClass));
    }

    public static Message failedToConvertStringUsingEncoding(String encoding)
    {
        return factory.createMessage(BUNDLE_PATH, 188, encoding);
    }

    public static Message propertyHasInvalidValue(String property, Object value)
    {
        return factory.createMessage(BUNDLE_PATH, 189, property, value);
    }

    public static Message schemeCannotChangeForRouter(String scheme, String scheme2)
    {
        return factory.createMessage(BUNDLE_PATH, 192, scheme, scheme2);
    }

    public static Message days()
    {
        return factory.createMessage(BUNDLE_PATH, 193);
    }

    public static Message hours()
    {
        return factory.createMessage(BUNDLE_PATH, 194);
    }

    public static Message minutes()
    {
        return factory.createMessage(BUNDLE_PATH, 195);
    }

    public static Message seconds()
    {
        return factory.createMessage(BUNDLE_PATH, 196);
    }

    public static Message templateCausedMalformedEndpoint(String uri, String newUri)
    {
        return factory.createMessage(BUNDLE_PATH, 197, uri, newUri);
    }

    public static Message couldNotDetermineDestinationComponentFromEndpoint(String endpoint)
    {
        return factory.createMessage(BUNDLE_PATH, 198, endpoint);
    }

    public static Message sessionValueIsMalformed(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 201, string);
    }

    public static Message streamingFailedNoStream()
    {
        return factory.createMessage(BUNDLE_PATH, 205);
    }

    public static Message connectorSchemeIncompatibleWithEndpointScheme(Object expected, Object actual)
    {
        return factory.createMessage(BUNDLE_PATH, 206, expected, actual);
    }

    public static Message failedToReadAttachment(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 207, string);
    }

    public static Message failedToInitSecurityProvider(String providerClass)
    {
        return factory.createMessage(BUNDLE_PATH, 208, providerClass);
    }

    public static Message streamingNotSupported(String protocol)
    {
        return factory.createMessage(BUNDLE_PATH, 209, protocol);
    }

    public static Message streamingComponentMustHaveOneEndpoint(String name)
    {
        return factory.createMessage(BUNDLE_PATH, 210, name);
    }

    public static Message streamingFailedForEndpoint(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 212, string);
    }

    public static Message streamingEndpointsDoNotSupportTransformers()
    {
        return factory.createMessage(BUNDLE_PATH, 213);
    }

    public static Message streamingEndpointsMustBeUsedWithStreamingModel()
    {
        return factory.createMessage(BUNDLE_PATH, 214);
    }

    public static Message tooManyMatchingMethodsOnObjectWhichReturn(Object object, Object returnType)
    {
        return factory.createMessage(BUNDLE_PATH, 216, StringMessageUtils.toString(object),
                StringMessageUtils.toString(returnType));
    }

    public static Message failedToSetProxyOnService(Object proxy, Class routerClass)
    {
        return factory.createMessage(BUNDLE_PATH, 217, proxy, routerClass);
    }

    public static Message mustSetMethodNamesOnBinding()
    {
        return factory.createMessage(BUNDLE_PATH, 218);
    }

    public static Message cannotFindBindingForMethod(String name)
    {
        return factory.createMessage(BUNDLE_PATH, 219, name);
    }

    public static Message noMatchingMethodsOnObjectReturning(Object object, Class returnType)
    {
        return factory.createMessage(BUNDLE_PATH, 220, StringMessageUtils.toString(object),
                StringMessageUtils.toString(returnType));
    }

    public static Message moreThanOneConnectorWithProtocol(String protocol)
    {
        return factory.createMessage(BUNDLE_PATH, 221, protocol);
    }

    public static Message failedToGetOutputStream()
    {
        return factory.createMessage(BUNDLE_PATH, 223);
    }

    public static Message noEntryPointFoundForNoArgsMethod(final Object component, final String methodName)
    {
        return factory.createMessage(BUNDLE_PATH, 224, component, methodName);
    }

    public static Message noDelegateClassAndMethodProvidedForNoArgsWrapper()
    {
        return factory.createMessage(BUNDLE_PATH, 225);
    }

    public static Message noDelegateClassIfDelegateInstanceSpecified()
    {
        return factory.createMessage(BUNDLE_PATH, 226);
    }

    public static Message noServiceTransportDescriptor(String protocol)
    {
        return factory.createMessage(BUNDLE_PATH, 227, protocol);
    }

    public static Message failedToInvokeLifecycle(String phaseName, Object object)
    {
        return factory.createMessage(BUNDLE_PATH, 228, phaseName, object);
    }

    public static Message unrecognisedServiceType(String type)
    {
        return factory.createMessage(BUNDLE_PATH, 229, type);
    }

    public static Message serviceFinderCantFindService(String name)
    {
        return factory.createMessage(BUNDLE_PATH, 230, name);
    }

    public static Message inboundRouterMustUseInboundEndpoints(InboundRouterCollection router,
                                                               ImmutableEndpoint endpoint)
    {
        return factory.createMessage(BUNDLE_PATH, 232, endpoint, router);
    }

    public static Message outboundRouterMustUseOutboudEndpoints(OutboundRouter router, ImmutableEndpoint endpoint)
    {
        return factory.createMessage(BUNDLE_PATH, 233, endpoint, router);
    }

    public static Message responseRouterMustUseInboundEndpoints(ResponseRouterCollection router,
                                                                ImmutableEndpoint endpoint)
    {
        return factory.createMessage(BUNDLE_PATH, 234, endpoint, router);
    }

    public static Message exceptionListenerMustUseOutboundEndpoint(AbstractExceptionListener exceptionListener,
                                                                   ImmutableEndpoint endpoint)
    {
        return factory.createMessage(BUNDLE_PATH, 235, endpoint, exceptionListener);
    }

    /**
     * Returns a message that is a product informatin.
     *
     * @return message
     */
    public static Message productInformation()
    {
        String notset = CoreMessages.notSet().getMessage();
        return factory.createMessage(BUNDLE_PATH, 236, StringUtils.defaultString(MuleManifest.getProductDescription(), notset),
                StringUtils.defaultString(MuleManifest.getProductVersion(), notset), StringUtils.defaultString(
                MuleManifest.getVendorName(), notset)
                + " "
                + StringUtils.defaultString(
                MuleManifest.getVendorUrl(),
                notset));
    }

    public static Message noTransformerFoundForMessage(Class input, Class output)
    {
        return factory.createMessage(BUNDLE_PATH, 237, input.getName(), output.getName());
    }

    public static Message errorReadingStream()
    {
        return factory.createMessage(BUNDLE_PATH, 238);
    }

    public static Message noEntryPointFoundForNoArgsMethodUsingResolver(final Object component,
                                                                        final String methodName,
                                                                        EntryPointResolver resolver)
    {
        return factory.createMessage(BUNDLE_PATH, 239, methodName, component, resolver);
    }

    public static Message noEntryPointFoundWithArgsUsingResolver(Object object, Object args, EntryPointResolver resolver)
    {
        return factory.createMessage(BUNDLE_PATH, 240, StringMessageUtils.toString(object), StringMessageUtils.toString(args),
                resolver);
    }

    public static Message noMatchingMethodsOnObjectReturningUsingResolver(Object object,
                                                                          Class returnType,
                                                                          EntryPointResolver resolver)
    {
        return factory.createMessage(BUNDLE_PATH, 241, StringMessageUtils.toString(object), returnType.getClass().getName(),
                resolver);
    }

    public static Message tooManyAcceptableMethodsOnObjectUsingResolverForTypes(Object object,
                                                                                Object types,
                                                                                EntryPointResolver resolver)
    {
        return factory.createMessage(BUNDLE_PATH, 242, StringMessageUtils.toString(object), StringMessageUtils.toString(types),
                resolver);
    }

    public static Message tooManyMatchingMethodsOnObjectUsingResolverWhichReturn(Object object,
                                                                                 Object returnType,
                                                                                 EntryPointResolver resolver)
    {
        return factory.createMessage(BUNDLE_PATH, 243, StringMessageUtils.toString(returnType),
                StringMessageUtils.toString(object), resolver);
    }

    public static Message objectDoesNotImplementInterface(Object object, Class interfaceClass)
    {
        return factory.createMessage(BUNDLE_PATH, 244, StringMessageUtils.toString(object), interfaceClass);
    }

    public static Message invocationSuccessfulCantSetError()
    {
        return factory.createMessage(BUNDLE_PATH, 245);
    }

    public static Message noMatchingMethodsOnObjectCalledUsingResolver(Object object,
                                                                       String methodName,
                                                                       EntryPointResolver resolver)
    {
        return factory.createMessage(BUNDLE_PATH, 246, StringMessageUtils.toString(object), methodName, resolver);
    }

    public static Message noJtaTransactionAvailable(final Thread callingThread)
    {
        return factory.createMessage(BUNDLE_PATH, 247, StringUtils.defaultString(callingThread.toString()));
    }

    public static Message notMuleXaTransaction(Object tx)
    {
        return factory.createMessage(BUNDLE_PATH, 248, tx.getClass());
    }

    public static Message noServiceQueueTimeoutSet(Service service)
    {
        return factory.createMessage(BUNDLE_PATH, 249, service);
    }

    public static Message failedToProcessExtractorFunction(String name)
    {
        return factory.createMessage(BUNDLE_PATH, 250, name);
    }

    public static Message expressionEvaluatorNotRegistered(String key)
    {
        return factory.createMessage(BUNDLE_PATH, 251, key);
    }

    public static Message objectAlreadyExists(String key)
    {
        return factory.createMessage(BUNDLE_PATH, 252, key);
    }

    public static Message noMuleTransactionAvailable()
    {
        return factory.createMessage(BUNDLE_PATH, 253);
    }

    public static Message objectAlreadyRegistered(String name, Object origObject, Object newObject)
    {
        return factory.createMessage(BUNDLE_PATH, 254, name, origObject + "." + origObject.getClass(), newObject + "."
                + newObject.getClass());
    }

    public static Message transformerNotImplementDiscoverable(Transformer transformer)
    {
        return factory.createMessage(BUNDLE_PATH, 255, transformer);
    }

    public static Message transformHasMultipleMatches(Class input,
                                                      Class output,
                                                      Transformer transformer1,
                                                      Transformer transformer2)
    {
        return factory.createMessage(BUNDLE_PATH, 256, new Object[]{input, output,
                transformer1.getName() + "(" + transformer1.getClass() + ")",
                transformer2.getName() + "(" + transformer2.getClass() + ")"});
    }

    public static Message configurationBuilderSuccess(ConfigurationBuilder configurationBuilder, int numResources)
    {
        return factory.createMessage(BUNDLE_PATH, 257, configurationBuilder.getClass().getName(), new Integer(numResources));
    }

    public static Message configurationBuilderSuccess(ConfigurationBuilder configurationBuilder, String resources)
    {
        return factory.createMessage(BUNDLE_PATH, 258, configurationBuilder.getClass().getName(), resources);
    }

    public static Message configurationBuilderNoMatching(String resource)
    {
        return factory.createMessage(BUNDLE_PATH, 259, resource);
    }

    public static Message configurationBuilderError(ConfigurationBuilder configurationBuilder)
    {
        return factory.createMessage(BUNDLE_PATH, 260, StringMessageUtils.toString(configurationBuilder.getClass()));
    }

    public static Message nestedRetry()
    {
        return factory.createMessage(BUNDLE_PATH, 261);
    }

    public static Message expressionEvaluatorReturnedNull(String name, String expr)
    {
        return factory.createMessage(BUNDLE_PATH, 263, name, expr);
    }

    public static Message expressionInvalidForProperty(String property, String expr)
    {
        return factory.createMessage(BUNDLE_PATH, 264, property, expr);
    }

    public static Message expressionMalformed(String expr, String eval)
    {
        return factory.createMessage(BUNDLE_PATH, 265, expr, eval);
    }

    public static Message correlationTimedOut(Object groupId)
    {
        return factory.createMessage(BUNDLE_PATH, 266, groupId);
    }

    public static Message transformerInvalidReturnType(Class clazz, String transformerName)
    {
        return factory.createMessage(BUNDLE_PATH, 267, clazz, transformerName);
    }

    /**
     * 
     * @deprecated Use more specific {@link #commitTxButNoResource} or
     * {@link #rollbackTxButNoResource}
     */
    @Deprecated
    public static Message noBindingResource()
    {
        return factory.createMessage(BUNDLE_PATH, 268);
    }


    public static Message transactionFactoryIsMandatory(String action)
    {
        return factory.createMessage(BUNDLE_PATH, 269, action);
    }

    public static Message failedToCreateProxyFor(Object target)
    {
        return factory.createMessage(BUNDLE_PATH, 270, target);
    }

    public static Message authorizationAttemptFailed()
    {
        return factory.createMessage(BUNDLE_PATH, 271);
    }

    public static Message retryPolicyExhausted(RetryPolicyTemplate policy)
    {
        return factory.createMessage(BUNDLE_PATH, 272, policy);
    }

    public static Message notConnectedYet(String what)
    {
        return factory.createMessage(BUNDLE_PATH, 273, what);
    }

    public static Message stopPausedSedaServiceNonPeristentQueueMessageLoss(int num, Service service)
    {
        return factory.createMessage(BUNDLE_PATH, 274, num, service.getName());
    }

    public static Message splitMessageNoEndpointMatch(List endpoints, Object messagePart)
    {
        return factory.createMessage(BUNDLE_PATH, 275, StringMessageUtils.toString(endpoints), messagePart);
    }

    public static Message expressionResultWasNull(String expression)
    {
        return factory.createMessage(BUNDLE_PATH, 276, expression);
    }

    public static Message propertyDoesNotExistOnObject(String property, Object object)
    {
        return factory.createMessage(BUNDLE_PATH, 277, property, object);
    }

    public static Message commitTxButNoResource(Transaction tx)
    {
        return factory.createMessage(BUNDLE_PATH, 300, tx);
    }

    public static Message rollbackTxButNoResource(Transaction tx)
    {
        return factory.createMessage(BUNDLE_PATH, 301, tx);
    }
    
    public static Message cannotCopyStreamPayload(String streamType)
    {
        return factory.createMessage(BUNDLE_PATH, 302, streamType);
    }
}
