/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2014 Brocade Communications Systems, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.common.util.RpcErrors;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.core.api.mount.MountInstance;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdWithSchemaNode;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.controller.sal.restconf.impl.StructuredData;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorSeverity;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.MutableCompositeNode;
import org.opendaylight.yangtools.yang.data.api.MutableSimpleNode;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class InvokeRpcMethodTest {

    private RestconfImpl restconfImpl = null;
    private static ControllerContext controllerContext = null;


    @BeforeClass
    public static void init() throws FileNotFoundException {
        Set<Module> allModules = new HashSet<Module>( TestUtils
                .loadModulesFrom("/full-versions/yangs") );
        allModules.addAll( TestUtils.loadModulesFrom("/invoke-rpc") );
        assertNotNull(allModules);
        Module module = TestUtils.resolveModule("invoke-rpc-module", allModules);
        assertNotNull(module);
        SchemaContext schemaContext = TestUtils.loadSchemaContext(allModules);
        controllerContext = spy( ControllerContext.getInstance() );
        controllerContext.setSchemas(schemaContext);

    }

    @Before
    public void initMethod()
    {
        restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setControllerContext( controllerContext );
    }

    /**
     * Test method invokeRpc in RestconfImpl class tests if composite node as
     * input parameter of method invokeRpc (second argument) is wrapped to
     * parent composite node which has QName equals to QName of rpc (resolved
     * from string - first argument).
     */
    @Test
    public void invokeRpcMtethodTest() {
        ControllerContext contContext = controllerContext;
        try {
            contContext.findModuleNameByNamespace(new URI("invoke:rpc:module"));
        } catch (URISyntaxException e) {
            assertTrue("Uri wasn't created sucessfuly", false);
        }

        BrokerFacade mockedBrokerFacade = mock(BrokerFacade.class);

        RestconfImpl restconf = RestconfImpl.getInstance();
        restconf.setBroker(mockedBrokerFacade);
        restconf.setControllerContext(contContext);

        CompositeNode payload = preparePayload();

        when(mockedBrokerFacade.invokeRpc(any(QName.class), any(CompositeNode.class)))
            .thenReturn( Futures.<RpcResult<CompositeNode>>immediateFuture(
                                               Rpcs.<CompositeNode>getRpcResult( true ) ) );

        StructuredData structData = restconf.invokeRpc("invoke-rpc-module:rpc-test", payload);
        assertTrue(structData == null);

    }

    private CompositeNode preparePayload() {
        MutableCompositeNode cont = NodeFactory.createMutableCompositeNode(
                TestUtils.buildQName("cont", "nmspc", "2013-12-04"), null, null, ModifyAction.CREATE, null);
        MutableSimpleNode<?> lf = NodeFactory.createMutableSimpleNode(
                TestUtils.buildQName("lf", "nmspc", "2013-12-04"), cont, "any value", ModifyAction.CREATE, null);
        cont.getValue().add(lf);
        cont.init();

        return cont;
    }

    @Test
    public void testInvokeRpcWithNoPayloadRpc_FailNoErrors() {
        RpcResult<CompositeNode> rpcResult = Rpcs.<CompositeNode>getRpcResult( false );

        BrokerFacade brokerFacade = mock(BrokerFacade.class);
        when( brokerFacade.invokeRpc(
                 eq(QName.create("(http://netconfcentral.org/ns/toaster?revision=2009-11-20)cancel-toast")),
                 any(CompositeNode.class)))
            .thenReturn( Futures.<RpcResult<CompositeNode>>immediateFuture( rpcResult ) );

        restconfImpl.setBroker(brokerFacade);

        try {
            restconfImpl.invokeRpc("toaster:cancel-toast", "");
            fail("Expected an exception to be thrown.");
        }
        catch (RestconfDocumentedException e) {
            verifyRestconfDocumentedException( e, 0, ErrorType.RPC, ErrorTag.OPERATION_FAILED,
                                               Optional.<String>absent(), Optional.<String>absent() );
        }
    }

    void verifyRestconfDocumentedException( final RestconfDocumentedException e, final int index,
                                            final ErrorType expErrorType, final ErrorTag expErrorTag,
                                            final Optional<String> expErrorMsg,
                                            final Optional<String> expAppTag ) {
        RestconfError actual = null;
        try {
            actual = e.getErrors().get( index );
        }
        catch( ArrayIndexOutOfBoundsException ex ) {
            fail( "RestconfError not found at index " + index );
        }

        assertEquals( "getErrorType", expErrorType, actual.getErrorType() );
        assertEquals( "getErrorTag", expErrorTag, actual.getErrorTag() );
        assertNotNull( "getErrorMessage is null", actual.getErrorMessage() );

        if( expErrorMsg.isPresent() ) {
            assertEquals( "getErrorMessage", expErrorMsg.get(), actual.getErrorMessage() );
        }

        if( expAppTag.isPresent() ) {
            assertEquals( "getErrorAppTag", expAppTag.get(), actual.getErrorAppTag() );
        }
    }

    @Test
    public void testInvokeRpcWithNoPayloadRpc_FailWithRpcError() {
        List<RpcError> rpcErrors = Arrays.asList(
            RpcErrors.getRpcError( null, "bogusTag", null, ErrorSeverity.ERROR, "foo",
                                   RpcError.ErrorType.TRANSPORT, null ),
            RpcErrors.getRpcError( "app-tag", "in-use", null, ErrorSeverity.WARNING, "bar",
                                   RpcError.ErrorType.RPC, null ));

        RpcResult<CompositeNode> rpcResult = Rpcs.<CompositeNode>getRpcResult( false, rpcErrors );

        BrokerFacade brokerFacade = mock(BrokerFacade.class);
        when( brokerFacade.invokeRpc(
                 eq(QName.create("(http://netconfcentral.org/ns/toaster?revision=2009-11-20)cancel-toast")),
                 any(CompositeNode.class)))
            .thenReturn( Futures.<RpcResult<CompositeNode>>immediateFuture( rpcResult ) );

        restconfImpl.setBroker(brokerFacade);

        try {
            restconfImpl.invokeRpc("toaster:cancel-toast", "");
            fail("Expected an exception to be thrown.");
        }
        catch (RestconfDocumentedException e) {
            verifyRestconfDocumentedException( e, 0, ErrorType.TRANSPORT, ErrorTag.OPERATION_FAILED,
                                               Optional.of( "foo" ), Optional.<String>absent() );
            verifyRestconfDocumentedException( e, 1, ErrorType.RPC, ErrorTag.IN_USE,
                                               Optional.of( "bar" ), Optional.of( "app-tag" ) );
        }
    }

    @Test
    public void testInvokeRpcWithNoPayload_Success() {
        RpcResult<CompositeNode> rpcResult = Rpcs.<CompositeNode>getRpcResult( true );

        BrokerFacade brokerFacade = mock(BrokerFacade.class);
        when( brokerFacade.invokeRpc(
                 eq(QName.create("(http://netconfcentral.org/ns/toaster?revision=2009-11-20)cancel-toast")),
                 any( CompositeNode.class )))
            .thenReturn( Futures.<RpcResult<CompositeNode>>immediateFuture( rpcResult ) );

        restconfImpl.setBroker(brokerFacade);

        StructuredData output = restconfImpl.invokeRpc("toaster:cancel-toast",
                "");
        assertEquals(null, output);
        //additional validation in the fact that the restconfImpl does not throw an exception.
    }

    @Test
    public void testInvokeRpcMethodExpectingNoPayloadButProvidePayload() {
        try {
            restconfImpl.invokeRpc("toaster:cancel-toast", " a payload ");
            fail("Expected an exception");
        } catch (RestconfDocumentedException e) {
            verifyRestconfDocumentedException( e, 0, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE,
                                               Optional.<String>absent(), Optional.<String>absent() );
        }
    }

    @Test
    public void testInvokeRpcMethodWithBadMethodName() {
        try {
            restconfImpl.invokeRpc("toaster:bad-method", "");
            fail("Expected an exception");
        }
        catch (RestconfDocumentedException e) {
            verifyRestconfDocumentedException( e, 0, ErrorType.RPC, ErrorTag.UNKNOWN_ELEMENT,
                                               Optional.<String>absent(), Optional.<String>absent() );
        }
    }

    @Test
    public void testInvokeRpcMethodWithInput() {
        RpcResult<CompositeNode> rpcResult = Rpcs.<CompositeNode>getRpcResult( true );

        CompositeNode payload = mock(CompositeNode.class);

        BrokerFacade brokerFacade = mock(BrokerFacade.class);
        when( brokerFacade.invokeRpc(
                 eq(QName.create("(http://netconfcentral.org/ns/toaster?revision=2009-11-20)make-toast")),
                 any(CompositeNode.class)))
            .thenReturn( Futures.<RpcResult<CompositeNode>>immediateFuture( rpcResult ) );

        restconfImpl.setBroker(brokerFacade);

        StructuredData output = restconfImpl.invokeRpc("toaster:make-toast",
                payload);
        assertEquals(null, output);
        //additional validation in the fact that the restconfImpl does not throw an exception.
    }

    @Test
    public void testThrowExceptionWhenSlashInModuleName() {
        try {
            restconfImpl.invokeRpc("toaster/slash", "");
            fail("Expected an exception.");
        }
        catch (RestconfDocumentedException e) {
            verifyRestconfDocumentedException( e, 0, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE,
                                               Optional.<String>absent(), Optional.<String>absent() );
        }
    }

    @Test
    public void testInvokeRpcWithNoPayloadWithOutput_Success() {
        CompositeNode compositeNode = mock( CompositeNode.class );
        RpcResult<CompositeNode> rpcResult = Rpcs.<CompositeNode>getRpcResult( true, compositeNode,
                                                            Collections.<RpcError>emptyList() );

        BrokerFacade brokerFacade = mock(BrokerFacade.class);
        when( brokerFacade.invokeRpc(
                        eq(QName.create("(http://netconfcentral.org/ns/toaster?revision=2009-11-20)testOutput")),
                        any( CompositeNode.class )))
            .thenReturn( Futures.<RpcResult<CompositeNode>>immediateFuture( rpcResult ) );

        restconfImpl.setBroker(brokerFacade);

        StructuredData output = restconfImpl.invokeRpc("toaster:testOutput", "");
        assertNotNull( output );
        assertSame( compositeNode, output.getData() );
        assertNotNull( output.getSchema() );
    }

    @Test
    public void testMountedRpcCallNoPayload_Success() throws Exception
    {
        RpcResult<CompositeNode> rpcResult = Rpcs.<CompositeNode>getRpcResult( true );

        ListenableFuture<RpcResult<CompositeNode>> mockListener = mock( ListenableFuture.class );
        when( mockListener.get() ).thenReturn( rpcResult );

        QName cancelToastQName = QName.create( "namespace", "2014-05-28", "cancelToast" );

        RpcDefinition mockRpc = mock( RpcDefinition.class );
        when( mockRpc.getQName() ).thenReturn( cancelToastQName );

        MountInstance mockMountPoint = mock( MountInstance.class );
        when( mockMountPoint.rpc( eq( cancelToastQName ), any( CompositeNode.class ) ) )
        .thenReturn( mockListener );

        InstanceIdWithSchemaNode mockedInstanceId = mock( InstanceIdWithSchemaNode.class );
        when( mockedInstanceId.getMountPoint() ).thenReturn( mockMountPoint );

        ControllerContext mockedContext = mock( ControllerContext.class );
        String cancelToastStr = "toaster:cancel-toast";
        when( mockedContext.urlPathArgDecode( cancelToastStr ) ).thenReturn( cancelToastStr );
        when( mockedContext.getRpcDefinition( cancelToastStr ) ).thenReturn( mockRpc );
        when( mockedContext.toMountPointIdentifier(  "opendaylight-inventory:nodes/node/"
                + "REMOTE_HOST/yang-ext:mount/toaster:cancel-toast" ) ).thenReturn( mockedInstanceId );

        restconfImpl.setControllerContext( mockedContext );
        StructuredData output = restconfImpl.invokeRpc(
                "opendaylight-inventory:nodes/node/REMOTE_HOST/yang-ext:mount/toaster:cancel-toast",
                "");
        assertEquals(null, output);

        //additional validation in the fact that the restconfImpl does not throw an exception.
    }
}
