"""
BOOST — BytePlus AI Client — FIXED v2
──────────────────────────────────────
FIX: All 4 models now use BYTEPLUS_BASE_URL from .env
     Correct endpoint: https://ark.ap-southeast.bytepluses.com/api/v3

Model 1: GLM-4V vision       → /chat/completions (multimodal)
Model 2: DeepSeek-V3 reason  → /chat/completions (text)
Model 3: BytePlus image gen  → /images/generations
Model 4: BytePlus video gen  → /videos/generations
"""
import httpx
import json
import asyncio
import logging
from typing import Optional
from config import settings

logger = logging.getLogger(__name__)

TIMEOUT_STANDARD = 45.0
TIMEOUT_VIDEO    = 180.0

# ── Auth headers ──────────────────────────────────────────────────

def _headers() -> dict:
    return {
        "Authorization": f"Bearer {settings.BYTEPLUS_API_KEY}",
        "Content-Type": "application/json",
    }

# ── Base POST with full error logging ─────────────────────────────

async def _post(url: str, payload: dict, timeout: float = TIMEOUT_STANDARD) -> dict:
    logger.info(f"[BytePlus] POST {url}")
    async with httpx.AsyncClient(timeout=timeout) as client:
        try:
            resp = await client.post(url, json=payload, headers=_headers())
        except httpx.TimeoutException:
            logger.error(f"[BytePlus] Timeout {timeout}s → {url}")
            raise TimeoutError(f"AI model timed out after {int(timeout)}s. Please retry.")
        except httpx.RequestError as e:
            logger.error(f"[BytePlus] Network error → {url}: {e}")
            raise ConnectionError("Cannot reach AI service. Check connection.")

        if resp.status_code != 200:
            body = resp.text[:1000]
            logger.error(f"[BytePlus] HTTP {resp.status_code} → {url}\nBody: {body}")
            raise ValueError(f"AI service error {resp.status_code}: {body[:200]}")

        data = resp.json()

        # Log error codes embedded in 200 response
        err = data.get("error") or data.get("ResponseMetadata", {}).get("Error")
        if err:
            msg = err.get("message") or err.get("Message", "unknown")
            logger.error(f"[BytePlus] API error in response → {url}: {msg}")
            raise ValueError(f"AI processing failed: {msg}")

        return data


# ═══════════════════════════════════════════════════════════════════
# MODEL 1 — Vision LLM (GLM-4V)
# Endpoint: BYTEPLUS_BASE_URL/chat/completions
# ═══════════════════════════════════════════════════════════════════

async def analyze_product_vision(
    image_b64: str,
    niche: str,
    vibe: str,
    platform: str,
    product_name: Optional[str] = None,
) -> dict:
    name_hint = f"Product name: {product_name}. " if product_name else ""
    prompt = f"""{name_hint}Analyze this product image for TikTok affiliate marketing.
Niche: {niche}. Vibe: {vibe}. Platform: {platform}.

Return ONLY valid JSON, no markdown:
{{
  "product": "brief product description",
  "category": "product category",
  "buyer_persona": "target buyer (2 sentences)",
  "strongest_visual": "most eye-catching element",
  "pain_point": "problem this product solves",
  "content_potential": "high|medium|low",
  "angle_recommendation": "best content angle (1 sentence)"
}}"""

    # FIXED: use BYTEPLUS_BASE_URL from .env
    url = f"{settings.BYTEPLUS_BASE_URL}/chat/completions"
    payload = {
        "model": settings.BYTEPLUS_VISION_MODEL,
        "messages": [{
            "role": "user",
            "content": [
                {
                    "type": "image_url",
                    "image_url": {"url": f"data:image/jpeg;base64,{image_b64}"}
                },
                {"type": "text", "text": prompt}
            ]
        }],
        "max_tokens": 600,
        "temperature": 0.3,
    }

    data = await _post(url, payload)
    content = data.get("choices", [{}])[0].get("message", {}).get("content", "{}")
    clean = content.replace("```json", "").replace("```", "").strip()

    try:
        return json.loads(clean)
    except json.JSONDecodeError:
        logger.warning(f"[Model 1] JSON parse failed, using raw: {clean[:300]}")
        return {
            "angle_recommendation": clean[:200],
            "product": product_name or niche + " product",
            "content_potential": "medium",
            "buyer_persona": f"{niche} enthusiast",
        }


