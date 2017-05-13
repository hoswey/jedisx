package com.yy.jedis.selector;

import com.yy.jedis.JedisServer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * @author hoswey
 */
public class LatencyMinimizingServerSelector implements ServerSelector {

  private static final int DEFAULT_ACCEPTABLE_LATENCY_DIFFERENCE = 5;

  private final long acceptableLatencyDifferenceNanos;

  public LatencyMinimizingServerSelector() {
    this(DEFAULT_ACCEPTABLE_LATENCY_DIFFERENCE, TimeUnit.MILLISECONDS);
  }

  /**
   * @param acceptableLatencyDifference the maximum difference in ping-time latency between the
   * fastest ping time and the slowest of the chosen servers
   * @param timeUnit the time unit of the acceptableLatencyDifference
   */
  public LatencyMinimizingServerSelector(final long acceptableLatencyDifference,
      final TimeUnit timeUnit) {
    this.acceptableLatencyDifferenceNanos = NANOSECONDS
        .convert(acceptableLatencyDifference, timeUnit);
  }

  @Override
  public List<JedisServer> select(List<JedisServer> jedisServers) {

    return getServersWithAcceptableLatencyDifference(jedisServers,
        getFastestRoundTripTimeNanos(jedisServers));
  }

  private List<JedisServer> getServersWithAcceptableLatencyDifference(

      final List<JedisServer> servers,
      final long bestPingTime) {

    List<JedisServer> goodSecondaries = new ArrayList<>(servers.size());

    for (final JedisServer cur : servers) {
      if (cur.getRoundTripTimeNanos() - acceptableLatencyDifferenceNanos <= bestPingTime) {
        goodSecondaries.add(cur);
      }
    }
    return goodSecondaries;
  }

  private long getFastestRoundTripTimeNanos(final List<JedisServer> members) {

    long fastestRoundTripTime = Long.MAX_VALUE;
    for (final JedisServer cur : members) {
      if (cur.getRoundTripTimeNanos() < fastestRoundTripTime) {
        fastestRoundTripTime = cur.getRoundTripTimeNanos();
      }
    }
    return fastestRoundTripTime;
  }
}
