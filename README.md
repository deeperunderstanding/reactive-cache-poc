# Reactive Caching using Reactive Redis Integration and CacheFlux Reactor Extension

- Reactive Integration with Redis works fine and so does the CacheFlux / Mono
- None of the convenient Spring Magic available.
    - Need to manually configure Redis connection and create typed operation facades
    - Need to configure Redis for Caching (Setting Retention of Keys)
    - Need to come up with a key-schema

## TODO

- [x] Figure out how to set key expiration
    - using the expire() command on `ReactiveRedisOperation` 
    
- [x] generic way of creating typed redis templates