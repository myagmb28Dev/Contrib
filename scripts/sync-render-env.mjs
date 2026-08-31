import fs from "node:fs";
import path from "node:path";

function parseEnv(content) {
  const result = {};
  const lines = content.split(/\r?\n/);
  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) continue;
    const match = trimmed.match(/^([^=]+)=(.*)$/);
    if (match) {
      const key = match[1].trim();
      let value = match[2].trim();
      if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
        value = value.slice(1, -1);
      }
      result[key] = value;
    }
  }
  return result;
}

const rootDir = process.cwd();
const envPath = path.resolve(rootDir, ".env");

let envConfig = {};
if (fs.existsSync(envPath)) {
  envConfig = parseEnv(fs.readFileSync(envPath, "utf-8"));
}

const apiKey = process.env.RENDER_API_KEY || envConfig.RENDER_API_KEY;
const targetServiceId = process.env.RENDER_SERVICE_ID;
const rawJsonEnv = process.env.RENDER_ENV_VARS_JSON;

if (!apiKey) {
  console.log("[Render Sync] RENDER_API_KEY not configured. Skipping environment variable sync.");
  process.exit(0);
}

const authHeader = {
  Accept: "application/json",
  "Content-Type": "application/json",
  Authorization: `Bearer ${apiKey}`,
};

const LOCAL_ONLY_KEYS = new Set([
  "RENDER_API_KEY",
  "POSTGRES_DB",
  "POSTGRES_PORT",
]);

async function getServices() {
  const res = await fetch("https://api.render.com/v1/services?limit=50", {
    headers: authHeader,
  });
  if (!res.ok) {
    throw new Error(`Failed to fetch Render services: ${res.status} ${await res.text()}`);
  }
  return (await res.json()).map((item) => item.service);
}

async function getExistingEnvVars(serviceId) {
  const res = await fetch(`https://api.render.com/v1/services/${serviceId}/env-vars`, {
    headers: authHeader,
  });
  if (!res.ok) return {};
  const list = await res.json();
  const map = {};
  for (const item of list) {
    map[item.envVar.key] = item.envVar.value;
  }
  return map;
}

async function updateServiceEnvVars(serviceId, serviceName, newVars) {
  const existing = await getExistingEnvVars(serviceId);
  const merged = { ...existing, ...newVars };

  const payload = Object.entries(merged).map(([key, value]) => ({
    key,
    value: String(value),
  }));

  const res = await fetch(`https://api.render.com/v1/services/${serviceId}/env-vars`, {
    method: "PUT",
    headers: authHeader,
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    throw new Error(`Failed to update env vars for ${serviceName || serviceId} (${serviceId}): ${res.status} ${await res.text()}`);
  }

  console.log(`[Render Sync] Successfully merged and updated ${payload.length} environment variables for ${serviceName || serviceId}. Redeployment triggered.`);
}

async function run() {
  try {
    if (targetServiceId && rawJsonEnv) {
      let parsedVars = {};
      try {
        parsedVars = JSON.parse(rawJsonEnv);
      } catch (err) {
        console.error(`[Render Sync] Invalid JSON in RENDER_ENV_VARS_JSON: ${err.message}`);
        process.exit(1);
      }
      await updateServiceEnvVars(targetServiceId, "Target Service", parsedVars);
      return;
    }

    const services = await getServices();
    const backend = services.find((s) => s.name === "contrib-backend");
    const frontend = services.find((s) => s.name === "contrib-frontend");

    const backendVars = {};
    const frontendVars = {};

    for (const [key, value] of Object.entries(envConfig)) {
      if (LOCAL_ONLY_KEYS.has(key)) continue;

      if (key === "DATABASE_URL" && (value.includes("localhost") || value.includes("127.0.0.1"))) {
        continue;
      }

      if (key.startsWith("NEXT_PUBLIC_")) {
        frontendVars[key] = value;
      } else {
        backendVars[key] = value;
      }
    }

    if (backend && Object.keys(backendVars).length > 0) {
      await updateServiceEnvVars(backend.id, backend.name, backendVars);
    }

    if (frontend && Object.keys(frontendVars).length > 0) {
      await updateServiceEnvVars(frontend.id, frontend.name, frontendVars);
    }

    console.log("[Render Sync] All environment variables safely synchronized with Render.");
  } catch (error) {
    console.error("[Render Sync] Error:", error.message);
  }
}

run();
