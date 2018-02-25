package org.apache.activemq.network.test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.activemq.broker.region.Destination;
import org.apache.activemq.broker.region.Subscription;
import org.apache.activemq.command.BrokerId;
import org.apache.activemq.command.ConsumerInfo;
import org.apache.activemq.command.Message;
import org.apache.activemq.command.NetworkBridgeFilter;
import org.apache.activemq.filter.MessageEvaluationContext;
import org.apache.activemq.network.ConditionalNetworkBridgeFilterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConditionalTestNetworkBridgeFilterFactory extends ConditionalNetworkBridgeFilterFactory {

   final static Logger LOG = LoggerFactory.getLogger(ConditionalTestNetworkBridgeFilterFactory.class);
   
   boolean selectorAware = false;

   public NetworkBridgeFilter create(ConsumerInfo info, BrokerId[] remoteBrokerPath, int messageTTL, int consumerTTL) {
      org.apache.activemq.network.test.ConditionalTestNetworkBridgeFilterFactory.ConditionalNetworkBridgeFilter filter = new org.apache.activemq.network.test.ConditionalTestNetworkBridgeFilterFactory.ConditionalNetworkBridgeFilter();
      filter.setNetworkBrokerId(remoteBrokerPath[0]);
      filter.setMessageTTL(messageTTL);
      filter.setConsumerTTL(consumerTTL);
      filter.setAllowReplayWhenNoConsumers(isReplayWhenNoConsumers());
      filter.setRateLimit(getRateLimit());
      filter.setRateDuration(getRateDuration());
      filter.setReplayDelay(getReplayDelay());
      filter.setSelectorAware(getSelectorAware());

      LOG.warn("**** TEST ConditionalTestNetworkBridgeFilterFactory created for network bridge.");

      return filter;
   }

   public void setSelectorAware(boolean selectorAware) {
      this.selectorAware = selectorAware;
   }

   public boolean getSelectorAware() {
      return selectorAware;
   }

   private static class ConditionalNetworkBridgeFilter extends NetworkBridgeFilter {


      private int rateLimit;
      private int rateDuration = 1000;
      private boolean allowReplayWhenNoConsumers = true;
      private int replayDelay = 1000;

      private int matchCount;
      private long rateDurationEnd;

      private boolean selectorAware = false;

      @Override
      protected boolean matchesForwardingFilter(Message message, final MessageEvaluationContext mec) {
         boolean match = true;
         if (mec.getDestination().isQueue() && contains(message.getBrokerPath(), networkBrokerId)) {
            // potential replay back to origin
            match = allowReplayWhenNoConsumers && hasNoLocalConsumers(message, mec) && hasNotJustArrived(message);

            if (match) {
               LOG.trace("Replaying [{}] for [{}] back to origin in the absence of a local consumer", message.getMessageId(), message.getDestination());
            } else {
               LOG.trace("Suppressing replay of [{}] for [{}] back to origin {}", new Object[]{message.getMessageId(), message.getDestination(), Arrays.asList(message.getBrokerPath())});
            }

         } else {
            // use existing filter logic for topics and non replays
            match = super.matchesForwardingFilter(message, mec);
         }

         if (match && rateLimitExceeded()) {
            LOG.trace("Throttled network consumer rejecting [{}] for [{}] {}>{}/{}", new Object[]{message.getMessageId(), message.getDestination(), matchCount, rateLimit, rateDuration});
            match = false;
         }

         return match;
      }

      private boolean hasNotJustArrived(Message message) {
         return replayDelay == 0 || (message.getBrokerInTime() + replayDelay < System.currentTimeMillis());
      }

      private boolean hasNoLocalConsumers(final Message message, final MessageEvaluationContext mec) {
         Destination regionDestination = (Destination) mec.getMessageReference().getRegionDestination();
         List<Subscription> consumers = regionDestination.getConsumers();
         for (Subscription sub : consumers) {
            if (!sub.getConsumerInfo().isNetworkSubscription() && !sub.getConsumerInfo().isBrowser()) {

               // selector aware has checks if the sub matches
               if (selectorAware) {
                  try {
                     if (sub.matches(message, mec)) {
                        LOG.debug(" (selector aware) Not replaying [{}] for [{}] to origin due to *matching* local consumer: {}", new Object[]{message.getMessageId(), message.getDestination(), sub.getConsumerInfo()});
                        return false;
                     }
                  } catch (IOException ex) {
                     //sub.matches() throws an exception
                     // so I *assume* it does NOT match - do nothing go to next sub
                  }

               } else {
                  // not selector aware (and has a sub) - that is enough, jump out.
                  LOG.debug(" (NOT selector aware) Not replaying [{}] for [{}] to origin due to local consumer: {}", new Object[]{message.getMessageId(), message.getDestination(), sub.getConsumerInfo()});
                  return false;
               }

            }
         }
         return true;
      }

      private boolean rateLimitExceeded() {
         if (rateLimit == 0) {
            return false;
         }

         if (rateDurationEnd < System.currentTimeMillis()) {
            rateDurationEnd = System.currentTimeMillis() + rateDuration;
            matchCount = 0;
         }
         return ++matchCount > rateLimit;
      }

      public void setReplayDelay(int replayDelay) {
         this.replayDelay = replayDelay;
      }

      public void setRateLimit(int rateLimit) {
         this.rateLimit = rateLimit;
      }

      public void setRateDuration(int rateDuration) {
         this.rateDuration = rateDuration;
      }

      public void setAllowReplayWhenNoConsumers(boolean allowReplayWhenNoConsumers) {
         this.allowReplayWhenNoConsumers = allowReplayWhenNoConsumers;
      }

      public void setSelectorAware(boolean selectorAware) {
         this.selectorAware = selectorAware;
      }
   }
}

