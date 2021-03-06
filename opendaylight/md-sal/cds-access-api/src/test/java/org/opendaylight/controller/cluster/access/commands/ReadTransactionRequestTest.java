/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;

public class ReadTransactionRequestTest extends AbstractReadTransactionRequestTest<ReadTransactionRequest> {
    private static final ReadTransactionRequest OBJECT = new ReadTransactionRequest(
            TRANSACTION_IDENTIFIER, 0, ACTOR_REF, PATH, SNAPSHOT_ONLY);

    @Override
    protected ReadTransactionRequest object() {
        return OBJECT;
    }

    @Test
    public void cloneAsVersionTest() throws Exception {
        final ABIVersion cloneVersion = ABIVersion.TEST_FUTURE_VERSION;
        final ReadTransactionRequest clone = OBJECT.cloneAsVersion(cloneVersion);
        Assert.assertEquals(cloneVersion, clone.getVersion());
        Assert.assertEquals(OBJECT.getPath(), clone.getPath());
        Assert.assertEquals(OBJECT.isSnapshotOnly(), clone.isSnapshotOnly());
    }

    @Override
    protected void doAdditionalAssertions(final Object deserialize) {
        Assert.assertTrue(deserialize instanceof ReadTransactionRequest);
        Assert.assertEquals(OBJECT.getReplyTo(), ((ReadTransactionRequest) deserialize).getReplyTo());
        Assert.assertEquals(OBJECT.getPath(), ((ReadTransactionRequest) deserialize).getPath());
    }
}