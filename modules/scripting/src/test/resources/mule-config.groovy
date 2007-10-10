package org.mule.config.builders;

import org.mule.MuleServer
import org.mule.tck.testmodels.mule.TestTransactionManagerFactory
import org.mule.tck.testmodels.mule.TestConnector
import org.mule.tck.testmodels.mule.TestExceptionStrategy
import org.mule.tck.testmodels.mule.TestCompressionTransformer
import org.mule.routing.filters.xml.JXPathFilter
import org.mule.impl.endpoint.EndpointURIEndpointBuilder
import org.mule.umo.model.UMOModel
import org.mule.impl.model.seda.SedaModel
import org.mule.umo.endpoint.UMOImmutableEndpoint
import org.mule.tck.testmodels.fruit.FruitCleaner;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.mule.TestDefaultLifecycleAdapterFactory
import org.mule.tck.testmodels.mule.TestEntryPointResolverSet
import org.mule.impl.MuleDescriptor
import org.mule.util.object.SingletonObjectFactory
import org.mule.routing.filters.PayloadTypeFilter
import org.mule.routing.ForwardingCatchAllStrategy
import org.mule.routing.nested.NestedRouterCollection
import org.mule.routing.nested.NestedRouter
import org.mule.routing.response.ResponseRouterCollection
import org.mule.umo.UMOManagementContext
import org.mule.tck.testmodels.mule.TestResponseAggregator
import org.mule.routing.outbound.OutboundPassThroughRouter
import org.mule.impl.DefaultComponentExceptionStrategy
import org.mule.routing.inbound.InboundRouterCollection

// TODO: MULE-2520 Management context binding is not working in ScriptConfigurationBuilder
// remove this string when fixed
UMOManagementContext managementContext = MuleServer.managementContext

managementContext.registry.registerObject("doCompression", "true", managementContext);

UMOImmutableEndpoint lookupEndpoint(String url, Boolean receiver)
{
    if (receiver)
    {
        return managementContext.registry.lookupInboundEndpoint(url, managementContext)
    }
    else
    {
        return managementContext.registry.lookupOutboundEndpoint(url, managementContext)
    }
}

//Set a dummy TX manager
managementContext.transactionManager = new TestTransactionManagerFactory().create()

//register connector
TestConnector c = new TestConnector();
c.name = "dummyConnector"
c.exceptionListener = new TestExceptionStrategy()
managementContext.registry.registerConnector(c);

//Register transformers
TestCompressionTransformer testCompressionTransformer = new TestCompressionTransformer();
testCompressionTransformer.name = "TestCompressionTransformer"
testCompressionTransformer.returnClass = String.class
testCompressionTransformer.beanProperty2 = 12
testCompressionTransformer.containerProperty = "myString"
managementContext.registry.registerTransformer(testCompressionTransformer);

//Register endpoints
filter = new JXPathFilter("name");
filter.value = "bar"
filter.namespaces = [foo: "http://foo.com"]

ep = new EndpointURIEndpointBuilder("test://fruitBowlPublishQ", managementContext)
ep.filter = filter
managementContext.registry.registerEndpointBuilder("fruitBowlEndpoint", ep, managementContext);

ep = new EndpointURIEndpointBuilder("test://test.queue", managementContext)
managementContext.registry.registerEndpointBuilder("waterMelonEndpoint", ep, managementContext);

ep = new EndpointURIEndpointBuilder("test://AppleQueue", managementContext)
ep.name = "appleInEndpoint"
managementContext.registry.registerEndpointBuilder("appleInEndpoint", ep, managementContext);

ep = new EndpointURIEndpointBuilder("test://AppleResponseQueue", managementContext)
ep.name = "appleResponseEndpoint"
managementContext.registry.registerEndpoint(ep.buildResponseEndpoint(), managementContext);

