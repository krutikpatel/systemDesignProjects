package com.ratelimiter.store;

import com.ratelimiter.model.Errors.StoreException;
import java.util.List;

public interface StoreAdapter {
    String get(String key) throws StoreException;

    void set(String key, String value, long ttlMs) throws StoreException;

    Object eval(String script, List<String> keys, List<String> args) throws StoreException;

    boolean ping();

    void close();
}
