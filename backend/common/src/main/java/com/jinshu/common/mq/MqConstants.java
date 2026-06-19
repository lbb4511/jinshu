package com.jinshu.common.mq;

/**
 * MQ 公共常量：死信交换机 / 死信队列。
 */
public final class MqConstants {

    /** 死信交换机 */
    public static final String DLX_EXCHANGE = "jinshu.dlx";

    /** 死信队列 */
    public static final String DLQ_QUEUE = "jinshu.dlq";

    /** 死信路由键 */
    public static final String DLX_ROUTING_KEY = "deadletter";

    private MqConstants() {}
}
