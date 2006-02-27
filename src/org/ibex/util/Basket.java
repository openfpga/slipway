// Copyright 2000-2005 the Contributors, as shown in the revision logs.
// Licensed under the Apache Public Source License 2.0 ("the License").
// You may not use this file except in compliance with the License.

package org.ibex.util;

import java.io.Serializable;

public interface Basket extends Serializable {
    public boolean containsValue(Object object);
    public void clear();
    public int size();
    public void remove(Object object);

    public interface List extends Basket {
        public void add(Object object);
        public void add(int index, Object object);
        public Object set(int index, Object object);
        public Object get(int index);
        public Object remove(int index);
        public int indexOf(Object object);
        public void reverse();
        public void sort(CompareFunc c);
    }

    public interface RandomAccess extends List { }

    public interface Queue extends Basket {
        public void   enqueue(Object o);
        public Object dequeue();
    }

    public interface Stack extends Basket {
        public Object pop();
        public Object peek();
        public void push(Object object);
    }

    public interface Map extends Basket {
        public boolean containsKey(Object key);
        public Object get(Object key);
        public Object put(Object key, Object value);
    }

    public interface CompareFunc {
        public int compare(Object a, Object b);
    }


    // Implementations ////////////////////////////////////////////////////////

    public class Array implements RandomAccess, Stack, Queue {
        private static final long serialVersionUID = 1233428092L;

        private Object[] o;
        private int size = 0;

        public Array() { this(10); }
        public Array(int initialCapacity) { o = new Object[initialCapacity]; }
        public Array(Object entry) { this(1); add(entry); }

        public void   enqueue(Object o) { add(o); }

        // FEATURE: make this more efficient with general wraparound
        public Object dequeue() {
            if (size==0) return null;
            Object ret = o[0];
            for(int i=1; i<size; i++) o[i-1]=o[i];
            return ret;
        }

        public void add(Object obj) { add(size, obj); }
        public void add(int i, Object obj) {
            size(size + 1);
            if (size - 1 > i) System.arraycopy(o, i, o, size, size - i - 1);
            o[i] = obj; size++;
        }
        public Object set(int i, Object obj) {
            if (i >= o.length) throw new IndexOutOfBoundsException(
                "index "+i+" is beyond list boundary "+size);
            Object old = o[i]; o[i] = obj;
	    size = Math.max(i+1, size);
	    return old;
        }
        public Object get(int i) {
            if (i >= size) throw new IndexOutOfBoundsException(
                "index "+i+" is beyond list boundary "+size);
            return o[i];
        }
        public Object remove(int i) {
            if (i >= size || i < 0) throw new IndexOutOfBoundsException(
                "index "+i+" is beyond list boundary "+size);
            Object old = o[i]; o[i] = null;
            if (size - 1 > i) System.arraycopy(o, i + 1, o, i, size - i - 1);
            size--; return old;
        }
        public void remove(Object obj) { remove(indexOf(obj)); }

        public int indexOf(Object obj) {
            for (int i=0; i < size; i++)
                if ((obj == null && o[i] == null) || obj.equals(o[i])) return i;
            return -1;
        }

        public boolean containsValue(Object obj) {
            for (int i=0; i < size; i++)
                if ((obj == null && o[i] == null) || obj.equals(o[i])) return true;
            return false;
        }
        public void clear() { for (int i=0; i < size; i++) o[i] = null; size = 0; }
        public int size() { return size; }
        public void size(int s) {
            if (o.length >= s) return;
            Object[] newo = new Object[s];
            System.arraycopy(o, 0, newo, 0, size);
            o = newo;
        }

        public void reverse() {
            Object tmp; int max = (int)Math.floor((double)size / 2);
            for (int i=0; i < size; i++) { tmp = o[i]; o[i] = o[size - i]; o[size - i] = tmp; }
        }

        public void sort(CompareFunc c) { sort(this, null, c, 0, size); }

