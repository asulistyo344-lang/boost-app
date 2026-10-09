from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, desc, func
from datetime import datetime, timezone
from typing import Optional
import uuid

from database import get_db
from models import BoostDevice, BoostProject, BoostGeneration, TrendingProduct, GenType
from schemas import (
    DeviceResponse, QuotaResponse, QuotaItem,
    AnalyzeRequest, AnalyzeResponse, StrategyCard, CaptionItem, HashtagPack,
    GenerateImagesRequest, GenerateImagesResponse, ImageResult,
    GenerateVideoRequest, GenerateVideoResponse, VideoResult,
    ProjectSummary, ProjectDetail,
    TrendItem, VerifySubscriptionRequest,
)
from redis_client import get_quota, increment_quota, check_quota, FREE_LIMITS, PRO_DAILY_CAP
from byteplus_client import (
    analyze_product_vision, generate_content_strategy,
    generate_image_variants, generate_video_variants,
)

router = APIRouter()

# ── Health ────────────────────────────────────────────────────────

@router.get("/health")
async def health(db: AsyncSession = Depends(get_db)):
    try:
        await db.execute(select(func.now()))
        db_status = "connected"
    except Exception:
        db_status = "error"
    return {"status": "ok", "app": "Boost", "database": db_status}

# ── Device ────────────────────────────────────────────────────────

@router.post("/device/register", response_model=DeviceResponse)
async def register_device(
    device_id: str,
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(BoostDevice).where(BoostDevice.device_id == device_id)
    )
    device = result.scalar_one_or_none()
    if not device:
        device = BoostDevice(device_id=device_id)
        db.add(device)
        await db.commit()
        await db.refresh(device)
    return DeviceResponse.model_validate(device)

@router.get("/device/{device_id}", response_model=DeviceResponse)
async def get_device(device_id: str, db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(BoostDevice).where(BoostDevice.device_id == device_id)
    )
    device = result.scalar_one_or_none()
    if not device:
        # Auto-register on first get
        device = BoostDevice(device_id=device_id)
        db.add(device)
        await db.commit()
        await db.refresh(device)
    return DeviceResponse.model_validate(device)

# ── Quota ─────────────────────────────────────────────────────────

@router.get("/quota/{device_id}", response_model=QuotaResponse)
async def get_device_quota(device_id: str, db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(BoostDevice).where(BoostDevice.device_id == device_id)
    )
    device = result.scalar_one_or_none()
    is_pro = device.is_pro if device else False
    date_str = datetime.now(timezone.utc).strftime("%Y-%m-%d")

    quotas = []
    for gen_type in ["analysis", "image", "video"]:
        used = await get_quota(device_id, date_str, gen_type)
        limit = PRO_DAILY_CAP if is_pro else FREE_LIMITS[gen_type]
        quotas.append(QuotaItem(
            gen_type=gen_type,
            used=used,
            limit=limit,
            remaining=max(0, limit - used),
        ))

    return QuotaResponse(
        is_pro=is_pro,
        date_utc=date_str,
        quotas=quotas,
        resets_at=date_str,
    )

# ── ANALYZE ───────────────────────────────────────────────────────

@router.post("/analyze", response_model=AnalyzeResponse)
async def analyze_product(
    body: AnalyzeRequest,
    db: AsyncSession = Depends(get_db),
):
    # Get or create device
    result = await db.execute(
        select(BoostDevice).where(BoostDevice.device_id == body.device_id)
    )
    device = result.scalar_one_or_none()
    if not device:
        device = BoostDevice(device_id=body.device_id)
        db.add(device)
        await db.flush()

    is_pro = device.is_pro
    date_str = datetime.now(timezone.utc).strftime("%Y-%m-%d")

    # Quota check
    used, exceeded = await check_quota(body.device_id, date_str, "analysis", is_pro)
    if exceeded:
        limit = PRO_DAILY_CAP if is_pro else FREE_LIMITS["analysis"]
        raise HTTPException(
            status_code=429,
            detail=f"Daily analysis limit reached ({limit}/day). Resets at midnight UTC."
        )

    # Validate inputs
    if not body.image_b64 and not getattr(body, "video_b64", None):
        raise HTTPException(
            status_code=400,
            detail="Either image or video data is required."
        )
    if not body.niche:
        raise HTTPException(status_code=400, detail="Niche is required.")
    if not body.vibe:
        raise HTTPException(status_code=400, detail="Vibe is required.")

    # Run Models 1 + 2
    try:
        vision = await analyze_product_vision(
            image_b64=body.image_b64,
            niche=body.niche,
            vibe=body.vibe,
            platform=body.platform,
            product_name=body.product_name,
            video_b64=getattr(body, "video_b64", None),
        )
        strategy_data = await generate_content_strategy(
            vision, body.niche, body.vibe, body.platform
        )
    except TimeoutError as e:
        raise HTTPException(status_code=503, detail=str(e))
    except ValueError as e:
        raise HTTPException(status_code=502, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=503, detail="AI analysis failed. Please retry.")

    # Save project
    project = BoostProject(
        device_id=body.device_id,
        product_name=body.product_name,
        niche=body.niche,
        vibe=body.vibe,
        platform=body.platform,
        strategy=strategy_data.get("strategy"),
        captions=strategy_data.get("captions"),
        hashtags=strategy_data.get("hashtags"),
    )
    db.add(project)

    # Log generation
    db.add(BoostGeneration(
        device_id=body.device_id,
        project_id=project.id,
        gen_type=GenType.ANALYSIS,
        date_utc=date_str,
    ))

    await db.commit()
    await increment_quota(body.device_id, date_str, "analysis")

    strat = strategy_data.get("strategy", {})
    caps  = strategy_data.get("captions", [])
    tags  = strategy_data.get("hashtags", {})

    return AnalyzeResponse(
        project_id=str(project.id),
        strategy=StrategyCard(
            angle=strat.get("angle", ""),
            audience=strat.get("audience", ""),
            content_potential=strat.get("content_potential", "medium"),
            tags=strat.get("tags", []),
        ),
        captions=[CaptionItem(hook=c["hook"], caption=c["caption"]) for c in caps],
        hashtags=HashtagPack(
            viral=tags.get("viral", []),
            niche=tags.get("niche", []),
            brand=tags.get("brand", []),
        ),
    )

