// Copyright 2006-2007 all rights reserved; see LICENSE file for BSD-style license

package edu.berkeley.slipway.mpar;
import java.util.*;

/** a mapping from keys of type <tt>K</tt> to <i>sets</i> of values of type <tt>T</tt> */
public final class HashMapBag<K,V> implements MapBag<K,V> {

    private final HashMap<K,HashSet<V>> hm = new HashMap<K,HashSet<V>>();

    public void add(K k, V v) {
        HashSet<V> hs = hm.get(k);
        if (hs==null) hm.put(k, hs = new HashSet<V>());
        size -= hs.size();
        hs.add(v);
        size += hs.size();
    }

    public void addAll(K k, Iterable<V> iv) {
        for(V v : iv) add(k, v);
    }

    public int size(K k) {
        HashSet<V> ret = hm.get(k);
        return ret==null ? 0 : ret.size();
    }
    public HashSet<V> getAll(K k) {
        HashSet<V> ret = hm.get(k);
        if (ret==null) return new HashSet<V>();
        return ret;
    }

    public void remove(K k, V v) {
        if (hm.get(k)==null) return;
        HashSet<V> hs = hm.get(k);
        if (hs==null) return;
        size -= hs.size();
        hs.remove(v);
        size += hs.size();
    }

    public void removeAll(K k, Iterable<V> iv) {
        for(V v : iv) remove(k, v);
    }

    public void clear() { hm.clear(); }

    public boolean contains(K k, V v) {
        return hm.get(k)!=null && hm.get(k).contains(v);
    }

    public void addAll(HashMapBag<K,V> hmb) {
        for(K k : hmb) addAll(k, hmb.getAll(k));
    }
    public void removeAll(HashMapBag<K,V> hmb) {
        for(K k : hmb) removeAll(k, hmb.getAll(k));
    }

    public Iterator<K> iterator() { return hm.keySet().iterator(); }
    public int size() { return size; }
    private int size = 0;
}
