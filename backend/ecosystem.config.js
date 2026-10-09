module.exports = {
  apps: [{
    name: "boost-backend",
    script: "/root/workspace/boost-app/backend/venv/bin/python3.11",
    args: "main.py",
    cwd: "/root/workspace/boost-app/backend",
    interpreter: "none",
    env: {
      PYTHONPATH: "/root/workspace/boost-app/backend",
    },
    error_file: "/root/workspace/boost-app/backend/logs/error.log",
    out_file:   "/root/workspace/boost-app/backend/logs/out.log",
    restart_delay: 5000,
    max_restarts: 10,
  }]
};
