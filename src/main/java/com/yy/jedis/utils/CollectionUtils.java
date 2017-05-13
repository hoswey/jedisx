package com.yy.jedis.utils;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author hoswey
 */
public class CollectionUtils {

  private CollectionUtils() {
  }

  public static boolean isNotEmpty(Collection<?> collection) {
    return !isEmpty(collection);
  }

  public static boolean isEmpty(Collection<?> collection) {

    return collection == null || collection.isEmpty();
  }

  public static <T> ArrayList<T> concat(Collection<T> c1, Collection<T> c2) {

    ArrayList<T> arrayList = new ArrayList<>(c1.size() + c2.size());
    arrayList.addAll(c1);
    arrayList.addAll(c2);

    return arrayList;
  }

  public static boolean isEqual(Collection<?> c1, Collection<?> c2) {

//    System.out.println(1 + ", c1=" + c1 + ",c2=" + c2);
    if (c1 == null && c2 != null || c1 != null && c2 == null) {
      return false;
    }

    System.out.println(2);
    if (c1 == c2) {
      return true;
    }
    System.out.println(3 + " " + (c1.containsAll(c2) && c2.containsAll(c1)));

    return c1.containsAll(c2) && c2.containsAll(c1);
  }
}
