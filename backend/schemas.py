from pydantic import BaseModel
from typing import Optional, List, Dict, Any
from datetime import datetime
from uuid import UUID


# ── Device ────────────────────────────────────────────────────────

class DeviceResponse(BaseModel):
    device_id: str
    is_pro: bool
    subscription_expiry: Optional[datetime]

    class Config:
        from_attributes = True


# ── Quota ─────────────────────────────────────────────────────────

class QuotaItem(BaseModel):
    gen_type: str
    used: int
    limit: int
    remaining: int

class QuotaResponse(BaseModel):
    is_pro: bool
    date_utc: str
    quotas: List[QuotaItem]
    resets_at: str


# ── Generation ────────────────────────────────────────────────────

class AnalyzeRequest(BaseModel):
    device_id: str
    image_b64: Optional[str] = None   # base64 product image
    video_b64: Optional[str] = None   # base64 product video (seed-2-0-lite supports video)
    product_name: Optional[str] = None
    niche: str
    vibe: str
    platform: str = "TikTok"

class GenerateImagesRequest(BaseModel):
    device_id: str
    project_id: str
    image_b64: str              # original source image
    product_context: str        # from analysis

class GenerateVideoRequest(BaseModel):
    device_id: str
    project_id: str
    video_b64: Optional[str] = None    # source video (can be None if photo)
    image_b64: Optional[str]    # source image (if no video, img2video)
    hook_text: str
    caption: str
    cta_text: str = "Link in bio 👇"

class StrategyCard(BaseModel):
    angle: str
    audience: str
    content_potential: str
    tags: List[str]

class CaptionItem(BaseModel):
    hook: str
    caption: str

class ImageResult(BaseModel):
    label: str     # "Lifestyle" | "Studio" | "Meme"
    url: str

class VideoResult(BaseModel):
    label: str     # "Hook Version" | "CTA Version"
    url: str

class HashtagPack(BaseModel):
    viral: List[str]
    niche: List[str]
    brand: List[str]

class AnalyzeResponse(BaseModel):
    project_id: str
    strategy: StrategyCard
    captions: List[CaptionItem]
    hashtags: HashtagPack

class GenerateImagesResponse(BaseModel):
    project_id: str
    images: List[ImageResult]

class GenerateVideoResponse(BaseModel):
    project_id: str
    videos: List[VideoResult]


# ── Project ───────────────────────────────────────────────────────

class ProjectSummary(BaseModel):
    id: str
    product_name: Optional[str]
    niche: Optional[str]
    platform: Optional[str]
    image_count: int
    video_count: int
    caption_count: int
    created_at: datetime

    class Config:
        from_attributes = True

class ProjectDetail(BaseModel):
    id: str
    product_name: Optional[str]
    niche: Optional[str]
    vibe: Optional[str]
    platform: Optional[str]
    strategy: Optional[Dict]
    captions: Optional[List]
    image_urls: Optional[List]
    video_urls: Optional[List]
    hashtags: Optional[Dict]
    created_at: datetime

    class Config:
        from_attributes = True


# ── Trending ──────────────────────────────────────────────────────

class TrendItem(BaseModel):
    id: str
    emoji: str
    category: str
    trend: str
    heat: int
    growth: str
    is_pro_only: bool
    rank: int

    class Config:
        from_attributes = True


# ── Subscription ──────────────────────────────────────────────────

class VerifySubscriptionRequest(BaseModel):
    device_id: str
    purchase_token: str
    product_id: str = "boost_pro_monthly"
