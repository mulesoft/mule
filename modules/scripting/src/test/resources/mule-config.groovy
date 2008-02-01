package org.mule.config.builders;

import org.mule.tck.testmodels.mule.TestTransactionManagerFactory
import org.mule.tck.testmodels.mule.TestConnector
import org.mule.tck.testmodels.mule.TestExceptionStrategy
import org.mule.tck.testmodels.mule.TestCompressionTransformer
import org.mule.routing.filters.xml.JXPathFilter
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

ImmutableEndpoint createEndpoint(String url, String name, Boolean inbound)
{
    ep = new EndpointURIEndpointBuilder(url, muleContext)
    ep.name = name
    ImmutableEndpoint toReturn = inbound ? ep.buildInboundEndpoint() : ep.buildOutboundEndpoint()
    muleContext.registry.registerEndpoint(toReturn);

    return toReturn
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

ep = new EndpointURIEndpointBuilder("test://fruitBowlPublishQ", muleContext)
ep.filter = filter
muleContext.registry.registerEndpointBuilder("fruitBowlEndpoint", ep);

ep = new EndpointURIEndpointBuilder("test://test.queue", muleContext)
muleContext.registry.registerEndpointBuilder("waterMelonEndpoint", ep);

ep = new EndpointURIEndpointBuilder("test://AppleQueue", muleContext)
ep.name = "appleInEndpoint"
muleContext.registry.registerEndpointBuilder("appleInEndpoint", ep);

ep = new EndpointURIEndpointBuilder("test://AppleResponseQueue", muleContext)
ep.name = "appleResponseEndpoint"
muleContext.registry.registerEndpoint(ep.buildInboundEndpoint());

ep = new EndpointURIEndpointBuilder("test://orangeQ", muleContext)
ep.name = "orangeEndpoint"
ep.setProperty("testGlobal", "value1")
muleContext.registry.registerEndpointBuilder("orangeEndpoint", ep);

ep = new EndpointURIEndpointBuilder("test://orange", muleContext)
ep.name = "Orange"
ep.responseTransformers = [ muleContext.registry.lookupTransformer("TestCompressionTransformer") ]
muleContext.registry.registerEndpoint(ep.buildInboundEndpoint());

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
ep = new EndpointURIEndpointBuilder(muleContext.registry.lookupEndpointBuilder("orangeEndpoint"))
ep.muleContext = muleContext
ep.setProperty("testLocal", "value1")
ep.filter = new PayloadTypeFilter(String.class)
ep.transformers = [ muleContext.registry.lookupTransformer("TestCompressionTransformer") ]
service.inboundRouter = new DefaultInboundRouterCollection()
service.inboundRouter.addEndpoint(ep.buildInboundEndpoint())
service.inboundRouter.addEndpoint(muleContext.registry.lookupEndpoint("Orange"))

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
ep = new EndpointURIEndpointBuilder(muleContext.registry.lookupEndpointBuilder("appleInEndpoint"))
ep.muleContext = muleContext
ep.transformers = [ muleContext.registry.lookupTransformer("TestCompressionTransformer") ]
outboundRouter.addEndpoint(ep.buildOutboundEndpoint())
service.outboundRouter.addRouter(outboundRouter)

//Response Router
responseRouter = new DefaultResponseRouterCollection();
responseRouter.addEndpoint(createEndpoint("test://response1", null, true));
responseRouter.addEndpoint(muleContext.registry.lookupEndpoint("appleResponseEndpoint"));
responseRouter.addRouter(new TestResponseAggregator());
responseRouter.timeout = 10001
service.responseRouter = responseRouter

//Exception Strategy
dces = new DefaultServiceExceptionStrategy();
dces.addEndpoint(createEndpoint("test://orange.exceptions", null, false));
service.exceptionListener = dces

//properties
service.properties = [
        brand: "Juicy Baby!",
        segments: "9",
        radius: "4.21",
        mapProperties: [prop1: "prop1", prop2: "prop2"],
        listProperties: ["prop1", "prop2", "prop3"],
        arrayProperties: ["prop4", "prop5", "prop6"]
        ]

//register components
muleContext.registry.registerService(service);