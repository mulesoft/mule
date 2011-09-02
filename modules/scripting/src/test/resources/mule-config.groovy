package org.mule.config.builders

import org.mule.api.config.MuleProperties
import org.mule.api.config.ThreadingProfile
import org.mule.api.endpoint.InboundEndpoint
import org.mule.api.endpoint.OutboundEndpoint
import org.mule.api.model.Model
import org.mule.api.service.Service
import org.mule.component.DefaultInterfaceBinding
import org.mule.component.DefaultJavaComponent
import org.mule.config.ChainedThreadingProfile
import org.mule.endpoint.DefaultEndpointFactory
import org.mule.endpoint.EndpointURIEndpointBuilder
import org.mule.exception.DefaultMessagingExceptionStrategy
import org.mule.interceptor.InterceptorStack
import org.mule.model.seda.SedaModel
import org.mule.model.seda.SedaService
import org.mule.object.SingletonObjectFactory
import org.mule.retry.policies.NoRetryPolicyTemplate
import org.mule.routing.ForwardingCatchAllStrategy
import org.mule.routing.MessageFilter
import org.mule.routing.filters.MessagePropertyFilter
import org.mule.routing.filters.PayloadTypeFilter
import org.mule.routing.outbound.OutboundPassThroughRouter
import org.mule.security.MuleSecurityManager
import org.mule.tck.testmodels.fruit.FruitCleaner
import org.mule.tck.testmodels.fruit.Orange
import org.mule.tck.testmodels.mule.TestCompressionTransformer
import org.mule.tck.testmodels.mule.TestConnector
import org.mule.tck.testmodels.mule.TestComponentLifecycleAdapterFactory
import org.mule.tck.testmodels.mule.TestEntryPointResolverSet
import org.mule.tck.testmodels.mule.TestExceptionStrategy
import org.mule.tck.testmodels.mule.TestResponseAggregator
import org.mule.tck.testmodels.mule.TestTransactionManagerFactory
import org.mule.util.store.SimpleMemoryObjectStore
import org.mule.util.queue.QueueManager
import org.mule.util.queue.TransactionalQueueManager
import org.mule.util.store.MuleObjectStoreManager

// Set up defaults / system objects
QueueManager queueManager = new TransactionalQueueManager();
muleContext.registry.registerObject(MuleProperties.OBJECT_QUEUE_MANAGER, queueManager);

muleContext.registry.registerObject(MuleProperties.OBJECT_SECURITY_MANAGER, new MuleSecurityManager());

muleContext.registry.registerObject(MuleProperties.OBJECT_MULE_ENDPOINT_FACTORY, new DefaultEndpointFactory());

MuleObjectStoreManager objectStoreManager = new MuleObjectStoreManager();
muleContext.registry.registerObject(MuleProperties.OBJECT_STORE_MANAGER, objectStoreManager);


ThreadingProfile defaultThreadingProfile = new ChainedThreadingProfile();
defaultThreadingProfile.setThreadWaitTimeout(30);
defaultThreadingProfile.setMaxThreadsActive(10);
defaultThreadingProfile.setMaxThreadsIdle(10);
defaultThreadingProfile.setMaxBufferSize(0);
defaultThreadingProfile.setThreadTTL(60000);
defaultThreadingProfile.setPoolExhaustedAction(ThreadingProfile.WHEN_EXHAUSTED_RUN);
muleContext.registry.registerObject(MuleProperties.OBJECT_DEFAULT_SERVICE_THREADING_PROFILE,
            new ChainedThreadingProfile(defaultThreadingProfile));
muleContext.registry.registerObject(MuleProperties.OBJECT_DEFAULT_MESSAGE_RECEIVER_THREADING_PROFILE,
            new ChainedThreadingProfile(defaultThreadingProfile));
muleContext.registry.registerObject(MuleProperties.OBJECT_DEFAULT_MESSAGE_DISPATCHER_THREADING_PROFILE,
            new ChainedThreadingProfile(defaultThreadingProfile));
