package bsearch.util;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

//TODO: Add cache size limit option to fitness caching?

//Courtesy of http://www.codewalk.com/2012/04/least-recently-used-lru-cache-implementation-java.html
@SuppressWarnings("serial")
public class LRUCache < K, V > extends LinkedHashMap < K, V > {
 
	private int capacity; // Maximum number of items in the cache.
     
    public LRUCache(int capacity) { 
        super(capacity+1, 1.0f, true); // Pass 'true' for accessOrder.
        this.capacity = capacity;
    }
     
    protected boolean removeEldestEntry(Entry<K, V> entry) {
        return (size() > this.capacity);
    } 
}
