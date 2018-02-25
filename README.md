** NOTE : THIS HAS NOT BEEN TESTED FULLY AND IS NOT INTENDED FOR USE **

A quick test, to show a modified ActiveMQ network Filter. In this case it is selectorAware and will only replay messages when
no 'matching' local subscriptions.

>>  <policyEntry queue=">" producerFlowControl="true" memoryLimit="1mb">
>>        <networkBridgeFilterFactory>
>>              <bean xmlns="http://www.springframework.org/schema/beans" class="org.apache.activemq.network.test.ConditionalTestNetworkBridgeFilterFactory">
>>                  <property name="replayWhenNoConsumers">
>>			                   <value>true</value>
>>		             </property>
>>                 <property name="selectorAware">
>>                        <value>true</value>
>>                </property>
>>              </bean>
>>        </networkBridgeFilterFactory>
>>   </policyEntry>



**To install in AMQ 6.x (karaf, osgi)**

The  amq-test-network-filter-1.0-SNAPSHOT.jar is a fragment bundle to be hosted by bundle "org.apache.activemq.activemq-osgi" (should be already installed in AMQ 6)

1) install the fragment on the karaf console of AMQ6

>> JBossA-MQ:karaf@root> osgi:install -s file:///<YOUR_FULL_PATH>/amq-test-network-filter/target/amq-test-network-filter-1.0-SNAPSHOT.jar

2) This will result in a message like the following

>>   Bundle ID: 195
>>   Error executing command: Error installing bundles:
>>   	Unable to start bundle file:///<YOUR_FULL_PATH>/amq-test-network-filter/target/amq-test-network-filter-1.0-SNAPSHOT.jar: Fragment bundles can not be started.

3) Restart the AMQ6 karaf container.

4) modify the activemq.xml to include the policy entry config above

5) Restart the AMQ6 karaf container again (just to ensure config changes fully picked up)

6) after the network has been established and a consumer has created a demand across the network (to a remote queue) you will see the following WARN message in the log

>> **** TEST ConditionalTestNetworkBridgeFilterFactory created for network bridge.