        public static void sort(Array a, Array b, CompareFunc c, int start, int end) {
            Object tmpa, tmpb = null;
            if(start >= end) return;
            if(end-start <= 6) {
                for(int i=start+1;i<=end;i++) {
                    tmpa = a.o[i];
                    if (b != null) tmpb = b.o[i];
                    int j;
                    for(j=i-1;j>=start;j--) {
                        if(c.compare(a.o[j],tmpa) <= 0) break;
                        a.o[j+1] = a.o[j];
                        if (b != null) b.o[j+1] = b.o[j];
                    }
                    a.o[j+1] = tmpa;
                    if (b != null) b.o[j+1] = tmpb;
                }
                return;
            }
            Object pivot = a.o[end];
            int lo = start - 1;
            int hi = end;
            do {
                while(c.compare(a.o[++lo],pivot) < 0) { }
                while((hi > lo) && c.compare(a.o[--hi],pivot) > 0) { }
                swap(a, lo,hi);
                if (b != null) swap(b, lo,hi);
            } while(lo < hi);

            swap(a, lo,end);
            if (b != null) swap(b, lo,end);
            sort(a, b, c, start, lo-1);
            sort(a, b, c, lo+1, end);
        }

        private static final void swap(Array vec, int a, int b) {
            if(a != b) {
                Object tmp = vec.o[a];
                vec.o[a] = vec.o[b];
                vec.o[b] = tmp;
            }
        }

        public Object peek() {
            if (size < 1) throw new IndexOutOfBoundsException("array is empty");
            return o[size - 1];
        }
        public Object pop() { return remove(size - 1); }
        public void push(Object o) { add(o); }
    }

    /** Implementation of a hash table using Radke's quadratic residue
     *  linear probing. Uses a single array to store all entries.
     *
     * <p>See C. Radke, Communications of the ACM, 1970, 103-105</p>
     *
     * @author adam@ibex.org, crawshaw@ibex.org
     */
    public class Hash implements Basket, Map {
        static final long serialVersionUID = 3948384093L;

        /** Used internally to record used slots. */
        final Object placeholder = new java.io.Serializable() { private static final long serialVersionUID = 1331L; };

        /** When <tt>loadFactor * usedslots > num_slots</tt>, call
         *  <tt>rehash()</tt>. */
        final float loadFactor;

        /** Used to determine the number of array slots required by each
         *  mapping entry. */
        final int indexmultiple;

        /** Number of currently active entries. */
        private int size = 0;

        /** Number of placeholders in entries[]. */
        private int placeholders = 0;

        /** Array of mappings.
         *
         *  <p>Each map requires multiple slots in the array, and subclasses
         *  can vary the number of required slots without reimplementing all
         *  the functions of this class by changing the value of
         *  <tt>indexmultiple</tt>.</p>
         *
         *  Default implementation uses <tt>indexmultiple == 1</tt>, and
         *  stores only the keys in <tt>entries</tt>.
         */
        private Object[] entries = null;

        public Hash() { this(16, 0.75F); }
        public Hash(int cap, float load) { this(2, cap, load); }
        public Hash(int indexmultiple, int initialCapacity, float loadFactor) {
            // using a pseudoprime in the form 4x+3 ensures full coverage
            initialCapacity = (initialCapacity / 4) * 4 + 3;
            this.loadFactor = loadFactor;
            this.indexmultiple = indexmultiple;
            this.entries = new Object[initialCapacity * indexmultiple];
        }

        public int size() { return size; }
        public void clear() { for (int i = 0; i<entries.length; i++) entries[i] = null; size = 0; }

        public boolean containsKey(Object k) { return indexOf(k) >= 0; }

        /** <b>Warning:</b> This function is equivalent here to
         *  <tt>containsKey()</tt>. For a value map, use Basket.HashMap. */
        public boolean containsValue(Object k) { return containsKey(k); }


        // UGLY
        public Object[] dumpkeys() {
            Object[] ret = new Object[size];
            int pos = 0;
            for(int i=0; i<entries.length; i+=indexmultiple)
                if (placeholder!=entries[i] && entries[i]!=null) {
                    ret[pos++] = entries[i];
                }
            return ret;
        }

        public void remove(Object k) { remove(indexOf(k)); }
        public void remove(int dest) {
            if (dest < 0) return;
            // instead of removing, insert a placeholder
            entries[dest] = placeholder;
            for (int inc=1; inc < indexmultiple; inc++) entries[dest + inc] = null;
            size--;
            placeholders++;
        }

