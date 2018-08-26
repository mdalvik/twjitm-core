package com.twjitm.core.common.process.udp;import com.twjitm.core.common.config.global.GlobalConstants;import com.twjitm.core.common.config.global.NettyGameServiceConfigService;import com.twjitm.core.common.config.global.NettyGameUdpConfig;import com.twjitm.core.common.enums.MessageAttributeEnum;import com.twjitm.core.common.netstack.entity.AbstractNettyNetMessage;import com.twjitm.core.common.netstack.entity.udp.AbstractNettyNetProtoBufUdpMessage;import com.twjitm.core.common.netstack.session.udp.NettyUdpSession;import com.twjitm.core.common.process.IMessageProcessor;import com.twjitm.core.common.process.NettyNetMessageProcessLogic;import com.twjitm.core.spring.SpringServiceManager;import com.twjitm.threads.common.executor.NettyOrderThreadPoolExecutor;import com.twjitm.threads.entity.AbstractNettyTask;import org.slf4j.Logger;import org.slf4j.LoggerFactory;import org.springframework.stereotype.Service;/** * @author EGLS0807 - [Created on 2018-08-26 10:29] * @company http://www.g2us.com/ * @jdk java version "1.8.0_77" */@Servicepublic class NettyUdpOrderNetProtoMessageProcessor implements IMessageProcessor {    private Logger logger = LoggerFactory.getLogger(NettyUdpOrderNetProtoMessageProcessor.class);    /**     * 处理池     */    private NettyOrderThreadPoolExecutor threadPoolExecutor;    /**     * 任务队列大小     */    private int taskSize;    @Override    public void startup() {        NettyGameServiceConfigService serviceConfig = SpringServiceManager.getSpringLoadService().getNettyGameServiceConfigService();        NettyGameUdpConfig udpConfig = serviceConfig.getUdpConfig();        this.taskSize = udpConfig.getUpdQueueMessageProcessWorkerSize();        this.threadPoolExecutor = new NettyOrderThreadPoolExecutor(GlobalConstants.Thread.MESSAGE_UDP_ORDER_EXECUTOR, this.taskSize, this.taskSize);        logger.info("NettyUdpOrderNetProtoMessageProcessor IS START ,THIS SIZE =" + taskSize);    }    @Override    public void shutdown() {        if (threadPoolExecutor != null) {            threadPoolExecutor.shutdown();            logger.info("NettyUdpOrderNetProtoMessageProcessor IS STOP");        }    }    @Override    public void put(AbstractNettyNetMessage msg) {        if (msg != null) {            if (msg instanceof AbstractNettyNetProtoBufUdpMessage) {                AbstractNettyNetProtoBufUdpMessage udpMessage = (AbstractNettyNetProtoBufUdpMessage) msg;                UdpNettyNetMessageTask task = new UdpNettyNetMessageTask(udpMessage);                int index = (int) (udpMessage.getPlayerId() % taskSize);                threadPoolExecutor.addTask(index, task);            }        }    }    @Override    public boolean isFull() {        return false;    }    /**     * udp网络数据包提交任务     */    private class UdpNettyNetMessageTask extends AbstractNettyTask {        private AbstractNettyNetMessage netMessage;        public UdpNettyNetMessageTask(AbstractNettyNetMessage netMessage) {            this.netMessage = netMessage;        }        @Override        public void run() {            AbstractNettyNetProtoBufUdpMessage udpMessage = (AbstractNettyNetProtoBufUdpMessage) netMessage;            NettyUdpSession session = (NettyUdpSession) udpMessage.getAttribute(MessageAttributeEnum.DISPATCH_SESSION);            if (session != null) {                NettyNetMessageProcessLogic messageProcessLogic;                messageProcessLogic = SpringServiceManager.getSpringLoadService().getNettyNetMessageProcessLogic();                messageProcessLogic.processTcpMessage(udpMessage, session);            }        }    }}