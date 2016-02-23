package com.soagrowers.productview.configuration;

import org.axonframework.contextsupport.spring.AnnotationDriven;
import org.axonframework.eventhandling.*;
import org.axonframework.eventhandling.amqp.spring.ListenerContainerLifecycleManager;
import org.axonframework.eventhandling.amqp.spring.SpringAMQPConsumerConfiguration;
import org.axonframework.eventhandling.amqp.spring.SpringAMQPTerminal;
import org.axonframework.serializer.xml.XStreamSerializer;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Created by ben on 18/02/16.
 */
@Configuration
@AnnotationDriven
class AxonConfiguration {

    private static final String AMQP_CONFIG_KEY = "AMQP.Config";

    @Autowired
    public ConnectionFactory connectionFactory;

    @Autowired
    public PlatformTransactionManager transactionManager;

    @Value("${spring.application.queue}")
    private String queueName;

    @Value("${spring.application.terminal}")
    private String terminalName;


    @Bean
    XStreamSerializer xStreamSerializer() {
        return new XStreamSerializer();
    }

    @Bean
    ListenerContainerLifecycleManager listenerContainerLifecycleManager() {
        ListenerContainerLifecycleManager listenerContainerLifecycleManager = new ListenerContainerLifecycleManager();
        listenerContainerLifecycleManager.setConnectionFactory(connectionFactory);
        return listenerContainerLifecycleManager;
    }

    @Bean
    SpringAMQPConsumerConfiguration springAMQPConsumerConfiguration() {
        SpringAMQPConsumerConfiguration amqpConsumerConfiguration = new SpringAMQPConsumerConfiguration();
        amqpConsumerConfiguration.setTxSize(10);
        amqpConsumerConfiguration.setTransactionManager(transactionManager);
        amqpConsumerConfiguration.setQueueName(queueName);
        return amqpConsumerConfiguration;
    }


    @Bean
    SimpleCluster simpleCluster() {
        SimpleCluster simpleCluster = new SimpleCluster(queueName);
        simpleCluster.getMetaData().setProperty(AMQP_CONFIG_KEY, springAMQPConsumerConfiguration());
        return simpleCluster;
    }

    @Bean
    EventBusTerminal terminal() {
        SpringAMQPTerminal terminal = new SpringAMQPTerminal();
        terminal.setConnectionFactory(connectionFactory);
        terminal.setSerializer(xStreamSerializer());
        terminal.setExchangeName(terminalName);
        terminal.setListenerContainerLifecycleManager(listenerContainerLifecycleManager());
        terminal.setDurable(true);
        terminal.setTransactional(true);
        return terminal;
    }

    @Bean
    EventBus eventBus() {
        return new ClusteringEventBus(new DefaultClusterSelector(simpleCluster()), terminal());
    }

}