# ═══════════════════════════════════════════════════════════════════
# MODEL 2 — Reasoning LLM (DeepSeek-V3)
# Endpoint: BYTEPLUS_BASE_URL/chat/completions
# ═══════════════════════════════════════════════════════════════════

async def generate_content_strategy(
    vision_output: dict,
    niche: str,
    vibe: str,
    platform: str,
) -> dict:
    prompt = f"""You are a viral TikTok content strategist for affiliate marketers.

Product analysis:
{json.dumps(vision_output, indent=2)}

Target: {niche} niche, {vibe} vibe, {platform}.

Return ONLY valid JSON, no markdown:
{{
  "strategy": {{
    "angle": "best content angle",
    "audience": "target audience description",
    "content_potential": "high|medium|low",
    "tags": ["tag1", "tag2", "tag3"]
  }},
  "captions": [
    {{"hook": "under 7 words", "caption": "under 150 chars with emoji + hashtags"}},
    {{"hook": "...", "caption": "..."}},
    {{"hook": "...", "caption": "..."}},
    {{"hook": "...", "caption": "..."}},
    {{"hook": "...", "caption": "..."}}
  ],
  "hashtags": {{
    "viral":  ["#fyp","#viral","#foryou","#trending","#foryoupage","#explore","#reels","#tiktok","#viralvideo","#trending2025"],
    "niche":  ["10 niche hashtags for {niche}"],
    "brand":  ["#tiktokshop","#tiktokfinds","#affiliate","#linkinbio","#musthave","#productreview","#shopnow","#honest","#recommended","#deal"]
  }}
}}"""

    # FIXED: use BYTEPLUS_BASE_URL from .env
    url = f"{settings.BYTEPLUS_BASE_URL}/chat/completions"
    payload = {
        "model": settings.BYTEPLUS_REASONING_MODEL,
        "messages": [{"role": "user", "content": prompt}],
        "max_tokens": 1500,
        "temperature": 0.7,
    }

    data = await _post(url, payload)
    content = data.get("choices", [{}])[0].get("message", {}).get("content", "{}")
    clean = content.replace("```json", "").replace("```", "").strip()

    try:
        return json.loads(clean)
    except json.JSONDecodeError:
        logger.warning(f"[Model 2] JSON parse failed: {clean[:300]}")
        return {
            "strategy": {
                "angle": vision_output.get("angle_recommendation", "Show the product in use"),
                "audience": vision_output.get("buyer_persona", "General audience"),
                "content_potential": vision_output.get("content_potential", "medium"),
                "tags": [niche, vibe, platform],
            },
            "captions": [
                {"hook": "You need to see this", "caption": f"Obsessed with this {niche} find 🔥 #tiktokshop #affiliate #fyp"},
                {"hook": "Nobody talks about this", "caption": f"This {niche} product changed my routine ✨ Link in bio #musthave"},
                {"hook": "POV you finally found it", "caption": f"The {niche} find I didn't know I needed 😍 #tiktokfinds #linkinbio"},
                {"hook": "Wait until you see this", "caption": f"Rating this {niche} product 10/10 🏆 #honest #productreview #fyp"},
                {"hook": "Stop scrolling right now", "caption": f"This {niche} deal won't last long 👇 Link in bio #deal #affiliate"},
            ],
            "hashtags": {
                "viral": ["#fyp","#viral","#foryou","#trending","#foryoupage","#explore","#reels","#tiktok","#viralvideo","#trending2025"],
                "niche": [f"#{niche.lower().replace(' ','')}","#find","#haul","#review","#unboxing","#musthave","#obsessed","#routine","#trending","#tiktokmademebuyit"],
                "brand": ["#tiktokshop","#tiktokfinds","#affiliate","#linkinbio","#musthave","#productreview","#shopnow","#honest","#recommended","#deal"],
            }
        }


# ═══════════════════════════════════════════════════════════════════
# MODEL 3 — Image Generation (BytePlus)
# Endpoint: BYTEPLUS_BASE_URL/images/generations
# ═══════════════════════════════════════════════════════════════════

STYLE_PROMPTS = {
    "Lifestyle": "professional lifestyle product photography, aspirational scene, warm natural light, bokeh background, Instagram-ready, 8k quality, commercial photography style",
    "Studio":    "clean minimalist studio product photo, pure white background, sharp focus, professional studio lighting and shadows, commercial product photography, e-commerce ready",
    "Meme":      "bold viral meme-style product image, high contrast colors, white background, room for text overlay at top and bottom, viral social media format",
}

