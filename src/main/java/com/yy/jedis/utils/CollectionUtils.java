package com.yy.jedis.utils;

import java.util.Collection;

/**
 * @author hoswey
 */
public class CollectionUtils {

  private CollectionUtils() {
  }

  public static boolean isEqual(Collection<?> c1, Collection<?> c2) {

    if (c1 == null && c2 != null || c1 != null && c2 == null) {
      return false;
    }

    if (c1 == c2) {
      return true;
    }

    return c1.containsAll(c2) && c2.containsAll(c1);
  }
}
