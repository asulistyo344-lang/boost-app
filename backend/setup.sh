#!/bin/bash
# Boost Backend Setup Script
# Applies all lessons from Nebula + Aura dev reports
# Run on VPS: bash setup.sh

set -e

INSTALL_DIR="${BOOST_INSTALL_DIR:-/root/workspace/boost-app/backend}"
APP_USER="boost"
APP_PASS="boost_password"
APP_DB="boost_db"
PORT=8004

echo "🚀 Setting up Boost backend at $INSTALL_DIR..."

# ── Python 3.11 (explicit — never assume python3 is correct) ──────
if ! python3.11 --version &>/dev/null; then
    echo "Installing Python 3.11..."
    apt-get update -qq
    add-apt-repository -y ppa:deadsnakes/ppa
    apt-get install -y python3.11 python3.11-venv python3.11-dev
fi
echo "✅ Python: $(python3.11 --version)"

# ── PostgreSQL ────────────────────────────────────────────────────
echo "Setting up PostgreSQL..."
if ! pg_isready -q 2>/dev/null; then
    apt-get install -y postgresql postgresql-contrib
    service postgresql start
fi

# Create user and DB (idempotent)
sudo -u postgres psql -tc "SELECT 1 FROM pg_roles WHERE rolname='$APP_USER'" | grep -q 1 || \
    sudo -u postgres psql -c "CREATE USER $APP_USER WITH PASSWORD '$APP_PASS';"

sudo -u postgres psql -tc "SELECT 1 FROM pg_database WHERE datname='$APP_DB'" | grep -q 1 || \
    sudo -u postgres psql -c "CREATE DATABASE $APP_DB OWNER $APP_USER;"

# Ensure password matches (Aura lesson BE-02 — always force correct password)
sudo -u postgres psql -c "ALTER USER $APP_USER WITH PASSWORD '$APP_PASS';"
echo "✅ PostgreSQL: $APP_DB ready"

# ── Redis ─────────────────────────────────────────────────────────
if ! redis-cli ping &>/dev/null; then
    apt-get install -y redis-server
    service redis-server start
fi
echo "✅ Redis: $(redis-cli ping)"

# ── Venv + dependencies ───────────────────────────────────────────
cd "$INSTALL_DIR"
if [ ! -d venv ]; then
    python3.11 -m venv venv
fi
# Always use python3.11 -m pip (Nebula lesson BE-01)
venv/bin/python3.11 -m pip install --upgrade pip -q
venv/bin/python3.11 -m pip install -r /root/workspace/boost-app/backend/requirements.txt -q
echo "✅ Dependencies installed"

# ── .env file ─────────────────────────────────────────────────────
if [ ! -f .env ]; then
    cp .env.example .env
    # Auto-fill DATABASE_URL with correct password (Aura lesson BE-02)
    sed -i "s|postgresql+asyncpg://boost:boost_password|postgresql+asyncpg://$APP_USER:$APP_PASS|g" .env
    echo ""
    echo "⚠️  Created .env — edit these values:"
    echo "    BYTEPLUS_API_KEY"
    echo "    JWT_SECRET (change to random string)"
    echo ""
fi

# ── PM2 ecosystem config ──────────────────────────────────────────
# Aura lesson BE-04 + BE-09: use INSTALL_DIR, never hardcode /opt/
cat > ecosystem.config.js << EOF
module.exports = {
  apps: [{
    name: "boost-backend",
    script: "$INSTALL_DIR/venv/bin/python3.11",
    args: "main.py",
    cwd: "$INSTALL_DIR",
    interpreter: "none",
    env: {
      PYTHONPATH: "$INSTALL_DIR",
    },
    error_file: "$INSTALL_DIR/logs/error.log",
    out_file:   "$INSTALL_DIR/logs/out.log",
    restart_delay: 5000,
    max_restarts: 10,
  }]
};
EOF

mkdir -p logs
echo "✅ PM2 config written"

# ── Icon generation for Android (Aura lesson AB-10) ───────────────
if command -v python3.11 &>/dev/null && python3.11 -c "import PIL" 2>/dev/null; then
    echo "Generating placeholder Android icons..."
    python3.11 << 'PYEOF'
from PIL import Image, ImageDraw
import os

sizes = {
    "mipmap-hdpi":    (72,72),
    "mipmap-mdpi":    (48,48),
    "mipmap-xhdpi":   (96,96),
    "mipmap-xxhdpi":  (144,144),
    "mipmap-xxxhdpi": (192,192),
}
res_dir = "../mobile/app/src/main/res"
for folder, size in sizes.items():
    path = os.path.join(res_dir, folder)
    os.makedirs(path, exist_ok=True)
    img = Image.new("RGBA", size, (255, 107, 53, 255))
    d = ImageDraw.Draw(img)
    d.ellipse([size[0]//4, size[1]//4, size[0]*3//4, size[1]*3//4], fill=(255,255,255,200))
    img.save(os.path.join(path, "ic_launcher.png"))
    img.save(os.path.join(path, "ic_launcher_round.png"))
print("Icons generated")
PYEOF
fi

echo ""
echo "✅ Boost backend setup complete!"
echo ""
echo "To start:"
echo "  source venv/bin/activate"
echo "  python3.11 main.py              # development"
echo "  pm2 start ecosystem.config.js  # production"
echo ""
echo "Swagger UI: http://$(hostname -I | cut -d' ' -f1):$PORT/docs"
