package org.mule.config.builders;

import org.mule.MuleServer
import org.mule.config.MuleProperties;
import org.mule.impl.DefaultComponentExceptionStrategy;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.ResponseEndpoint;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.model.seda.SedaModel;
import org.mule.management.agents.JmxAgent;
import org.mule.management.agents.RmiRegistryAgent;
import org.mule.providers.SimpleRetryConnectionStrategy;
import org.mule.routing.ForwardingCatchAllStrategy;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.routing.filters.xml.JXPathFilter;
import org.mule.routing.inbound.InboundRouterCollection;
import org.mule.routing.nested.NestedRouter;
import org.mule.routing.nested.NestedRouterCollection;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.routing.response.ResponseRouterCollection;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.testmodels.fruit.FruitCleaner;
import org.mule.tck.testmodels.fruit.FloridaSunnyOrangeFactory;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.tck.testmodels.mule.TestDefaultLifecycleAdapterFactory;
import org.mule.tck.testmodels.mule.TestEntryPointResolver;
import org.mule.tck.testmodels.mule.TestExceptionStrategy;
import org.mule.tck.testmodels.mule.TestResponseAggregator;
import org.mule.tck.testmodels.mule.TestTransactionManagerFactory;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.model.UMOModel;
import org.mule.umo.routing.UMOInboundRouterCollection;
import org.mule.umo.routing.UMONestedRouterCollection;
import org.mule.umo.routing.UMOOutboundRouter;
import org.mule.umo.routing.UMOResponseRouterCollection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

UMOManagementContext managementContext = MuleServer.managementContext
     
// need this when running with JMX
managementContext.id = "GroovyScriptTestCase"

//set global properties
Map props = ((Map)managementContext.registry.lookupObject(MuleProperties.OBJECT_MULE_APPLICATION_PROPERTIES));
if(props == null)
{
    props = new HashMap();
    props.put("doCompression", "true");
    managementContext.registry.registerObject(MuleProperties.OBJECT_MULE_APPLICATION_PROPERTIES, props, managementContext);
} else
{
    props.put("doCompression", "true");
}

//Set a dummy TX manager
managementContext.transactionManager = new TestTransactionManagerFactory().create()

//register agents
RmiRegistryAgent rmiAgent = new RmiRegistryAgent();
rmiAgent.name = "rmiAgent"
managementContext.registry.registerAgent(rmiAgent);

//JmxAgent agent = new JmxAgent();
//agent.name = "jmxAgent"
//agent.connectorServerUrl = "service:jmx:rmi:///jndi/rmi://localhost:1099/server"
//Map p = new HashMap();
//p.put("jmx.remote.jndi.rebind", "true");
//agent.connectorServerProperties = p
//managementContext.registry.registerAgent(agent);

//register connector
TestConnector c = new TestConnector();
c.name = "dummyConnector"
c.exceptionListener = new TestExceptionStrategy()
managementContext.registry.registerConnector(c);

//Register transformers
TestCompressionTransformer t = new TestCompressionTransformer();
t.returnClass = String.class
t.beanProperty2 = 12
t.containerProperty = ""
t.beanProperty1 = "this was set from the manager properties!"
managementContext.registry.registerTransformer(t);

//Register endpoints
JXPathFilter filter = new JXPathFilter("name");
filter.value = "bar"
Map ns = new HashMap();
ns.put("foo", "http://foo.com");
filter.namespaces = ns;

UMOEndpoint ep = new MuleEndpoint("test://fruitBowlPublishQ", false);
ep.name = "fruitBowlEndpoint"
ep.filter = filter
managementContext.registry.registerEndpoint(ep, managementContext);
managementContext.registry.registerEndpoint(new MuleEndpoint("test://AppleQueue?endpointName=appleInEndpoint", true), managementContext);
managementContext.registry.registerEndpoint(new MuleEndpoint("test://AppleResponseQueue?endpointName=appleResponseEndpoint", false),managementContext);
managementContext.registry.registerEndpoint(new MuleEndpoint("test://apple.queue?endpointName=AppleQueue", false),managementContext);
managementContext.registry.registerEndpoint(new MuleEndpoint("test://banana.queue?endpointName=Banana_Queue", false),managementContext);
managementContext.registry.registerEndpoint(new MuleEndpoint("test://test.queue?endpointName=waterMelonEndpoint", false),managementContext);
ep = new MuleEndpoint("test://test.queue2", false);
ep.name = "testEPWithCS"
SimpleRetryConnectionStrategy cs = new SimpleRetryConnectionStrategy();
cs.retryCount = 4
cs.retryFrequency = 3000
ep.connectionStrategy = cs
managementContext.registry.registerEndpoint(ep, managementContext);