muleContext.registry.registerObject(MuleProperties.OBJECT_DEFAULT_MESSAGE_REQUESTER_THREADING_PROFILE,
            new ChainedThreadingProfile(defaultThreadingProfile));
muleContext.registry.registerObject(MuleProperties.OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE, new NoRetryPolicyTemplate());

muleContext.registry.registerObject("doCompression", "true")

epFactory = muleContext.getEndpointFactory()

InboundEndpoint createInboundEndpoint(String url, String name)
{
    epBuilder= new EndpointURIEndpointBuilder(url, muleContext)
    epBuilder.name = name
    return epFactory.getInboundEndpoint(epBuilder)
}

OutboundEndpoint createOutboundEndpoint(String url, String name)
{
    epBuilder= new EndpointURIEndpointBuilder(url, muleContext)
    epBuilder.name = name
    return epFactory.getOutboundEndpoint(epBuilder)
}


//Set a dummy TX manager
muleContext.transactionManager = new TestTransactionManagerFactory().create()

//register connector
TestConnector c = new TestConnector(muleContext);
c.name = "dummyConnector"
muleContext.registry.registerConnector(c);

//Register transformers
TestCompressionTransformer testCompressionTransformer = new TestCompressionTransformer();
testCompressionTransformer.name = "TestCompressionTransformer"
testCompressionTransformer.returnClass = String.class
testCompressionTransformer.beanProperty2 = 12
testCompressionTransformer.containerProperty = "myString"
muleContext.registry.registerTransformer(testCompressionTransformer);

//Register Filter
filter = new MessagePropertyFilter();
filter.pattern = "foo=bar"

// Global Endpoint
epBuilder= new EndpointURIEndpointBuilder("test://fruitBowlPublishQ", muleContext)
epBuilder.addMessageProcessor(new MessageFilter(filter))
muleContext.registry.registerEndpointBuilder("fruitBowlEndpoint", epBuilder);

// Global Endpoint
epBuilder= new EndpointURIEndpointBuilder("test://test.queue", muleContext)
muleContext.registry.registerEndpointBuilder("waterMelonEndpoint", epBuilder);

// Global Endpoint
epBuilder= new EndpointURIEndpointBuilder("test://AppleQueue", muleContext)
epBuilder.name = "appleInEndpoint"
muleContext.registry.registerEndpointBuilder("appleInEndpoint", epBuilder);

// Global Endpoint
epBuilder= new EndpointURIEndpointBuilder("test://orangeQ", muleContext)
epBuilder.name = "orangeEndpoint"
epBuilder.setProperty("testGlobal", "value1")
muleContext.registry.registerEndpointBuilder("orangeEndpoint", epBuilder);

epBuilder = new EndpointURIEndpointBuilder("test://test.queue2", muleContext);
epBuilder.name = "testEPWithCS";
muleContext.registry.registerEndpointBuilder("testEPWithCS", epBuilder);

// Concrete Endpoints
epBuilder= new EndpointURIEndpointBuilder("test://AppleResponseQueue", muleContext)
epBuilder.name = "appleResponseEndpoint"
appleResponseEndpoint = epFactory.getInboundEndpoint(epBuilder)

epBuilder= new EndpointURIEndpointBuilder("test://orange", muleContext)
epBuilder.name = "Orange"
epBuilder.responseTransformers = [ muleContext.registry.lookupTransformer("TestCompressionTransformer") ]
orangeEndpoint = epFactory.getInboundEndpoint(epBuilder)

List interceptorStackList = new ArrayList()
interceptorStackList.add(new org.mule.interceptor.LoggingInterceptor())
interceptorStackList.add(new org.mule.interceptor.TimerInterceptor())
interceptorStackList.add(new org.mule.interceptor.LoggingInterceptor())
InterceptorStack interceptorStack = new InterceptorStack(interceptorStackList);
muleContext.registry.registerObject("testInterceptorStack", interceptorStack)

