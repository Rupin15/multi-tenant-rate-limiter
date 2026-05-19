redis.replicate_commands()

-- KEYS[1] = global bucket key, for example rate-limit:tenant-or-ip
--
-- ARGV[1] = maxTokens
-- ARGV[2] = refillTokensPerSecond
-- ARGV[3] = leaseSize
-- ARGV[4] = ttlSeconds

local bucketKey = KEYS[1]

local maxTokens = tonumber(ARGV[1])
local refillTokensPerSecond = tonumber(ARGV[2])/1000
local leaseSize = tonumber(ARGV[3])
local ttlSeconds = tonumber(ARGV[4])

if maxTokens == nil
        or refillTokensPerSecond == nil
        or leaseSize == nil
        or ttlSeconds == nil
then
    return redis.error_reply("missing rate limit policy")
end

local redisTime = redis.call("TIME")
local currentTimeMicros =
(tonumber(redisTime[1]) * 1000000) + tonumber(redisTime[2])

local refillRatePerMicrosecond = refillTokensPerSecond / 1000000

local bucket = redis.call(
        "HMGET",
        bucketKey,
        "tokens",
        "lastUpdatedMicros"
)

local tokens = tonumber(bucket[1])
local lastUpdatedMicros = tonumber(bucket[2])

if tokens == nil or lastUpdatedMicros == nil then
    tokens = maxTokens
    lastUpdatedMicros = currentTimeMicros
else
    local elapsedMicros = currentTimeMicros - lastUpdatedMicros

    if elapsedMicros > 0 then
        tokens = math.min(
                maxTokens,
                tokens + (elapsedMicros * refillRatePerMicrosecond)
        )

        lastUpdatedMicros = currentTimeMicros
    end
end

local leasedTokens = math.min(tokens, leaseSize)
leasedTokens = math.floor(leasedTokens)

tokens = tokens - leasedTokens

redis.call(
        "HSET",
        bucketKey,
        "tokens", tokens,
        "lastUpdatedMicros", lastUpdatedMicros,
        "maxTokens", maxTokens,
        "refillTokensPerSecond", refillTokensPerSecond,
        "leaseSize", leaseSize
)

redis.call("EXPIRE", bucketKey, ttlSeconds)

return leasedTokens