        public Object get(Object key) { return get(key, 0); }
        public Object get(Object key, int whichval) {
            int i = indexOf(key);
            return i >= 0 ? entries[i + 1 + whichval] : null;
        }

        public Object put(Object key, Object value) { return put(key, value, 0); }
        public Object put(Object key, Object value, int whichval) {
            if (loadFactor * (size + placeholders) > entries.length) rehash();
            int dest = indexOf(key);
            Object old = null;
            if (dest < 0) {
                dest = -1 * (dest + 1);
                if (entries[dest] != placeholder) size++;
                entries[dest] = key;
                for(int i=1; i<indexmultiple; i++) entries[dest+i] = null;
            } else {
                old = entries[dest + 1 + whichval];
            }
            entries[dest + 1 + whichval] = value;
            return old;
        }

        /*
        public boolean containsKey(Object k) { return super.containsValue(k); }
        public boolean containsValue(Object v) {
            for (int i=0; i < entries.length/indexmultiple; i++)
                if ((i == 0 || entries[i * indexmultiple] != null) && // exception for null key
                    !equals(placeholder, entries[i * indexmultiple]) &&
                    v == null ? entries[i + 1] == null : v.equals(entries[i + 1]))
                        return true;
            return false;
        }
        */

        /** Compares two keys for equality. <tt>userKey</tt> is the key
         *  passed to the map, <tt>storedKey</tt> is from the map.
         *
         * <p>Default implementation provides standard Java equality
         * testing, <tt>k1 == null ? k2 == null : k1.equals(k2)</tt>.</p>
         */
        protected boolean equals(Object userKey, Object storedKey) {
            return userKey == null ? storedKey == null : userKey.equals(storedKey);
        }

        /** Returns the array position in <tt>entries</tt>, adjusted for
         *  <tt>indexmultiple</tt>, where <tt>k</tt> is/should be stored
         *  using Radke's quadratic residue linear probing. 
         *
         *  <p><tt>entries[0]</tt> is a hard coded exception for the null
         *  key.</p>
         *  
         * <p>If the key is not found, this function returns
         * <tt>(-1 * indexPosition) - 1</tt>, where <tt>indexPosition</tt>
         * is the array position where the mapping should be stored.</p>
         *
         * <p>Uses <tt>placeholder</tt> as a placeholder object, and
         * compares keys using <tt>equals(Object, Object)</tt>.</p>
         */
        private int indexOf(Object k) {
            // special case null key
            if (k == null) return equals(placeholder, entries[0]) ? -1 : 0;

            int hash = k == null ? 0 : k.hashCode();
            final int orig = Math.abs(hash) % (entries.length / indexmultiple);
            int dest = orig * indexmultiple;
            int tries = 1;
            boolean plus = true;

            while (entries[dest] != null) {
                if (equals(k, entries[dest])) return dest;
                dest = Math.abs((orig + (plus ? 1 : -1) * tries * tries) % (entries.length / indexmultiple)) * indexmultiple;
                if (plus) tries++;
                plus = !plus;
            }
            return -1 * dest - 1;
        }

        /** Doubles the available entry space, first by packing the data
         *  set (removes <tt>placeholder</tt> references) and if necessary
         *  by increasing the size of the <tt>entries</tt> array.
         */
        private void rehash() {
            Object[] oldentries = entries;
            entries = new Object[oldentries.length * indexmultiple];

            for (int i=0; i < (oldentries.length/indexmultiple); i++) {
                int pos = i * indexmultiple;
                if (pos > 0 && oldentries[pos] == null) continue;
                if (oldentries[pos] == placeholder) continue;

                // dont adjust any of the support entries
                int dest = indexOf(oldentries[pos]);
                dest = -1 * dest - 1; size++;  // always new entry
                for (int inc=0; inc < indexmultiple; inc++)
                    entries[dest + inc] = oldentries[pos + inc];
            }
            placeholders = 0;
        }
    }

    // FIXME, BalancedTree goes here

}
