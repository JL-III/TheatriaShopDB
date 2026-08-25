package com.playtheatria.shopdb.updater;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** LRU cache (ported from ShopDB-Updater). */
public class SimpleCache<K, V> {
    private final LinkedHashMap<K, V> map;

    public SimpleCache(int cacheSize) {
        map = new LinkedHashMap<K, V>(cacheSize * 10 / 9, 0.7f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > cacheSize;
            }
        };
    }

    public void put(K key, V value) {
        map.put(key, value);
    }

    public void clear() {
        map.clear();
    }

    public long size() {
        return map.size();
    }

    public Collection<V> values() {
        return map.values();
    }
}
