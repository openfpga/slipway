// Copyright 2006-2007 all rights reserved; see LICENSE file for BSD-style license

package edu.berkeley.slipway.mpar;
import java.util.*;

/** a mapping from keys of type <tt>K</tt> to <i>sets</i> of values of type <tt>V</tt> */
public interface MapBag<K,V> extends Iterable<K> {
    public void add(K k, V v);
    public void addAll(K k, Iterable<V> iv);
    public Iterator<K> iterator();
}
