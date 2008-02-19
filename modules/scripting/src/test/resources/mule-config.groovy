package org.mule.config.builders;

import org.mule.tck.testmodels.mule.TestTransactionManagerFactory
import org.mule.tck.testmodels.mule.TestConnector
import org.mule.tck.testmodels.mule.TestExceptionStrategy
import org.mule.tck.testmodels.mule.TestCompressionTransformer
import org.mule.routing.filters.xml.JXPathFilter
import org.mule.api.endpoint.EndpointBuilder
import org.mule.endpoint.EndpointURIEndpointBuilder
import org.mule.model.seda.SedaModel
import org.mule.tck.testmodels.fruit.FruitCleaner;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.mule.TestDefaultLifecycleAdapterFactory
import org.mule.tck.testmodels.mule.TestEntryPointResolverSet
import org.mule.util.object.SingletonObjectFactory
import org.mule.routing.filters.PayloadTypeFilter
import org.mule.routing.ForwardingCatchAllStrategy
import org.mule.tck.testmodels.mule.TestResponseAggregator
import org.mule.routing.outbound.OutboundPassThroughRouter
import org.mule.api.endpoint.ImmutableEndpoint
import org.mule.api.model.Model
import org.mule.api.service.Service
import org.mule.model.seda.SedaService
import org.mule.routing.inbound.DefaultInboundRouterCollection
import org.mule.routing.nested.DefaultNestedRouterCollection
import org.mule.routing.nested.DefaultNestedRouter
import org.mule.routing.response.DefaultResponseRouterCollection
import org.mule.service.DefaultServiceExceptionStrategy
import org.mule.util.queue.QueueManager
import org.mule.util.queue.TransactionalQueueManager
import org.mule.util.queue.MemoryPersistenceStrategy
import org.mule.security.MuleSecurityManager
import org.mule.endpoint.DefaultEndpointFactory
import org.mule.api.config.MuleProperties;

QueueManager queueManager = new TransactionalQueueManager();
queueManager.persistenceStrategy = new MemoryPersistenceStrategy()
muleContext.registry.registerObject(MuleProperties.OBJECT_QUEUE_MANAGER, queueManager);
muleContext.registry.registerObject(MuleProperties.OBJECT_SECURITY_MANAGER, new MuleSecurityManager());
muleContext.registry.registerObject(MuleProperties.OBJECT_MULE_ENDPOINT_FACTORY, new DefaultEndpointFactory());

muleContext.registry.registerObject("doCompression", "true")

epFactory = muleContext.registry.lookupEndpointFactory()

ImmutableEndpoint createEndpoint(String url, String name, Boolean inbound)
{
    epBuilder= new EndpointURIEndpointBuilder(url, muleContext)
    epBuilder.name = name
    return inbound ? epFactory.getInboundEndpoint(epBuilder) : epFactory.getOutboundEndpoint(epBuilder)
}

//Set a dummy TX manager
muleContext.transactionManager = new TestTransactionManagerFactory().create()

//register connector
TestConnector c = new TestConnector();
c.name = "dummyConnector"
c.exceptionListener = new TestExceptionStrategy()
muleContext.registry.registerConnector(c);

//Register transformers
TestCompressionTransformer testCompressionTransformer = new TestCompressionTransformer();
testCompressionTransformer.name = "TestCompressionTransformer"
testCompressionTransformer.returnClass = String.class
testCompressionTransformer.beanProperty2 = 12
testCompressionTransformer.containerProperty = "myString"
muleContext.registry.registerTransformer(testCompressionTransformer);

//Register endpoints
filter = new JXPathFilter("name");
filter.value = "bar"
filter.namespaces = [foo: "http://foo.com"]

// Global Endpoint
epBuilder= new EndpointURIEndpointBuilder("test://fruitBowlPublishQ", muleContext)
epBuilder.filter = filter
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

// Concrete Endpoints
epBuilder= new EndpointURIEndpointBuilder("test://AppleResponseQueue", muleContext)
epBuilder.name = "appleResponseEndpoint"
appleResponseEndpoint = epFactory.getInboundEndpoint(epBuilder)

epBuilder= new EndpointURIEndpointBuilder("test://orange", muleContext)
epBuilder.name = "Orange"
epBuilder.responseTransformers = [ muleContext.registry.lookupTransformer("TestCompressionTransformer") ]
orangeEndpoint = epFactory.getInboundEndpoint(epBuilder)

//register model
Model model = new SedaModel();
exceptionStrategy = new TestExceptionStrategy();
exceptionStrategy.addEndpoint(createEndpoint("test://component.exceptions", null, false));
model.exceptionListener = exceptionStrategy
model.lifecycleAdapterFactory = new TestDefaultLifecycleAdapterFactory()
model.entryPointResolverSet = new TestEntryPointResolverSet()
muleContext.registry.registerModel(model)

// building service
Service service = new SedaService();
service.model = model
service.name = "orangeComponent"
service.serviceFactory = new SingletonObjectFactory(Orange.class.name);
epBuilder= new EndpointURIEndpointBuilder(muleContext.registry.lookupEndpointBuilder("orangeEndpoint"))
epBuilder.muleContext = muleContext
epBuilder.setProperty("testLocal", "value1")
epBuilder.filter = new PayloadTypeFilter(String.class)
epBuilder.transformers = [ muleContext.registry.lookupTransformer("TestCompressionTransformer") ]
service.inboundRouter = new DefaultInboundRouterCollection()
service.inboundRouter.addEndpoint(epFactory.getInboundEndpoint(epBuilder))
service.inboundRouter.addEndpoint(orangeEndpoint)

catchAllStrategy = new ForwardingCatchAllStrategy()
catchAllStrategy.endpoint = createEndpoint("test://catch.all", null, true)
service.inboundRouter.catchAllStrategy = catchAllStrategy

//Nested Router
nestedRouter = new DefaultNestedRouterCollection();
nr = new DefaultNestedRouter();
nr.endpoint = createEndpoint("test://do.wash", null, false)
nr.setInterface(FruitCleaner.class);
nr.method = "wash"
nestedRouter.addRouter(nr);
nr = new DefaultNestedRouter();
nr.endpoint = createEndpoint("test://do.polish", null, false)
nr.setInterface(FruitCleaner.class);
nr.method = "polish"
nestedRouter.addRouter(nr);
service.nestedRouter = nestedRouter

//Outbound Router
outboundRouter = new OutboundPassThroughRouter()
epBuilder = new EndpointURIEndpointBuilder(muleContext.registry.lookupEndpointBuilder("appleInEndpoint"))
epBuilder.muleContext = muleContext
epBuilder.transformers = [ muleContext.registry.lookupTransformer("TestCompressionTransformer") ]
outboundRouter.addEndpoint(epFactory.getOutboundEndpoint(epBuilder))
service.outboundRouter.addRouter(outboundRouter)

//Response Router
responseRouter = new DefaultResponseRouterCollection();
responseRouter.addEndpoint(createEndpoint("test://response1", null, true));
responseRouter.addEndpoint(appleResponseEndpoint);
responseRouter.addRouter(new TestResponseAggregator());
responseRouter.timeout = 10001
service.responseRouter = responseRouter

//Exception Strategy
dces = new DefaultServiceExceptionStrategy();
dces.addEndpoint(createEndpoint("test://orange.exceptions", null, false));
service.exceptionListener = dces

// properties
// Since MULE-1933, Service no longer has properties and most properties are set on endpoint.
// So lets continue to test properties, but on endpoints instead.
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