# ── GENERATE IMAGES ───────────────────────────────────────────────

@router.post("/generate-images", response_model=GenerateImagesResponse)
async def generate_images(
    body: GenerateImagesRequest,
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(BoostDevice).where(BoostDevice.device_id == body.device_id)
    )
    device = result.scalar_one_or_none()
    is_pro = device.is_pro if device else False
    date_str = datetime.now(timezone.utc).strftime("%Y-%m-%d")

    used, exceeded = await check_quota(body.device_id, date_str, "image", is_pro)
    if exceeded:
        limit = PRO_DAILY_CAP if is_pro else FREE_LIMITS["image"]
        raise HTTPException(
            status_code=429,
            detail=f"Daily image generation limit reached ({limit}/day). Upgrade to Pro for unlimited."
        )

    if not body.image_b64 and not getattr(body, "video_b64", None):
        raise HTTPException(
            status_code=400,
            detail="Either image or video data is required."
        )

    try:
        images = await generate_image_variants(body.image_b64, body.product_context)
    except TimeoutError as e:
        raise HTTPException(status_code=503, detail=str(e))
    except ValueError as e:
        raise HTTPException(status_code=502, detail=str(e))
    except Exception:
        raise HTTPException(status_code=503, detail="Image generation failed. Please retry.")

    # Update project with image URLs
    if body.project_id:
        proj = await db.execute(
            select(BoostProject).where(BoostProject.id == uuid.UUID(body.project_id))
        )
        project = proj.scalar_one_or_none()
        if project:
            project.image_urls = images
            db.add(BoostGeneration(
                device_id=body.device_id,
                project_id=project.id,
                gen_type=GenType.IMAGE,
                date_utc=date_str,
            ))

    await db.commit()
    await increment_quota(body.device_id, date_str, "image")

    return GenerateImagesResponse(
        project_id=body.project_id,
        images=[ImageResult(label=i["label"], url=i["url"]) for i in images],
    )

# ── GENERATE VIDEOS ───────────────────────────────────────────────

@router.post("/generate-video", response_model=GenerateVideoResponse)
async def generate_video(
    body: GenerateVideoRequest,
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(BoostDevice).where(BoostDevice.device_id == body.device_id)
    )
    device = result.scalar_one_or_none()
    is_pro = device.is_pro if device else False
    date_str = datetime.now(timezone.utc).strftime("%Y-%m-%d")

    used, exceeded = await check_quota(body.device_id, date_str, "video", is_pro)
    if exceeded:
        limit = PRO_DAILY_CAP if is_pro else FREE_LIMITS["video"]
        raise HTTPException(
            status_code=429,
            detail=f"Daily video generation limit reached ({limit}/day). Upgrade to Pro for unlimited."
        )

    if not body.video_b64 and not body.image_b64:
        raise HTTPException(status_code=400, detail="Either video or image must be provided.")

    try:
        videos = await generate_video_variants(
            hook_text=body.hook_text,
            caption=body.caption,
            cta_text=body.cta_text,
            product_context="product",
            video_b64=body.video_b64,
            image_b64=body.image_b64,
        )
    except TimeoutError as e:
        raise HTTPException(status_code=503, detail=str(e))
    except ValueError as e:
        raise HTTPException(status_code=502, detail=str(e))
    except Exception:
        raise HTTPException(status_code=503, detail="Video generation failed. Please retry.")

    if body.project_id:
        proj = await db.execute(
            select(BoostProject).where(BoostProject.id == uuid.UUID(body.project_id))
        )
        project = proj.scalar_one_or_none()
        if project:
            project.video_urls = videos
            db.add(BoostGeneration(
                device_id=body.device_id,
                project_id=project.id,
                gen_type=GenType.VIDEO,
                date_utc=date_str,
            ))

    await db.commit()
    await increment_quota(body.device_id, date_str, "video")

    return GenerateVideoResponse(
        project_id=body.project_id,
        videos=[VideoResult(label=v["label"], url=v["url"]) for v in videos],
    )

