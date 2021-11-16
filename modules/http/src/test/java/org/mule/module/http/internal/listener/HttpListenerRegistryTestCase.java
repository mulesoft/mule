/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import static java.lang.Boolean.parseBoolean;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.api.MuleRuntimeException;
import org.mule.api.config.MuleProperties;
import org.mule.module.http.internal.domain.request.HttpRequest;
import org.mule.module.http.internal.listener.async.RequestHandler;
import org.mule.module.http.internal.listener.matcher.AcceptsAllMethodsRequestMatcher;
import org.mule.module.http.internal.listener.matcher.ListenerRequestMatcher;
import org.mule.module.http.internal.listener.matcher.MethodRequestMatcher;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.size.SmallTest;
import org.mule.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@SmallTest
@RunWith(Parameterized.class)
public class HttpListenerRegistryTestCase extends AbstractMuleTestCase
{

    public static final String TEST_IP = "127.0.0.1";
    public static final String URI_PARAM = "{uri-param}";
    public static final int TEST_PORT = 10000;
    public static final String ANOTHER_PATH = "/another-path";
    public static final String SOME_PATH = "some-path";
    public static final String SOME_OTHER_PATH = "some-other-path";
    public static final String GET_METHOD = "GET";
    public static final String POST_METHOD = "POST";
    public static final String PUT_METHOD = "PUT";

    public static final String PATH_SEPARATOR = "/";
    public static final String ROOT_PATH = PATH_SEPARATOR;
    public static final String FIRST_LEVEL_PATH_LOWER_CASE = "/first-level-path";
    public static final String FIRST_LEVEL_PATH_UPPER_CASE = "/FIRST_LEVEL_PATH";
    public static final String FIRST_LEVEL_PATH_UPPER_CASE_CATCH_ALL = "/FIRST_LEVEL_PATH/*";
    public static final String SECOND_LEVEL_PATH = "/first-level-path/second-level";
    public static final String FIRST_LEVEL_URI_PARAM = PATH_SEPARATOR + URI_PARAM;
    public static final String FIRST_LEVEL_CATCH_ALL = "/*";
    public static final String SECOND_LEVEL_URI_PARAM = "/first-level-path/" + URI_PARAM;
    public static final String SECOND_LEVEL_CATCH_ALL = "/first-level-path/*";
    public static final String FOURTH_LEVEL_CATCH_ALL = "/another-first-level-path/second-level-path/third-level-path/*";
    public static final String URI_PARAM_IN_THE_MIDDLE = "/first-level-path/" + URI_PARAM + "/third-level-path";
    public static final String CATCH_ALL_IN_THE_MIDDLE = "/first-level-path/*/third-level-path";
    public static final String CATCH_ALL_IN_THE_MIDDLE_NO_COLLISION = "/another-first-level-path/*/third-level-path";
    public static final String SEVERAL_URI_PARAMS = "/{uri-param1}/second-level-path/{uri-param2}/fourth-level-path";
    public static final String SEVERAL_CATCH_ALL = "/*/second-level-path/*/fourth-level-path";
    public static final String METHOD_PATH_WILDCARD = "/method-path/*/";
    public static final String METHOD_PATH_URI_PARAM = "/another-method-path/{uri-param}/some-path";
    public static final String METHOD_PATH_CATCH_ALL = "/another-method-path/some-path/*";
    public static final String WILDCARD_CHARACTER = "*";

    public final RequestHandler methodPathWildcardGetRequestHandler = mock(RequestHandler.class);
    public final RequestHandler methodPathWildcardPostRequestHandler = mock(RequestHandler.class);
    public final RequestHandler methodPathUriParamGetRequestHandler = mock(RequestHandler.class);
    public final RequestHandler methodPathUriParamPostRequestHandler = mock(RequestHandler.class);
    public final RequestHandler methodPathCatchAllGetRequestHandler = mock(RequestHandler.class);
    public final RequestHandler methodPathCatchAllPostRequestHandler = mock(RequestHandler.class);

    private final ServerAddress testServerAddress = new ServerAddress(TEST_IP, TEST_PORT);

