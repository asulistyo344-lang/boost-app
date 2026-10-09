import redis.asyncio as aioredis
from config import settings

redis_pool = None

async def get_redis() -> aioredis.Redis:
    global redis_pool
    if redis_pool is None:
        redis_pool = aioredis.from_url(
            settings.REDIS_URL,
            encoding="utf-8",
            decode_responses=True,
        )
    return redis_pool

async def close_redis():
    global redis_pool
    if redis_pool:
        await redis_pool.close()

# ── Quota limits ──────────────────────────────────────────────────
FREE_LIMITS = {
    "analysis": 3,
    "image":    1,
    "video":    1,
}
PRO_DAILY_CAP = 50   # even Pro capped to control BytePlus costs

async def get_quota(device_id: str, date_str: str, gen_type: str) -> int:
    r = await get_redis()
    key = f"boost:quota:{device_id}:{date_str}:{gen_type}"
    val = await r.get(key)
    return int(val) if val else 0

async def increment_quota(device_id: str, date_str: str, gen_type: str) -> int:
    r = await get_redis()
    key = f"boost:quota:{device_id}:{date_str}:{gen_type}"
    count = await r.incr(key)
    await r.expire(key, 86400)
    return count

async def check_quota(
    device_id: str, date_str: str,
    gen_type: str, is_pro: bool
) -> tuple[int, bool]:
    """Returns (used, is_exceeded)"""
    used = await get_quota(device_id, date_str, gen_type)
    limit = PRO_DAILY_CAP if is_pro else FREE_LIMITS.get(gen_type, 0)
    return used, used >= limit