ep = new EndpointURIEndpointBuilder("test://orangeQ", managementContext)
ep.name = "orangeEndpoint"
ep.setProperty("testGlobal", "value1")
managementContext.registry.registerEndpointBuilder("orangeEndpoint", ep, managementContext);

ep = new EndpointURIEndpointBuilder("test://orange", managementContext)
ep.name = "Orange"
ep.responseTransformers = [ managementContext.registry.lookupTransformer("TestCompressionTransformer") ]
managementContext.registry.registerEndpoint(ep.buildInboundEndpoint(), managementContext);

//register model
UMOModel model = new SedaModel();
exceptionStrategy = new TestExceptionStrategy();
exceptionStrategy.addEndpoint(lookupEndpoint("test://component.exceptions", false));
model.exceptionListener = exceptionStrategy
model.lifecycleAdapterFactory = new TestDefaultLifecycleAdapterFactory()
model.entryPointResolverSet = new TestEntryPointResolverSet()
managementContext.registry.registerModel(model, managementContext)

// building service
MuleDescriptor descriptor = new MuleDescriptor("orangeComponent");
descriptor.serviceFactory = new SingletonObjectFactory(Orange.class.name);
ep = new EndpointURIEndpointBuilder(managementContext.registry.lookupEndpoint("orangeEndpoint"), managementContext)
ep.setProperty("testLocal", "value1")
ep.filter = new PayloadTypeFilter(String.class)
ep.transformers = [ managementContext.registry.lookupTransformer("TestCompressionTransformer") ]
descriptor.inboundRouter = new InboundRouterCollection()
descriptor.inboundRouter.addEndpoint(ep.buildInboundEndpoint())
descriptor.inboundRouter.addEndpoint(managementContext.registry.lookupEndpoint("Orange", managementContext))

catchAllStrategy = new ForwardingCatchAllStrategy()
catchAllStrategy.endpoint = lookupEndpoint("test://catch.all", false)
descriptor.inboundRouter.catchAllStrategy = catchAllStrategy

//Nested Router
nestedRouter = new NestedRouterCollection();
nr = new NestedRouter();
nr.endpoint = lookupEndpoint("test://do.wash", false)
nr.setInterface(FruitCleaner.class);
nr.method = "wash"
nestedRouter.addRouter(nr);
nr = new NestedRouter();
nr.endpoint = lookupEndpoint("test://do.polish", false)
nr.setInterface(FruitCleaner.class);
nr.method = "polish"
nestedRouter.addRouter(nr);
descriptor.nestedRouter = nestedRouter

//Outbound Router
outboundRouter = new OutboundPassThroughRouter()
ep = new EndpointURIEndpointBuilder(managementContext.registry.lookupEndpoint("appleInEndpoint"), managementContext)
ep.transformers = [ managementContext.registry.lookupTransformer("TestCompressionTransformer") ]
outboundRouter.addEndpoint(ep.buildOutboundEndpoint())
descriptor.outboundRouter.addRouter(outboundRouter)

//Response Router
responseRouter = new ResponseRouterCollection();
responseRouter.addEndpoint(managementContext.registry.lookupResponseEndpoint("test://response1", managementContext));
responseRouter.addEndpoint(managementContext.registry.lookupEndpoint("appleResponseEndpoint", managementContext));
responseRouter.addRouter(new TestResponseAggregator());
responseRouter.timeout = 10001
descriptor.responseRouter = responseRouter

//Exception Strategy
dces = new DefaultComponentExceptionStrategy();
dces.addEndpoint(lookupEndpoint("test://orange.exceptions", false));
descriptor.exceptionListener = dces

//properties
descriptor.properties = [
        brand: "Juicy Baby!",
        segments: "9",
        radius: "4.21",
        mapProperties: [prop1: "prop1", prop2: "prop2"],
        listProperties: ["prop1", "prop2", "prop3"],
        arrayProperties: ["prop4", "prop5", "prop6"]
        ]

//register components
managementContext.registry.registerService(descriptor);