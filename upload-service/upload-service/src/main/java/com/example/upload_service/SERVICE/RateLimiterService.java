package com.example.upload_service.SERVICE;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


// implementing a rate limiter service
@Service
public class RateLimiterService {

    private static final int MAX_TOKENS=5;
    private static final long REFILL_INTERVAL_MS=10_000;

    private static final ConcurrentHashMap<String,Bucket> buckets=new ConcurrentHashMap<>();

    public static class Bucket{
        AtomicInteger tokens=new AtomicInteger(MAX_TOKENS);
        long lastRefillTimestamp=System.currentTimeMillis();
    }


    public boolean allowRequest(String clientId){

        Bucket bucket=buckets.computeIfAbsent(clientId,id->new Bucket());
        refillIfNeeded(bucket);

        if (bucket.tokens.get() > 0) {
            bucket.tokens.decrementAndGet();
            return true;
        }
        return false;


    }


    private void refillIfNeeded(Bucket bucket){

        long now=System.currentTimeMillis();
        long elapsed=now-System.currentTimeMillis();

        int tokensAdded=(int)(elapsed/REFILL_INTERVAL_MS);

        if(tokensAdded>0){
           bucket.tokens.set(Math.min(MAX_TOKENS,bucket.tokens.get()+tokensAdded));
           bucket.lastRefillTimestamp=now;
        }
    }


}