async def _generate_single_image(image_b64: str, style_label: str, context: str) -> Optional[str]:
    style_prompt = STYLE_PROMPTS.get(style_label, STYLE_PROMPTS["Lifestyle"])
    prompt = f"{style_prompt}. Product context: {context}."

    # FIXED: correct BytePlus Ark image gen endpoint
    url = f"{settings.BYTEPLUS_BASE_URL}/images/generations"
    payload = {
        "model": settings.BYTEPLUS_IMAGE_MODEL,
        "prompt": prompt,
        "image": image_b64,        # source image for img2img
        "n": 1,
        "size": "1024x1024",
        "response_format": "url",  # get URL back, not base64
    }

    try:
        data = await _post(url, payload, timeout=TIMEOUT_STANDARD)
        # Ark API response format: data[].url
        images = data.get("data", [])
        if images and images[0].get("url"):
            return images[0]["url"]
        # Fallback: b64_json
        if images and images[0].get("b64_json"):
            return f"data:image/jpeg;base64,{images[0]['b64_json']}"
        logger.warning(f"[Model 3] No image in response for {style_label}: {str(data)[:200]}")
        return None
    except Exception as e:
        logger.error(f"[Model 3] {style_label} failed: {e}")
        return None


async def generate_image_variants(image_b64: str, product_context: str) -> list[dict]:
    styles = list(STYLE_PROMPTS.keys())
    tasks  = [_generate_single_image(image_b64, s, product_context) for s in styles]
    results = await asyncio.gather(*tasks, return_exceptions=True)

    output = []
    for style, result in zip(styles, results):
        if isinstance(result, str) and result:
            output.append({"label": style, "url": result})
        else:
            logger.warning(f"[Model 3] Skipping {style}: {result}")

    if not output:
        raise ValueError("Image generation failed for all 3 variants. Please retry.")

    return output


# ═══════════════════════════════════════════════════════════════════
# MODEL 4 — Video Generation (BytePlus)
# Endpoint: BYTEPLUS_BASE_URL/videos/generations
# ═══════════════════════════════════════════════════════════════════

async def _generate_video(
    prompt: str,
    image_b64: Optional[str] = None,
    video_b64: Optional[str] = None,
) -> Optional[str]:
    # FIXED: correct BytePlus Ark video gen endpoint
    url = f"{settings.BYTEPLUS_BASE_URL}/videos/generations"
    payload = {
        "model": settings.BYTEPLUS_VIDEO_MODEL,
        "prompt": prompt,
        "duration": 6,
        "resolution": "720p",
        "ratio": "9:16",            # TikTok vertical format
    }

    if image_b64:
        payload["image"] = image_b64
    if video_b64:
        payload["video"] = video_b64

    try:
        data = await _post(url, payload, timeout=TIMEOUT_VIDEO)
        # Response: data[].url
        videos = data.get("data", [])
        if videos and videos[0].get("url"):
            return videos[0]["url"]
        logger.warning(f"[Model 4] No video URL in response: {str(data)[:200]}")
        return None
    except Exception as e:
        logger.error(f"[Model 4] Video gen failed: {e}")
        return None


async def generate_video_variants(
    hook_text: str,
    caption: str,
    cta_text: str,
    product_context: str,
    video_b64: Optional[str] = None,
    image_b64: Optional[str] = None,
) -> list[dict]:
    if not video_b64 and not image_b64:
        raise ValueError("Either video or image must be provided.")

    hook_prompt = (
        f"TikTok product video. Bold hook text overlay: '{hook_text}'. "
        f"Add animated captions. Professional vertical TikTok style. "
        f"Product: {product_context}."
    )
    cta_prompt = (
        f"TikTok product video. End with CTA: '{cta_text}'. "
        f"Caption overlay: '{caption[:100]}'. "
        f"Clean product showcase. Vertical 9:16 format. "
        f"Product: {product_context}."
    )

    hook_task = _generate_video(hook_prompt, image_b64=image_b64, video_b64=video_b64)
    cta_task  = _generate_video(cta_prompt,  image_b64=image_b64, video_b64=video_b64)

    hook_url, cta_url = await asyncio.gather(hook_task, cta_task)

    output = []
    if hook_url: output.append({"label": "Hook Version", "url": hook_url})
    if cta_url:  output.append({"label": "CTA Version",  "url": cta_url})

    if not output:
        raise ValueError("Video generation failed. Please retry.")

    return output