# ── PROJECTS ──────────────────────────────────────────────────────

@router.get("/projects/{device_id}", response_model=list[ProjectSummary])
async def list_projects(device_id: str, db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(BoostProject)
        .where(BoostProject.device_id == device_id)
        .order_by(desc(BoostProject.created_at))
        .limit(50)
    )
    projects = result.scalars().all()
    return [
        ProjectSummary(
            id=str(p.id),
            product_name=p.product_name,
            niche=p.niche,
            platform=p.platform,
            image_count=len(p.image_urls) if p.image_urls else 0,
            video_count=len(p.video_urls) if p.video_urls else 0,
            caption_count=len(p.captions) if p.captions else 0,
            created_at=p.created_at,
        )
        for p in projects
    ]

@router.get("/projects/{device_id}/{project_id}", response_model=ProjectDetail)
async def get_project(device_id: str, project_id: str, db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(BoostProject).where(
            BoostProject.id == uuid.UUID(project_id),
            BoostProject.device_id == device_id,
        )
    )
    project = result.scalar_one_or_none()
    if not project:
        raise HTTPException(status_code=404, detail="Project not found.")
    return ProjectDetail(
        id=str(project.id),
        product_name=project.product_name,
        niche=project.niche,
        vibe=project.vibe,
        platform=project.platform,
        strategy=project.strategy,
        captions=project.captions,
        image_urls=project.image_urls,
        video_urls=project.video_urls,
        hashtags=project.hashtags,
        created_at=project.created_at,
    )

# ── TRENDING ──────────────────────────────────────────────────────

@router.get("/trending", response_model=list[TrendItem])
async def get_trending(
    device_id: Optional[str] = None,
    db: AsyncSession = Depends(get_db),
):
    is_pro = False
    if device_id:
        result = await db.execute(
            select(BoostDevice).where(BoostDevice.device_id == device_id)
        )
        device = result.scalar_one_or_none()
        is_pro = device.is_pro if device else False

    date_str = datetime.now(timezone.utc).strftime("%Y-%m-%d")
    result = await db.execute(
        select(TrendingProduct)
        .where(TrendingProduct.date_utc == date_str)
        .order_by(TrendingProduct.rank)
    )
    trends = result.scalars().all()

    # Free users: see all trends but is_pro_only flag lets mobile show/blur
    return [TrendItem(
        id=str(t.id), emoji=t.emoji, category=t.category,
        trend=t.trend, heat=t.heat, growth=t.growth,
        is_pro_only=t.is_pro_only, rank=t.rank,
    ) for t in trends]

# ── SUBSCRIPTION ──────────────────────────────────────────────────

@router.post("/verify-subscription", response_model=DeviceResponse)
async def verify_subscription(
    body: VerifySubscriptionRequest,
    db: AsyncSession = Depends(get_db),
):
    # Google Play subscription verification
    try:
        from googleapiclient.discovery import build
        from google.oauth2 import service_account
        from config import settings

        credentials = service_account.Credentials.from_service_account_file(
            settings.GOOGLE_SERVICE_ACCOUNT_JSON,
            scopes=["https://www.googleapis.com/auth/androidpublisher"],
        )
        service = build("androidpublisher", "v3", credentials=credentials)
        result = service.purchases().subscriptions().get(
            packageName=settings.GOOGLE_PLAY_PACKAGE,
            subscriptionId=body.product_id,
            token=body.purchase_token,
        ).execute()

        is_active = result.get("paymentState") == 1

        if not is_active:
            raise HTTPException(
                status_code=402,
                detail="Subscription payment not confirmed. Please complete purchase."
            )

        expiry_ms = int(result.get("expiryTimeMillis", 0))
        expiry = datetime.fromtimestamp(expiry_ms / 1000, tz=timezone.utc) if expiry_ms else None

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=503,
            detail="Could not verify subscription with Google Play. Try again."
        )

    # Update device
    db_result = await db.execute(
        select(BoostDevice).where(BoostDevice.device_id == body.device_id)
    )
    device = db_result.scalar_one_or_none()
    if not device:
        device = BoostDevice(device_id=body.device_id)
        db.add(device)

    device.is_pro = True
    device.subscription_token = body.purchase_token
    device.subscription_expiry = expiry
    await db.commit()
    await db.refresh(device)

    return DeviceResponse.model_validate(device)