//register model
Model model = new SedaModel();
exceptionStrategy = new TestExceptionStrategy();
exceptionStrategy.addEndpoint(createOutboundEndpoint("test://component.exceptions", null));
model.exceptionListener = exceptionStrategy
model.lifecycleAdapterFactory = new TestComponentLifecycleAdapterFactory()
model.entryPointResolverSet = new TestEntryPointResolverSet()
muleContext.registry.registerModel(model)

// building service
Service service = new SedaService(muleContext);
service.model = model
service.name = "orangeComponent"
def component = new DefaultJavaComponent(new SingletonObjectFactory(Orange.class.name))
component.muleContext = muleContext
service.component = component
List interceptorList = new ArrayList()
interceptorList.add(new org.mule.interceptor.LoggingInterceptor())
interceptorList.add(interceptorStack)
interceptorList.add(new org.mule.interceptor.TimerInterceptor())
service.component.interceptors = interceptorList
epBuilder= new EndpointURIEndpointBuilder(muleContext.registry.lookupEndpointBuilder("orangeEndpoint"))
epBuilder.muleContext = muleContext
epBuilder.setProperty("testLocal", "value1")
epBuilder.addMessageProcessor(new MessageFilter(new PayloadTypeFilter(String.class)))
epBuilder.transformers = [ muleContext.registry.lookupTransformer("TestCompressionTransformer") ]
service.messageSource.addSource(epFactory.getInboundEndpoint(epBuilder))
service.messageSource.addSource(orangeEndpoint)

catchAllStrategy = new ForwardingCatchAllStrategy()
catchAllStrategy.endpoint = createOutboundEndpoint("test://catch.all", null)
service.messageSource.catchAllStrategy = catchAllStrategy

//Nested Router
nr = new DefaultInterfaceBinding();
nr.endpoint = createOutboundEndpoint("test://do.wash", null)
nr.setInterface(FruitCleaner.class);
nr.method = "wash"
service.component.interfaceBindings.add(nr)
nr = new DefaultInterfaceBinding();
nr.endpoint = createOutboundEndpoint("test://do.polish", null)
nr.setInterface(FruitCleaner.class);
nr.method = "polish"
service.component.interfaceBindings.add(nr)

//Outbound Router
outboundRouter = new OutboundPassThroughRouter()
outboundRouter.muleContext = muleContext
epBuilder = new EndpointURIEndpointBuilder(muleContext.registry.lookupEndpointBuilder("appleInEndpoint"))
epBuilder.muleContext = muleContext
epBuilder.transformers = [ muleContext.registry.lookupTransformer("TestCompressionTransformer") ]
outboundRouter.addRoute(epFactory.getOutboundEndpoint(epBuilder))
service.outboundRouter.addRoute(outboundRouter)

//Response Router
service.asyncReplyMessageSource.addSource(createInboundEndpoint("test://response1", null));
service.asyncReplyMessageSource.addSource(appleResponseEndpoint);
TestResponseAggregator responseAggregator = new TestResponseAggregator()
responseAggregator.muleContext=muleContext
service.asyncReplyMessageSource.addMessageProcessor(responseAggregator);
service.asyncReplyMessageSource.timeout = 10001

//Exception Strategy
dces = new DefaultMessagingExceptionStrategy();
dces.addEndpoint(createOutboundEndpoint("test://orange.exceptions", null));
service.exceptionListener = dces

// properties
// Since MULE-1933, Service no longer has properties and most properties are set on endpoint.
// So lets continue to test properties, but on targets instead.
endpointBuilderWithProps = new EndpointURIEndpointBuilder("test://endpointWithProps", muleContext)
endpointBuilderWithProps.name = "endpointWithProps"
endpointBuilderWithProps.properties = [
        brand: "Juicy Baby!",
        segments: "9",
        radius: "4.21",
        mapProperties: [prop1: "prop1", prop2: "prop2"],
        listProperties: ["prop1", "prop2", "prop3"],
        arrayProperties: ["prop4", "prop5", "prop6"]
        ]
muleContext.registry.registerEndpointBuilder("endpointWithProps",endpointBuilderWithProps)

//register components
muleContext.registry.registerService(service);
