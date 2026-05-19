redis.replicate_commands()

local key = KEYS[1]

local maxTokens = 2
local refillRatePerNanosecond = 2 / (60 * 1000 * 1000 * 1000)
local ttlSeconds = 120 * 60

local redisTime = redis.call("TIME")
local currentTimeNanos = (tonumber(redisTime[1]) * 1000 * 1000 * 1000) + (tonumber(redisTime[2]) * 1000)

local bucket = redis.call("HMGET", key, "tokens", "lastUpdatedTimestamp")
local tokens = tonumber(bucket[1])
local lastUpdatedTimestamp = tonumber(bucket[2])

if tokens == nil or lastUpdatedTimestamp == nil then
    tokens = maxTokens
    lastUpdatedTimestamp = currentTimeNanos
else
    local elapsedNanos = currentTimeNanos - lastUpdatedTimestamp
    if elapsedNanos > 0 then
        tokens = math.min(maxTokens, tokens + (elapsedNanos * refillRatePerNanosecond))
        lastUpdatedTimestamp = currentTimeNanos
    end
end

local allowed = 0
if tokens >= 1 then
    tokens = tokens - 1
    allowed = 1
end

redis.call("HMSET",
        key,
        "tokens", tokens,
        "lastUpdatedTimestamp", lastUpdatedTimestamp,
        "maxTokens", maxTokens,
        "refillRatePerNanosecond", refillRatePerNanosecond)
redis.call("EXPIRE", key, ttlSeconds)

return allowed