    @Rule
    public SystemProperty decodeUrlDisable;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private RequestHandler mockRequestHandler = mock(RequestHandler.class);
    private Map<String, RequestHandler> requestHandlerPerPath = new HashMap<>();
    private HttpListenerRegistry httpListenerRegistry;
    private Server testServer;

    @Parameterized.Parameters(name= "{0}")
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
            {"true"},
            {"false"}});
    }

    public HttpListenerRegistryTestCase(String disable)
    {
        this.decodeUrlDisable = new SystemProperty(MuleProperties.MULE_DISABLE_DECODE_URL, disable);
    }

    @Before
    public void createMockTestServer()
    {
        this.testServer = mock(Server.class);
        when(testServer.getServerAddress()).thenReturn(testServerAddress);
    }

    @Test
    public void validateSimplePathAndAllMethodAllowedCollision()
    {
        final HttpListenerRegistry httpListenerRegister = new HttpListenerRegistry();
        httpListenerRegister.addRequestHandler(testServer, mock(RequestHandler.class), new ListenerRequestMatcher(AcceptsAllMethodsRequestMatcher.instance(), ANOTHER_PATH));
        expectedException.expect(MuleRuntimeException.class);
        httpListenerRegister.addRequestHandler(testServer, mock(RequestHandler.class), new ListenerRequestMatcher(AcceptsAllMethodsRequestMatcher.instance(), ANOTHER_PATH));
    }

    @Test
    public void validateUriParamPathAndAllMethodAllowedCollision()
    {
        final HttpListenerRegistry httpListenerRegister = new HttpListenerRegistry();
        httpListenerRegister.addRequestHandler(testServer, mock(RequestHandler.class), new ListenerRequestMatcher(AcceptsAllMethodsRequestMatcher.instance(), SECOND_LEVEL_URI_PARAM));
        expectedException.expect(MuleRuntimeException.class);
        httpListenerRegister.addRequestHandler(testServer, mock(RequestHandler.class), new ListenerRequestMatcher(AcceptsAllMethodsRequestMatcher.instance(), SECOND_LEVEL_URI_PARAM));
    }

    @Test
    public void validateCatchAllPathAndAllMethodAllowedCollision()
    {
        final HttpListenerRegistry httpListenerRegister = new HttpListenerRegistry();
        httpListenerRegister.addRequestHandler(testServer, mock(RequestHandler.class), new ListenerRequestMatcher(AcceptsAllMethodsRequestMatcher.instance(), SECOND_LEVEL_CATCH_ALL));
        expectedException.expect(MuleRuntimeException.class);
        httpListenerRegister.addRequestHandler(testServer, mock(RequestHandler.class), new ListenerRequestMatcher(AcceptsAllMethodsRequestMatcher.instance(), SECOND_LEVEL_CATCH_ALL));
    }

    @Test
    public void validateCatchAllPathAndMethodAllowedCollision()
    {
        final HttpListenerRegistry httpListenerRegister = new HttpListenerRegistry();
        httpListenerRegister.addRequestHandler(testServer, mock(RequestHandler.class), new ListenerRequestMatcher(new MethodRequestMatcher(GET_METHOD), SECOND_LEVEL_CATCH_ALL));
        expectedException.expect(MuleRuntimeException.class);
        httpListenerRegister.addRequestHandler(testServer, mock(RequestHandler.class), new ListenerRequestMatcher(new MethodRequestMatcher(GET_METHOD), SECOND_LEVEL_CATCH_ALL));
    }

    @Test
    public void validateCatchAllPathAndMethodIntersectionAllowedCollision()
    {
        final HttpListenerRegistry httpListenerRegister = new HttpListenerRegistry();
        httpListenerRegister.addRequestHandler(testServer, mock(RequestHandler.class), new ListenerRequestMatcher(new MethodRequestMatcher(GET_METHOD, POST_METHOD), SECOND_LEVEL_CATCH_ALL));
        expectedException.expect(MuleRuntimeException.class);
        httpListenerRegister.addRequestHandler(testServer, mock(RequestHandler.class), new ListenerRequestMatcher(new MethodRequestMatcher(PUT_METHOD, POST_METHOD), SECOND_LEVEL_CATCH_ALL));
    }

    @Test
    public void validateRootPathCollision()
    {
        validateCollision(ROOT_PATH, ROOT_PATH);
    }

    @Test
    public void validateFirstLevelPathCollision()
    {
        validateCollision(FIRST_LEVEL_PATH_LOWER_CASE, FIRST_LEVEL_PATH_LOWER_CASE);
    }

    @Test
    public void validateSecondLevelPathCollision()
    {
        validateCollision(SECOND_LEVEL_PATH, SECOND_LEVEL_PATH);
    }

    @Test
    public void validateNoCollisionWithSpecificAndCatchAll()
    {
        validateNoCollision(FIRST_LEVEL_CATCH_ALL, FIRST_LEVEL_PATH_LOWER_CASE);
    }

    @Test
    public void validateNoCollisionWithSpecificAndUriParameter()
    {
        validateNoCollision(FIRST_LEVEL_URI_PARAM, FIRST_LEVEL_PATH_LOWER_CASE);
    }

    @Test
    public void validateCollisionWithRootLevelCatchAllAndRootLevelCatchAll()
    {
        validateCollision(FIRST_LEVEL_CATCH_ALL, FIRST_LEVEL_CATCH_ALL);
    }

    @Test
    public void validateCollisionWithRootLevelUriParamAndRootLevelUriParam()
    {
        validateCollision(FIRST_LEVEL_URI_PARAM, FIRST_LEVEL_URI_PARAM);
    }

    @Test
    public void validateCollisionWithSecondLevelCatchAllAndSecondLevelCatchAll()
    {
        validateCollision(SECOND_LEVEL_CATCH_ALL, SECOND_LEVEL_CATCH_ALL);
    }

    @Test
    public void validateCollisionWithSecondLevelUriParamAndSecondLevelUriParam()
    {
        validateCollision(SECOND_LEVEL_URI_PARAM, SECOND_LEVEL_URI_PARAM);
    }

    @Test
    public void validateUriParamAndCatchAllInTheMiddle()
    {
        validateCollision(URI_PARAM_IN_THE_MIDDLE, CATCH_ALL_IN_THE_MIDDLE);
    }

    @Test
    public void validateUriParamAndUriParamInTheMiddle()
    {
        validateCollision(URI_PARAM_IN_THE_MIDDLE, URI_PARAM_IN_THE_MIDDLE);
    }

    @Test
    public void validateCatchAllAndUriParamInTheMiddle()
    {
        validateCollision(CATCH_ALL_IN_THE_MIDDLE, URI_PARAM_IN_THE_MIDDLE);
    }

    @Test
    public void validateCatchAllAndCatchAllInTheMiddle()
    {
        validateCollision(CATCH_ALL_IN_THE_MIDDLE, CATCH_ALL_IN_THE_MIDDLE);
    }

    @Test
    public void validateSeveralUriParamsAndSeveralUriParams()
    {
        validateCollision(SEVERAL_URI_PARAMS, SEVERAL_URI_PARAMS);
    }

    @Test
    public void validateSeveralUriParamsAndSeveralCatchAll()
    {
        validateCollision(SEVERAL_URI_PARAMS, SEVERAL_CATCH_ALL);
    }

    @Test
    public void validateSeveralCatchAllAndSeveralUriParams()
    {
        validateCollision(SEVERAL_CATCH_ALL, SEVERAL_URI_PARAMS);
    }

    @Test
    public void validateSeveralCatchAllAndSeveralCatchAll()
    {
        validateCollision(SEVERAL_CATCH_ALL, SEVERAL_CATCH_ALL);
    }

    @Test
    public void noCollisionWithCaseSensitivePaths()
    {
        validateNoCollision(FIRST_LEVEL_PATH_LOWER_CASE, FIRST_LEVEL_PATH_UPPER_CASE);
    }

    @Test
    public void routeToCorrectHandler()
    {
        httpListenerRegistry = createHttpListenerRegistryWithRegisteredHandlers();
        routePath(ROOT_PATH, ROOT_PATH);
        routePath("/something", FIRST_LEVEL_CATCH_ALL);
        routePath("/something/else", FIRST_LEVEL_CATCH_ALL);
        routePath(SECOND_LEVEL_PATH + "/somethingElse", FIRST_LEVEL_CATCH_ALL);
        routePath(SECOND_LEVEL_PATH + "somethingElse", SECOND_LEVEL_URI_PARAM);
        routePath(FIRST_LEVEL_PATH_LOWER_CASE, FIRST_LEVEL_PATH_LOWER_CASE);
        routePath(FIRST_LEVEL_PATH_LOWER_CASE + PATH_SEPARATOR, FIRST_LEVEL_PATH_LOWER_CASE);
        routePath(FIRST_LEVEL_PATH_UPPER_CASE, FIRST_LEVEL_PATH_UPPER_CASE);
        routePath(FIRST_LEVEL_PATH_UPPER_CASE + PATH_SEPARATOR, FIRST_LEVEL_PATH_UPPER_CASE);
        routePath(FIRST_LEVEL_PATH_UPPER_CASE + "/somethingElse", FIRST_LEVEL_PATH_UPPER_CASE_CATCH_ALL);
        routePath(SECOND_LEVEL_PATH, SECOND_LEVEL_PATH);
        routePath(SECOND_LEVEL_URI_PARAM.replace(URI_PARAM, "1"), SECOND_LEVEL_URI_PARAM);
        routePath(FOURTH_LEVEL_CATCH_ALL.replace(WILDCARD_CHARACTER, StringUtils.EMPTY), FOURTH_LEVEL_CATCH_ALL);
        routePath(FOURTH_LEVEL_CATCH_ALL.replace(WILDCARD_CHARACTER, "foo1/foo2"), FOURTH_LEVEL_CATCH_ALL);
        routePath(URI_PARAM_IN_THE_MIDDLE.replace(URI_PARAM, "1"), URI_PARAM_IN_THE_MIDDLE);
        routePath(URI_PARAM_IN_THE_MIDDLE.replace(URI_PARAM, "1") + ANOTHER_PATH, FIRST_LEVEL_CATCH_ALL);
        routePath(CATCH_ALL_IN_THE_MIDDLE_NO_COLLISION.replace(WILDCARD_CHARACTER, SOME_PATH), CATCH_ALL_IN_THE_MIDDLE_NO_COLLISION);
        routePath(CATCH_ALL_IN_THE_MIDDLE_NO_COLLISION.replace(WILDCARD_CHARACTER, SOME_PATH) + ANOTHER_PATH, FIRST_LEVEL_CATCH_ALL);
        routePath(SEVERAL_CATCH_ALL.replace(WILDCARD_CHARACTER, SOME_PATH), SEVERAL_CATCH_ALL);
        routePath(SEVERAL_CATCH_ALL.replace(WILDCARD_CHARACTER, SOME_PATH) + ANOTHER_PATH, FIRST_LEVEL_CATCH_ALL);
        routePath(SEVERAL_CATCH_ALL.replace(WILDCARD_CHARACTER, SOME_PATH) + ANOTHER_PATH, FIRST_LEVEL_CATCH_ALL);
        routePath(METHOD_PATH_CATCH_ALL.replace(WILDCARD_CHARACTER, ANOTHER_PATH), GET_METHOD, methodPathCatchAllGetRequestHandler);
        routePath(METHOD_PATH_CATCH_ALL.replace(WILDCARD_CHARACTER, ANOTHER_PATH), POST_METHOD, methodPathCatchAllPostRequestHandler);
        routePath(METHOD_PATH_URI_PARAM.replace(URI_PARAM, SOME_OTHER_PATH), GET_METHOD, methodPathUriParamGetRequestHandler);
        routePath(METHOD_PATH_URI_PARAM.replace(URI_PARAM, SOME_OTHER_PATH), POST_METHOD, methodPathUriParamPostRequestHandler);
        routePath(METHOD_PATH_WILDCARD.replace(WILDCARD_CHARACTER, SOME_PATH), GET_METHOD, methodPathWildcardGetRequestHandler);
        routePath(METHOD_PATH_WILDCARD.replace(WILDCARD_CHARACTER, SOME_PATH), POST_METHOD, methodPathWildcardPostRequestHandler);
    }

    @Test
    public void noPathFound()
    {
        httpListenerRegistry = new HttpListenerRegistry();
        httpListenerRegistry.addRequestHandler(testServer, mock(RequestHandler.class), new ListenerRequestMatcher(AcceptsAllMethodsRequestMatcher.instance(), ROOT_PATH));
        RequestHandler requestHandler = httpListenerRegistry.getRequestHandler(TEST_IP, TEST_PORT, createMockRequestWithPath(ANOTHER_PATH));
        assertThat(requestHandler, is(instanceOf(NoListenerRequestHandler.class)));
    }

    @Test
    public void decodeNoPathFound()
    {
        httpListenerRegistry = new HttpListenerRegistry();
        httpListenerRegistry.addRequestHandler(testServer, mock(RequestHandler.class), new ListenerRequestMatcher(AcceptsAllMethodsRequestMatcher.instance(), "/{uriParam}/"));
        RequestHandler requestHandler = httpListenerRegistry.getRequestHandler(TEST_IP, TEST_PORT, createMockRequestWithPath("/apath%2F%2F"));

        if(parseBoolean(decodeUrlDisable.getValue()))
        {
            assertThat(requestHandler, not(instanceOf(NoListenerRequestHandler.class)));
        }
        else
        {
            assertThat(requestHandler, is(instanceOf(NoListenerRequestHandler.class)));
        }
    }

    @Test
    public void decodePathFound()
    {
        httpListenerRegistry = new HttpListenerRegistry();
        httpListenerRegistry.addRequestHandler(testServer, mock(RequestHandler.class), new ListenerRequestMatcher(AcceptsAllMethodsRequestMatcher.instance(), "/{uriParam}/"));
        RequestHandler requestHandler = httpListenerRegistry.getRequestHandler(TEST_IP, TEST_PORT, createMockRequestWithPath("/apath%2F"));
        assertThat(requestHandler, not(instanceOf(NoListenerRequestHandler.class)));
    }

    @Test
    public void replacePathCatchAllForCatchAll()
    {
        replace("/a/b/*", "/a/*", "/a/b/c", "/a/c");
    }

    @Test
    public void replacePathCatchAllForUriParam()
    {
        replace("/a/b/*", "/a/{b}", "/a/b/c", "/a/c");
    }

    @Test
    public void replacePathForCatchAll()
    {
        replace("/a/b/c", "/a/*", "/a/b/c", "/a/c");
    }

    @Test
    public void replacePathForUriParam()
    {
        replace("/a/b/c", "/a/{b}", "/a/b/c", "/a/c");
    }

    @Test
    public void replacePathUriParamForCatchAll()
    {
        replace("/a/{b}/c", "/a/*", "/a/b/c", "/a/c");
    }

    @Test
    public void replacePathUriParamForUriParam()
    {
        replace("/a/{b}/c", "/a/{b}", "/a/b/c", "/a/c");
    }

    @Test
    public void removingCatchAllDoesNotAffectParentPath()
    {
        removeChildAndCheckParent("/a", "/*", "/a/b/c");
    }

    @Test
    public void removingSubPathDoesNotAffectParentPath()
    {
        removeChildAndCheckParent("/a", "/b", "/a/b");
    }

    @Test
    public void removingUriParamDoesNotAffectParentPath()
    {
        removeChildAndCheckParent("/a", "/{b}", "/a/c");
    }

    @Test
    @Ignore("MULE-15247: Analyse HTTP listener path behavior for catch all")
    public void removingCatchAllDoesNotAffectParentCatchAll()
    {
        removeChildAndCheckParent("/*", "/*", "/a/b/c");
    }

    @Test
    public void removingSubPathDoesNotAffectParentCatchAll()
    {
        removeChildAndCheckParent("/*", "/b", "/a/b");
    }

    @Test
    public void removingUriParamDoesNotAffectParentCatchAll()
    {
        removeChildAndCheckParent("/*", "/{param}", "/a/c");
    }

    @Test
    public void removingCatchAllDoesNotAffectParentUriParam()
    {
        removeChildAndCheckParent("/{param}", "/*", "/a/b/c");
    }

    @Test
    public void removingSubPathDoesNotAffectParentUriParam()
    {
        removeChildAndCheckParent("/{param}", "/b", "/a/b");
    }

    @Test
    public void removingUriParamDoesNotAffectParentUriParam()
    {
        removeChildAndCheckParent("/{param}", "/{param}", "/a/c");
    }

    @Test
    public void removingPathParentPathDoesNotAffectCatchAll()
    {
      removeParentAndCheckChild("/a", "/*", "/a/b/c");
    }

    @Test
    @Ignore("MULE-15247: Analyse HTTP listener path behavior for catch all")
    public void removingParentCatchAllDoesNotAffectCatchAll()
    {
        removeParentAndCheckChild("/*", "/*", "/a/b/c");
    }


    @Test
    @Ignore("MULE-15247: Analyse HTTP listener path behavior for catch all")
    public void removingParentUriParamDoesNotAffectCatchAll()
    {
        removeParentAndCheckChild("/{param}", "/*", "/a/b/c");
    }

    @Test
    public void removingPathParentPathDoesNotAffectSubPath()
    {
      removeParentAndCheckChild("/a", "/b", "/a/b");
    }

    @Test
    public void removingParentCatchAllDoesNotAffectSubPath()
    {
        removeParentAndCheckChild("/*", "/b", "/a/b");
    }


    @Test
    public void removingParentUriParamDoesNotAffectSubPath()
    {
        removeParentAndCheckChild("/{param}", "/b", "/a/b");
    }

    @Test
    public void removingPathParentPathDoesNotAffectUriParam()
    {
      removeParentAndCheckChild("/a", "/{param}", "/a/c");
    }

    @Test
    public void removingParentCatchAllDoesNotAffectUriParam()
    {
        removeParentAndCheckChild("/*", "/{param}", "/a/c");
    }


    @Test
    public void removingParentUriParamDoesNotAffectUriParam()
    {
        removeParentAndCheckChild("/{param}", "/{param}", "/a/c");
    }

    @Test
    public void removingCatchAllHandlerDoesNotAffectOther()
    {
        removePostAndCheckGet("/*");
    }

    @Test
    public void removingPathHandlerDoesNotAffectOther()
    {
      removePostAndCheckGet("/a");
    }

    @Test
    public void removingUriParamHandlerDoesNotAffectOther()
    {
      removePostAndCheckGet("/{a}");
    }

    private void replace(String oldPath, String newPath, String oldRequestPath, String newRequestPath) {
        httpListenerRegistry = new HttpListenerRegistry();
        RequestHandler oldPathHandler = mock(RequestHandler.class);
        RequestHandlerManager oldManager = httpListenerRegistry.addRequestHandler(testServer, oldPathHandler, new ListenerRequestMatcher(AcceptsAllMethodsRequestMatcher.instance(), oldPath));

        routePath(oldRequestPath, GET_METHOD, oldPathHandler);
        routePath(newRequestPath, GET_METHOD, NoListenerRequestHandler.getInstance());

        oldManager.dispose();

        routePath(oldRequestPath, GET_METHOD, NoListenerRequestHandler.getInstance());
        routePath(newRequestPath, GET_METHOD, NoListenerRequestHandler.getInstance());

        RequestHandler newPathHandler = mock(RequestHandler.class);
        httpListenerRegistry.addRequestHandler(testServer, newPathHandler, new ListenerRequestMatcher(AcceptsAllMethodsRequestMatcher.instance(), newPath));

        routePath(oldRequestPath, GET_METHOD, newPath.endsWith(WILDCARD_CHARACTER) ? newPathHandler : NoListenerRequestHandler.getInstance());
        routePath(newRequestPath, GET_METHOD, newPathHandler);
    }

    private void removeChildAndCheckParent(String parent, String child, String childRequestPath)
    {
        httpListenerRegistry = new HttpListenerRegistry();
        RequestHandler parentHandler = mock(RequestHandler.class);
        RequestHandler childHandler = mock(RequestHandler.class);
        httpListenerRegistry.addRequestHandler(testServer, parentHandler, new ListenerRequestMatcher(AcceptsAllMethodsRequestMatcher.instance(), parent));
        RequestHandlerManager childManager = httpListenerRegistry.addRequestHandler(testServer, childHandler, new ListenerRequestMatcher(AcceptsAllMethodsRequestMatcher.instance(), parent + child));

        routePath("/a", GET_METHOD, parentHandler);
        routePath(childRequestPath, GET_METHOD, childHandler);

        childManager.dispose();

        routePath("/a", GET_METHOD, parentHandler);
        routePath(childRequestPath, GET_METHOD, parent.endsWith(WILDCARD_CHARACTER) ? parentHandler : NoListenerRequestHandler.getInstance());

        RequestHandler newChildHandler = mock(RequestHandler.class);
        httpListenerRegistry.addRequestHandler(testServer, newChildHandler, new ListenerRequestMatcher(AcceptsAllMethodsRequestMatcher.instance(), parent + child));

        routePath("/a", GET_METHOD, parentHandler);
        routePath(childRequestPath, GET_METHOD, newChildHandler);
    }

    private void removeParentAndCheckChild(String path, String child, String childRequestPath)
    {
        httpListenerRegistry = new HttpListenerRegistry();
        RequestHandler parentHandler = mock(RequestHandler.class);
        RequestHandler childHandler = mock(RequestHandler.class);
        RequestHandlerManager parentManager = httpListenerRegistry.addRequestHandler(testServer, parentHandler, new ListenerRequestMatcher(AcceptsAllMethodsRequestMatcher.instance(), path));
        httpListenerRegistry.addRequestHandler(testServer, childHandler, new ListenerRequestMatcher(AcceptsAllMethodsRequestMatcher.instance(), path + child));

        routePath("/a", GET_METHOD, parentHandler);
        routePath(childRequestPath, GET_METHOD, childHandler);

        parentManager.dispose();

        routePath("/a", GET_METHOD, child.endsWith(WILDCARD_CHARACTER) ? childHandler : NoListenerRequestHandler.getInstance());
        routePath(childRequestPath, GET_METHOD, childHandler);

        RequestHandler newParentHandler = mock(RequestHandler.class);
        httpListenerRegistry.addRequestHandler(testServer, newParentHandler, new ListenerRequestMatcher(AcceptsAllMethodsRequestMatcher.instance(), path));

        routePath("/a", GET_METHOD, newParentHandler);
        routePath(childRequestPath, GET_METHOD, childHandler);
    }

    private void removePostAndCheckGet(String path)
    {
        httpListenerRegistry = new HttpListenerRegistry();
        RequestHandler getHandler = mock(RequestHandler.class);
        RequestHandler postHandler = mock(RequestHandler.class);
        httpListenerRegistry.addRequestHandler(testServer, getHandler, new ListenerRequestMatcher(new MethodRequestMatcher(GET_METHOD), path));
        RequestHandlerManager manager = httpListenerRegistry.addRequestHandler(testServer, postHandler, new ListenerRequestMatcher(new MethodRequestMatcher(POST_METHOD), path));

        routePath("/a", GET_METHOD, getHandler);
        routePath("/a", POST_METHOD, postHandler);

        manager.dispose();

        routePath("/a", GET_METHOD, getHandler);
        routePath("/a", POST_METHOD, NoMethodRequestHandler.getInstance());

        RequestHandler newPostHandler = mock(RequestHandler.class);
        httpListenerRegistry.addRequestHandler(testServer, newPostHandler, new ListenerRequestMatcher(new MethodRequestMatcher(POST_METHOD), path));

        routePath("/a", GET_METHOD, getHandler);
        routePath("/a", POST_METHOD, newPostHandler);
    }

    private void routePath(String requestPath, String listenerPath)
    {
        assertThat(httpListenerRegistry.getRequestHandler(TEST_IP, TEST_PORT, createMockRequestWithPath(requestPath)), is(requestHandlerPerPath.get(listenerPath)));
    }

    private void routePath(String requestPath, String requestMethod,  RequestHandler expectedRequestHandler)
    {
        final HttpRequest mockRequest = createMockRequestWithPath(requestPath);
        when(mockRequest.getMethod()).thenReturn(requestMethod);
        assertThat(httpListenerRegistry.getRequestHandler(TEST_IP, TEST_PORT, mockRequest), is(expectedRequestHandler));
    }

    private HttpRequest createMockRequestWithPath(String path)
    {
        final HttpRequest mockRequest = mock(HttpRequest.class);
        when(mockRequest.getPath()).thenReturn(path);
        return mockRequest;
    }

    private HttpListenerRegistry createHttpListenerRegistryWithRegisteredHandlers()
    {
        final HttpListenerRegistry httpListenerRegistry = new HttpListenerRegistry();
        requestHandlerPerPath.put(ROOT_PATH, mock(RequestHandler.class));
        requestHandlerPerPath.put(FIRST_LEVEL_CATCH_ALL, mock(RequestHandler.class));
        requestHandlerPerPath.put(FIRST_LEVEL_PATH_LOWER_CASE, mock(RequestHandler.class));
        requestHandlerPerPath.put(FIRST_LEVEL_PATH_UPPER_CASE, mock(RequestHandler.class));
        requestHandlerPerPath.put(FIRST_LEVEL_PATH_UPPER_CASE_CATCH_ALL, mock(RequestHandler.class));
        requestHandlerPerPath.put(SECOND_LEVEL_PATH, mock(RequestHandler.class));
        requestHandlerPerPath.put(SECOND_LEVEL_URI_PARAM, mock(RequestHandler.class));
        requestHandlerPerPath.put(FOURTH_LEVEL_CATCH_ALL, mock(RequestHandler.class));
        requestHandlerPerPath.put(URI_PARAM_IN_THE_MIDDLE, mock(RequestHandler.class));
        requestHandlerPerPath.put(CATCH_ALL_IN_THE_MIDDLE_NO_COLLISION, mock(RequestHandler.class));
        requestHandlerPerPath.put(SEVERAL_CATCH_ALL, mock(RequestHandler.class));
        for (String path : requestHandlerPerPath.keySet())
        {
            httpListenerRegistry.addRequestHandler(testServer, requestHandlerPerPath.get(path), new ListenerRequestMatcher(AcceptsAllMethodsRequestMatcher.instance(), path));
        }
        httpListenerRegistry.addRequestHandler(testServer, methodPathUriParamGetRequestHandler, new ListenerRequestMatcher(new MethodRequestMatcher("GET"), METHOD_PATH_URI_PARAM));
        httpListenerRegistry.addRequestHandler(testServer, methodPathUriParamPostRequestHandler, new ListenerRequestMatcher(new MethodRequestMatcher("POST"), METHOD_PATH_URI_PARAM));
        httpListenerRegistry.addRequestHandler(testServer, methodPathCatchAllGetRequestHandler, new ListenerRequestMatcher(new MethodRequestMatcher("GET"), METHOD_PATH_CATCH_ALL));
        httpListenerRegistry.addRequestHandler(testServer, methodPathCatchAllPostRequestHandler, new ListenerRequestMatcher(new MethodRequestMatcher("POST"), METHOD_PATH_CATCH_ALL));
        httpListenerRegistry.addRequestHandler(testServer, methodPathWildcardGetRequestHandler, new ListenerRequestMatcher(new MethodRequestMatcher("GET"), METHOD_PATH_WILDCARD));
        httpListenerRegistry.addRequestHandler(testServer, methodPathWildcardPostRequestHandler, new ListenerRequestMatcher(new MethodRequestMatcher("POST"), METHOD_PATH_WILDCARD));
        return httpListenerRegistry;
    }

    private void validateNoCollision(String... paths)
    {
        final HttpListenerRegistry httpListenerRegistry = new HttpListenerRegistry();
        for (String path : paths)
        {
            httpListenerRegistry.addRequestHandler(testServer, mockRequestHandler, new ListenerRequestMatcher(AcceptsAllMethodsRequestMatcher.instance(), path));
        }
    }

    private void validateCollision(String firstPath, String secondPath)
    {
        final HttpListenerRegistry httpListenerRegistry = new HttpListenerRegistry();
        httpListenerRegistry.addRequestHandler(testServer, mockRequestHandler, new ListenerRequestMatcher(AcceptsAllMethodsRequestMatcher.instance(), firstPath));

        expectedException.expect(MuleRuntimeException.class);
        httpListenerRegistry.addRequestHandler(testServer, mockRequestHandler, new ListenerRequestMatcher(AcceptsAllMethodsRequestMatcher.instance(), secondPath));
    }

}
