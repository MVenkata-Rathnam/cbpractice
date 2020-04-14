package com.cb.dcp.practice;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.couchbase.client.dcp.Client;
import com.couchbase.client.dcp.ControlEventHandler;
import com.couchbase.client.dcp.DataEventHandler;
import com.couchbase.client.dcp.StreamFrom;
import com.couchbase.client.dcp.StreamTo;
import com.couchbase.client.dcp.transport.netty.ChannelFlowController;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.dcp.message.DcpMutationMessage;
import com.couchbase.client.dcp.message.DcpDeletionMessage;

/**
 * Hello world!
 *
 */
public class SimpleDCP 
{
    public static void main( String[] args ) throws InterruptedException
    {
    	// Connect to localhost and use the travel-sample bucket
    	final Client client = Client.configure()
    	    .hostnames("localhost")
    	    .credentials("Administrator", "Admin123")
    	    .bucket("testbucket")
    	    .build();

    	// Don't do anything with control events in this example
    	client.controlEventHandler(new ControlEventHandler() {
    	    public void onEvent(ChannelFlowController flowController, ByteBuf event) {
    	    	System.out.println("Control Event");
    	        event.release();
    	    }
    	});

        final AtomicLong totalSize = new AtomicLong(0);
        final AtomicLong docCount = new AtomicLong(0);

    	// Print out Mutations and Deletions
    	client.dataEventHandler(new DataEventHandler() {
    	    public void onEvent(ChannelFlowController flowController, ByteBuf event) {
    	        if (DcpMutationMessage.is(event)) {
    	            System.out.println("Mutation: " + DcpMutationMessage.toString(event));
    	            System.out.println(DcpMutationMessage.content(event).toString(CharsetUtil.UTF_8));
    	            docCount.incrementAndGet();
    	            totalSize.addAndGet(DcpMutationMessage.content(event).readableBytes());
    	            System.out.println("Document count: " + docCount);
    	            // You can print the content via DcpMutationMessage.content(event).toString(StandardCharsets.UTF_8);
    	        } else if (DcpDeletionMessage.is(event)) {
    	            System.out.println("Deletion: " + DcpDeletionMessage.toString(event));
    	        }
    	        event.release();
    	    }
    	});

    	// Connect the sockets
    	client.connect().await();

    	// Initialize the state (start now, never stop)
    	client.initializeState(StreamFrom.BEGINNING, StreamTo.NOW).await();

    	// Start streaming on all partitions
    	client.startStreaming().await();

    	// Sleep for some time to print the mutations
    	// The printing happens on the IO threads!
    	Thread.sleep(TimeUnit.MINUTES.toMillis(10));

    	// Once the time is over, shutdown.
    	client.disconnect().await();
    }
}
