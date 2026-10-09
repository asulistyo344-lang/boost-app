from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    DATABASE_URL: str
    REDIS_URL: str = "redis://localhost:6379/2"
    JWT_SECRET: str
    JWT_ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 525600

    BYTEPLUS_API_KEY: str
    BYTEPLUS_BASE_URL: str = "https://visual.volcengineapi.com"
    BYTEPLUS_REGION: str = "ap-southeast-1"
    BYTEPLUS_VISION_MODEL: str = "glm-4v"
    BYTEPLUS_REASONING_MODEL: str = "deepseek-v3"
    BYTEPLUS_IMAGE_MODEL: str = "high_aes_general_v20_L"
    BYTEPLUS_VIDEO_MODEL: str = "bytedance-seedream-3-0-video"

    APP_NAME: str = "Boost"
    ENVIRONMENT: str = "development"
    PORT: int = 8004
    INSTALL_DIR: str = "/root/workspace/boost-app"

    GOOGLE_PLAY_PACKAGE: str = "com.boost.app"
    GOOGLE_SERVICE_ACCOUNT_JSON: str = "./service-account.json"

    class Config:
        env_file = ".env"

settings = Settings()
