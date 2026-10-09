from sqlalchemy import (
    Column, String, Boolean, DateTime,
    Integer, Text, Enum, func
)
from sqlalchemy.dialects.postgresql import UUID, JSONB
import uuid
import enum
from database import Base


class GenType(str, enum.Enum):
    ANALYSIS = "analysis"
    IMAGE    = "image"
    VIDEO    = "video"


class BoostDevice(Base):
    __tablename__ = "boost_devices"

    device_id           = Column(String(255), primary_key=True)
    is_pro              = Column(Boolean, default=False)
    subscription_token  = Column(Text, nullable=True)
    subscription_expiry = Column(DateTime(timezone=True), nullable=True)
    created_at          = Column(DateTime(timezone=True), server_default=func.now())
    updated_at          = Column(DateTime(timezone=True), onupdate=func.now())


class BoostProject(Base):
    __tablename__ = "boost_projects"

    id           = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    device_id    = Column(String(255), nullable=False, index=True)
    product_name = Column(String(255), nullable=True)
    niche        = Column(String(100), nullable=True)
    vibe         = Column(String(100), nullable=True)
    platform     = Column(String(50), nullable=True)
    # AI outputs stored as JSON
    strategy     = Column(JSONB, nullable=True)
    captions     = Column(JSONB, nullable=True)   # list of {hook, caption}
    image_urls   = Column(JSONB, nullable=True)   # list of {label, url}
    video_urls   = Column(JSONB, nullable=True)   # list of {label, url}
    hashtags     = Column(JSONB, nullable=True)   # {viral:[], niche:[], brand:[]}
    created_at   = Column(DateTime(timezone=True), server_default=func.now(), index=True)


class BoostGeneration(Base):
    __tablename__ = "boost_generations"

    id         = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    device_id  = Column(String(255), nullable=False, index=True)
    project_id = Column(UUID(as_uuid=True), nullable=True)
    gen_type   = Column(Enum(GenType), nullable=False)
    date_utc   = Column(String(10), nullable=False)   # YYYY-MM-DD
    created_at = Column(DateTime(timezone=True), server_default=func.now())


class TrendingProduct(Base):
    __tablename__ = "boost_trending"

    id         = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    emoji      = Column(String(10))
    category   = Column(String(100), nullable=False)
    trend      = Column(String(255), nullable=False)
    heat       = Column(Integer, default=0)          # 0-100
    growth     = Column(String(20))                  # e.g. "+340%"
    is_pro_only = Column(Boolean, default=False)     # first 2 = free
    rank       = Column(Integer, default=0)          # sort order
    date_utc   = Column(String(10), nullable=False)  # which day
    created_at = Column(DateTime(timezone=True), server_default=func.now())
