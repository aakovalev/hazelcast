package com.hazelcast.ringbuffer.impl.operations;

import com.hazelcast.config.Config;
import com.hazelcast.config.RingbufferConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.ringbuffer.Ringbuffer;
import com.hazelcast.ringbuffer.impl.RingbufferContainer;
import com.hazelcast.ringbuffer.impl.RingbufferService;
import com.hazelcast.spi.impl.NodeEngineImpl;
import com.hazelcast.spi.serialization.SerializationService;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.annotation.ParallelTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static com.hazelcast.ringbuffer.impl.operations.GenericOperation.OPERATION_CAPACITY;
import static com.hazelcast.ringbuffer.impl.operations.GenericOperation.OPERATION_HEAD;
import static com.hazelcast.ringbuffer.impl.operations.GenericOperation.OPERATION_REMAINING_CAPACITY;
import static com.hazelcast.ringbuffer.impl.operations.GenericOperation.OPERATION_SIZE;
import static com.hazelcast.ringbuffer.impl.operations.GenericOperation.OPERATION_TAIL;
import static org.junit.Assert.assertEquals;

@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelTest.class})
public class GenericOperationTest extends HazelcastTestSupport {
    private final static int CAPACITY = 10;

    private HazelcastInstance hz;
    private NodeEngineImpl nodeEngine;
    private Ringbuffer<Object> ringbuffer;
    private RingbufferContainer ringbufferContainer;
    private SerializationService serializationService;

    @Before
    public void setup() {
        RingbufferConfig rbConfig = new RingbufferConfig("foo")
                .setCapacity(CAPACITY)
                .setTimeToLiveSeconds(10);

        Config config = new Config().addRingBufferConfig(rbConfig);

        hz = createHazelcastInstance(config);
        nodeEngine = getNodeEngineImpl(hz);
        serializationService = nodeEngine.getSerializationService();
        ringbuffer = hz.getRingbuffer(rbConfig.getName());

        RingbufferService ringbufferService = getNodeEngineImpl(hz).getService(RingbufferService.SERVICE_NAME);
        ringbufferContainer = ringbufferService.getContainer(rbConfig.getName());
    }

    @Test
    public void size() throws Exception {
        ringbuffer.add("a");
        ringbuffer.add("b");

        GenericOperation op = new GenericOperation(ringbuffer.getName(), OPERATION_SIZE);
        op.setNodeEngine(nodeEngine);

        op.run();
        Long result = op.getResponse();
        assertEquals(new Long(ringbufferContainer.size()), result);
    }

    @Test
    public void capacity() throws Exception {
        ringbuffer.add("a");
        ringbuffer.add("b");

        GenericOperation op = new GenericOperation(ringbuffer.getName(), OPERATION_CAPACITY);
        op.setNodeEngine(nodeEngine);

        op.run();
        Long result = op.getResponse();
        assertEquals(new Long(CAPACITY), result);
    }

    @Test
    public void remainingCapacity() throws Exception {
        ringbuffer.add("a");
        ringbuffer.add("b");

        GenericOperation op = new GenericOperation(ringbuffer.getName(), OPERATION_REMAINING_CAPACITY);
        op.setNodeEngine(nodeEngine);

        op.run();
        Long result = op.getResponse();
        assertEquals(new Long(CAPACITY - 2), result);
    }

    @Test
    public void tail() throws Exception {
        ringbuffer.add("a");
        ringbuffer.add("b");

        GenericOperation op = new GenericOperation(ringbuffer.getName(), OPERATION_TAIL);
        op.setNodeEngine(nodeEngine);

        op.run();
        Long result = op.getResponse();
        assertEquals(new Long(ringbufferContainer.tailSequence()), result);
    }

    @Test
    public void head() throws Exception {
        for (int k = 0; k < CAPACITY * 2; k++) {
            ringbuffer.add("a");
        }

        GenericOperation op = new GenericOperation(ringbuffer.getName(), OPERATION_HEAD);
        op.setNodeEngine(nodeEngine);

        op.run();
        Long result = op.getResponse();
        assertEquals(new Long(ringbufferContainer.headSequence()), result);
    }

    public void serialize() {
        GenericOperation op = new GenericOperation(ringbuffer.getName(), OPERATION_HEAD);
        Data data = serializationService.toData(op);
        GenericOperation found = assertInstanceOf(GenericOperation.class, serializationService.toObject(data));

        assertEquals(op.operation, found.operation);
    }
}
