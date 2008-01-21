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
import org.mule.umo.MuleContext
import org.mule.tck.testmodels.mule.TestResponseAggregator
import org.mule.routing.outbound.OutboundPassThroughRouter
import org.mule.impl.DefaultComponentExceptionStrategy
import org.mule.routing.inbound.InboundRouterCollection

// TODO: MULE-2520 Management context binding is not working in ScriptConfigurationBuilder
// remove this string when fixed
MuleContext muleContext = MuleServer.muleContext

muleContext.registry.registerObject("doCompression", "true", muleContext);

UMOImmutableEndpoint lookupEndpoint(String url, Boolean receiver)
{
    if (receiver)
    {
        return muleContext.registry.lookupInboundEndpoint(url, muleContext)
    }
    else
    {
        return muleContext.registry.lookupOutboundEndpoint(url, muleContext)
    }
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
muleContext.registry.registerEndpointBuilder("fruitBowlEndpoint", ep, muleContext);

ep = new EndpointURIEndpointBuilder("test://test.queue", muleContext)
muleContext.registry.registerEndpointBuilder("waterMelonEndpoint", ep, muleContext);

ep = new EndpointURIEndpointBuilder("test://AppleQueue", muleContext)
ep.name = "appleInEndpoint"
muleContext.registry.registerEndpointBuilder("appleInEndpoint", ep, muleContext);

ep = new EndpointURIEndpointBuilder("test://AppleResponseQueue", muleContext)
ep.name = "appleResponseEndpoint"
muleContext.registry.registerEndpoint(ep.buildResponseEndpoint(), muleContext);

ep = new EndpointURIEndpointBuilder("test://orangeQ", muleContext)
ep.name = "orangeEndpoint"
ep.setProperty("testGlobal", "value1")
muleContext.registry.registerEndpointBuilder("orangeEndpoint", ep, muleContext);

ep = new EndpointURIEndpointBuilder("test://orange", muleContext)
ep.name = "Orange"
ep.responseTransformers = [ muleContext.registry.lookupTransformer("TestCompressionTransformer") ]
muleContext.registry.registerEndpoint(ep.buildInboundEndpoint(), muleContext);

//register model
UMOModel model = new SedaModel();
exceptionStrategy = new TestExceptionStrategy();
exceptionStrategy.addEndpoint(lookupEndpoint("test://component.exceptions", false));
model.exceptionListener = exceptionStrategy
model.lifecycleAdapterFactory = new TestDefaultLifecycleAdapterFactory()
model.entryPointResolverSet = new TestEntryPointResolverSet()
muleContext.registry.registerModel(model, muleContext)

// building service
MuleDescriptor descriptor = new MuleDescriptor("orangeComponent");
descriptor.serviceFactory = new SingletonObjectFactory(Orange.class.name);
ep = new EndpointURIEndpointBuilder(muleContext.registry.lookupEndpoint("orangeEndpoint"), muleContext)
ep.setProperty("testLocal", "value1")
ep.filter = new PayloadTypeFilter(String.class)
ep.transformers = [ muleContext.registry.lookupTransformer("TestCompressionTransformer") ]
descriptor.inboundRouter = new InboundRouterCollection()
descriptor.inboundRouter.addEndpoint(ep.buildInboundEndpoint())
descriptor.inboundRouter.addEndpoint(muleContext.registry.lookupEndpoint("Orange", muleContext))

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
ep = new EndpointURIEndpointBuilder(muleContext.registry.lookupEndpoint("appleInEndpoint"), muleContext)
ep.transformers = [ muleContext.registry.lookupTransformer("TestCompressionTransformer") ]
outboundRouter.addEndpoint(ep.buildOutboundEndpoint())
descriptor.outboundRouter.addRouter(outboundRouter)

//Response Router
responseRouter = new ResponseRouterCollection();
responseRouter.addEndpoint(muleContext.registry.lookupResponseEndpoint("test://response1", muleContext));
responseRouter.addEndpoint(muleContext.registry.lookupEndpoint("appleResponseEndpoint", muleContext));
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
muleContext.registry.registerService(descriptor);