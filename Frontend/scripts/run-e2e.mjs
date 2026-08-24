import { spawn } from "node:child_process";
import { once } from "node:events";
import { resolve } from "node:path";

const projectRoot = resolve(import.meta.dirname, "..");
const nextCli = resolve(projectRoot, "node_modules", "next", "dist", "bin", "next");
const playwrightCli = resolve(projectRoot, "node_modules", "@playwright", "test", "cli.js");
const testEnv = { ...process.env, NEXT_PUBLIC_API_BASE_URL: "http://backend.test" };
let server;

try {
  await run(process.execPath, [nextCli, "build"], testEnv);
  server = spawn(process.execPath, [nextCli, "start", "--hostname", "127.0.0.1", "--port", "3100"], {
    cwd: projectRoot,
    env: testEnv,
    stdio: "inherit",
    detached: process.platform !== "win32",
  });
  await waitForServer("http://127.0.0.1:3100", server);
  await run(process.execPath, [playwrightCli, "test"], testEnv);
} catch (error) {
  process.exitCode ||= 1;
  console.error(error);
} finally {
  await stopServer(server);
}
process.exit(process.exitCode ?? 0);

async function run(command, args, env) {
  const child = spawn(command, args, { cwd: projectRoot, env, stdio: "inherit" });
  const [code] = await once(child, "exit");
  if (code !== 0) process.exitCode = typeof code === "number" ? code : 1;
  if (code !== 0) throw new Error(`${args.join(" ")} exited with code ${code}`);
}

async function waitForServer(url, child) {
  const deadline = Date.now() + 120_000;
  while (Date.now() < deadline) {
    if (child.exitCode !== null) throw new Error(`Next.js server exited with code ${child.exitCode}`);
    try {
      const response = await fetch(url);
      if (response.ok) return;
    } catch {
      // The server is still starting.
    }
    await new Promise((resolvePromise) => setTimeout(resolvePromise, 250));
  }
  throw new Error("Timed out waiting for the Next.js E2E server");
}

async function stopServer(child) {
  if (!child || child.exitCode !== null) return;
  if (process.platform === "win32") {
    const killer = spawn("taskkill", ["/pid", String(child.pid), "/T", "/F"], { stdio: "ignore" });
    await once(killer, "exit");
    if (child.exitCode === null) {
      await Promise.race([once(child, "exit"), new Promise((resolvePromise) => setTimeout(resolvePromise, 5000))]);
    }
  } else {
    process.kill(-child.pid, "SIGTERM");
    await Promise.race([once(child, "exit"), new Promise((resolvePromise) => setTimeout(resolvePromise, 5000))]);
    if (child.exitCode === null) process.kill(-child.pid, "SIGKILL");
  }
}
