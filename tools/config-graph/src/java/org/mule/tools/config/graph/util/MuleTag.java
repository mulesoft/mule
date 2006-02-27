package org.mule.tools.config.graph.util;

public interface MuleTag {
	String ATTRIBUTE_ADDRESS = "address";

	String ELEMENT_ENDPOINT = "endpoint";

	String ATTRIBUTE_INBOUNDENDPOINT = "inboundEndpoint";
	
	String ATTRIBUTE_OUTBOUNDENDPOINT = "outboundEndpoint";
	
	String ATTRIBUTE_CLASS_NAME = "className";
	String ATTRIBUTE_RETURN_CLASS = "returnClass";

	String ATTRIBUTE_NAME = "name";
	String ATTRIBUTE_TYPE = "type";
	String ATTRIBUTE_VALUE = "value";
	String ATTRIBUTE_SYNCHRONOUS = "synchronous";

	String ELEMENT_AGENT = "agent";
	String ELEMENT_AGENTS = "agents";
	String ELEMENT_CONNECTOR = "connector";
	String ELEMENT_CONNECTION_STRATEGY = "connection-strategy";
	String ELEMENT_ENDPOINT_IDENTFIERS = "endpoint-identifiers";
	String ELEMENT_TRANSFORMERS = "transformers";
	String ELEMENT_TRANSFORMER = "transformer";
	String ELEMENT_MODEL = "model";
	String ELEMENT_MULE_DESCRIPTOR = "mule-descriptor";
	String ELEMENT_EXCEPTION_STRATEGY = "exception-strategy";
	String ELEMENT_CATCH_ALL_STRATEGY = "catch-all-strategy";
	String ELEMENT_MULE_ENVIRONMENT_PROPERTIES = "mule-environment-properties";

    String ELEMENT_FILTER = "filter";
    String ELEMENT_LEFT_FILTER = "left-filter";
    String ELEMENT_RIGHT_FILTER = "right-filter";

    String ELEMENT_INBOUND_ROUTER = "inbound-router";
    String ELEMENT_OUTBOUND_ROUTER = "outbound-router";
    String ELEMENT_RESPONSE_ROUTER = "response-router";
    String ELEMENT_ROUTER = "router";
    String ELEMENT_REPLY_TO = "reply-to";

    String ELEMENT_PROPERTIES = "properties";
    String ELEMENT_PROPERTY = "property";
    String ELEMENT_DESCRIPTION = "description";
    String ELEMENT_THREADING_PROFILE = "threading-profile";
    String ELEMENT_POOLING_PROFILE = "pooling-profile";
    String ELEMENT_QUEUE_PROFILE = "queue-profile";

}
