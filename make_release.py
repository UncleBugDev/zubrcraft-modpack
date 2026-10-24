#!/usr/bin/env python3
import os, sys, glob, json, urllib.request, zipfile, mimetypes

MC_DIR = "/home/unclebug/storage/minecraft"
ENV_FILE = os.path.join(MC_DIR, ".github_env")
REPO = "UncleBugDev/zubrcraft-modpack"

SERVER_ONLY_MODS = ["chunky", "servercore", "spark"]
SERVER_ONLY_CONFIGS = ["chunky", "servercore", "spark"]

def load_env():
    env = {}
    if os.path.exists(ENV_FILE):
        with open(ENV_FILE, "r") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    k, v = line.split("=", 1)
                    env[k.strip()] = v.strip()
    return env

def create_server_mods_zip(out_zip):
    print(f"[*] Packaging {out_zip}...")
    mods_dir = os.path.join(MC_DIR, "mods")
    config_dir = os.path.join(MC_DIR, "config")
    
    with zipfile.ZipFile(out_zip, "w", zipfile.ZIP_DEFLATED) as zf:
        # Add mods
        jar_count = 0
        for jar in sorted(glob.glob(os.path.join(mods_dir, "*.jar"))):
            base = os.path.basename(jar).lower()
            if any(s in base for s in SERVER_ONLY_MODS):
                print(f"  [SKIP server-only] {os.path.basename(jar)}")
                continue
            zf.write(jar, os.path.join("mods", os.path.basename(jar)))
            jar_count += 1
            print(f"  [+] mods/{os.path.basename(jar)}")
            
        # Add configs
        for root, dirs, files in os.walk(config_dir):
            for file in files:
                full = os.path.join(root, file)
                rel = os.path.relpath(full, MC_DIR)
                if any(s in rel.lower() for s in SERVER_ONLY_CONFIGS):
                    continue
                zf.write(full, rel)
                
    print(f"[OK] {out_zip} created! ({jar_count} mods, size: {round(os.path.getsize(out_zip)/1024/1024, 2)} MB)")

def github_api(endpoint, data=None, method=None, token=None, content_type="application/json"):
    url = f"https://api.github.com{endpoint}" if endpoint.startswith("/") else endpoint
    headers = {
        "Authorization": f"token {token}",
        "User-Agent": "ZubrCraft-Release-Builder",
        "Accept": "application/vnd.github.v3+json"
    }
    if content_type:
        headers["Content-Type"] = content_type
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    with urllib.request.urlopen(req) as resp:
        body = resp.read()
        if not body or resp.status == 204:
            return {}
        return json.loads(body.decode())

def upload_asset(upload_url_template, file_path, token):
    name = os.path.basename(file_path)
    url = upload_url_template.split("{")[0] + f"?name={name}"
    print(f"[*] Uploading {name} to GitHub Releases...")
    with open(file_path, "rb") as f:
        file_bytes = f.read()
    headers = {
        "Authorization": f"token {token}",
        "User-Agent": "ZubrCraft-Release-Builder",
        "Content-Type": "application/zip",
        "Content-Length": str(len(file_bytes))
    }
    req = urllib.request.Request(url, data=file_bytes, headers=headers, method="POST")
    with urllib.request.urlopen(req) as resp:
        res = json.loads(resp.read().decode())
        print(f"[OK] Uploaded {name}: {res.get('browser_download_url')}")

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 make_release.py <tag_name> [release_title] [release_notes]")
        sys.exit(1)
        
    tag = sys.argv[1]
    title = sys.argv[2] if len(sys.argv) > 2 else f"ZubrCraft Release {tag}"
    notes = sys.argv[3] if len(sys.argv) > 3 else "Официальное обновление сборки сервера ZubrCraft."
    
    env = load_env()
    token = env.get("GITHUB_TOKEN")
    if not token:
        print("[!] GITHUB_TOKEN not found in .github_env")
        sys.exit(1)
        
    out_zip = os.path.join(MC_DIR, "2_ZubrCraft_Server_Mods.zip")
    create_server_mods_zip(out_zip)
    
    # Check if release exists
    release = None
    try:
        release = github_api(f"/repos/{REPO}/releases/tags/{tag}", token=token)
        print(f"[*] Release {tag} already exists (ID: {release['id']}).")
    except urllib.error.HTTPError as e:
        if e.code == 404:
            print(f"[*] Creating new release {tag}...")
            payload = json.dumps({
                "tag_name": tag,
                "name": title,
                "body": notes,
                "draft": False,
                "prerelease": False
            }).encode()
            release = github_api(f"/repos/{REPO}/releases", data=payload, method="POST", token=token)
        else:
            raise
            
    upload_url = release["upload_url"]
    
    # Delete existing asset if re-uploading
    for asset in release.get("assets", []):
        if asset["name"] == os.path.basename(out_zip):
            print(f"[*] Deleting old asset {asset['name']} (ID: {asset['id']})...")
            github_api(f"/repos/{REPO}/releases/assets/{asset['id']}", method="DELETE", token=token)
            
    upload_asset(upload_url, out_zip, token)
    
    # Upload base optimization if present
    base_opt = os.path.join(MC_DIR, "1_ZubrCraft_Optimization_Base.zip")
    if os.path.exists(base_opt):
        for asset in release.get("assets", []):
            if asset["name"] == os.path.basename(base_opt):
                print(f"[*] Deleting old asset {asset['name']} (ID: {asset['id']})...")
                github_api(f"/repos/{REPO}/releases/assets/{asset['id']}", method="DELETE", token=token)
        upload_asset(upload_url, base_opt, token)

    # Upload Prism Launcher instance zip if present
    prism_opt = os.path.join(MC_DIR, "ZubrCraft-Prism.zip")
    if os.path.exists(prism_opt):
        for asset in release.get("assets", []):
            if asset["name"] == os.path.basename(prism_opt):
                print(f"[*] Deleting old asset {asset['name']} (ID: {asset['id']})...")
                github_api(f"/repos/{REPO}/releases/assets/{asset['id']}", method="DELETE", token=token)
        upload_asset(upload_url, prism_opt, token)

    print("\n[SUCCESS] Release successfully published to GitHub!")
    print(f"URL: {release.get('html_url')}")

if __name__ == "__main__":
    main()