props = new HashMap();
props.put("testGlobal", "value1");
ep = new MuleEndpoint("test://orangeQ", false)
ep.name = "orangeEndpoint"
ep.properties = props
managementContext.registry.registerEndpoint(ep,managementContext);

//register model
UMOModel model = new SedaModel();
model.name = "main"
TestExceptionStrategy es = new TestExceptionStrategy();
es.addEndpoint(new MuleEndpoint("test://component.exceptions", false));
model.exceptionListener = es
model.lifecycleAdapterFactory = new TestDefaultLifecycleAdapterFactory()
model.entryPointResolver = new TestEntryPointResolver()
managementContext.registry.registerModel(model,managementContext)

//register components
UMOEndpoint ep1 = managementContext.registry.getOrCreateEndpointForUri("appleInEndpoint", UMOEndpoint.ENDPOINT_TYPE_SENDER)
TransformerChain tc = new TransformerChain()
tc.addTransformer(managementContext.registry.lookupTransformer("TestCompressionTransformer"))
ep1.transformer = tc

UMODescriptor d = MuleTestUtils.createDescriptor(Orange.class.name, "orangeComponent", null, ep1, props)

DefaultComponentExceptionStrategy dces = new DefaultComponentExceptionStrategy();
dces.addEndpoint(new MuleEndpoint("test://orange.exceptions", false));
d.exceptionListener = dces
//Create the inbound router
UMOInboundRouterCollection inRouter = new InboundRouterCollection();
inRouter.catchAllStrategy = new ForwardingCatchAllStrategy()
inRouter.catchAllStrategy.endpoint = new MuleEndpoint("test2://catch.all", false)
UMOEndpoint ep2 = new MuleEndpoint("test://orange/", true)
ep2.name = "Orange"
ep2.responseTransformer = managementContext.registry.lookupTransformer("TestCompressionTransformer")
inRouter.addEndpoint(ep2);
UMOEndpoint ep3 = managementContext.registry.getOrCreateEndpointForUri("orangeEndpoint", UMOEndpoint.ENDPOINT_TYPE_RECEIVER)
ep3.filter = new PayloadTypeFilter(String.class)
tc = new TransformerChain()
tc.addTransformer(managementContext.registry.lookupTransformer("TestCompressionTransformer"))
ep3.transformer = tc
Map props2 = new HashMap();
props2.put("testLocal", "value1");
ep3.properties = props2
inRouter.addEndpoint(ep3);
d.inboundRouter = inRouter

//Nested Router
UMONestedRouterCollection nestedRouter = new NestedRouterCollection();
NestedRouter nr1 = new NestedRouter();
nr1.endpoint = new MuleEndpoint("test://do.wash", false)
nr1.setInterface(FruitCleaner.class);
nr1.method = "wash"
nestedRouter.addRouter(nr1);
NestedRouter nr2 = new NestedRouter();
nr2.endpoint = new MuleEndpoint("test://do.polish", false)
nr2.setInterface(FruitCleaner.class);
nr2.method = "polish"
nestedRouter.addRouter(nr2);
d.nestedRouter = nestedRouter

//Response Router
UMOResponseRouterCollection responseRouter = new ResponseRouterCollection();
responseRouter.addEndpoint(new ResponseEndpoint("test://response1", true));
responseRouter.addEndpoint(managementContext.registry.getOrCreateEndpointForUri("appleResponseEndpoint", UMOEndpoint.ENDPOINT_TYPE_RESPONSE));
responseRouter.addRouter(new TestResponseAggregator());
responseRouter.timeout = 10001
d.responseRouter = responseRouter

//properties
Map cprops = new HashMap();
//cprops.put("orange", new Orange());
cprops.put("brand", "Juicy Baby!");
cprops.put("segments", "9");
cprops.put("radius", "4.21");
Map nested = new HashMap();
nested.put("prop1", "prop1");
nested.put("prop2", "prop2");
cprops.put("mapProperties", nested);
List nested2 = new ArrayList();
nested2.add("prop1");
nested2.add("prop2");
nested2.add("prop3");
cprops.put("listProperties", nested2);
List nested3 = new ArrayList();
nested3.add("prop4");
nested3.add("prop5");
nested3.add("prop6");
cprops.put("arrayProperties", nested3);
d.properties = cprops

d.modelName = "main"

//register components
managementContext.registry.registerService(d);
