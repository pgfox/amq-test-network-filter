** NOTE : THIS HAS NOT BEEN TESTED FULLY **

A quick test, to show a modified ActiveMQ network Filter. In this case it is selectorAware and will only replay messages when
no 'matching' local subscriptions.

`
  <policyEntry queue=">" producerFlowControl="true" memoryLimit="1mb">
        <networkBridgeFilterFactory>
              <bean xmlns="http://www.springframework.org/schema/beans" class="org.apache.activemq.network.ConditionalTestNetworkBridgeFilterFactory">
                  <property name="replayWhenNoConsumers">
			                   <value>true</value>
		             </property>
                 <property name="selectorAware">
                        <value>true</value>
                </property>
              </bean>
        </networkBridgeFilterFactory>
   </policyEntry>

`

