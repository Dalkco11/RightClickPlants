"""
One-shot: create GitHub repo for RightClickPlants + push code.
Run from d:\git\minecraft-mods\RightClickPlants\
"""
import os, json, urllib.request, urllib.error, subprocess, sys

script_dir = os.path.dirname(os.path.realpath(__file__))
token_path = os.path.join(script_dir, "..", "publish_tools", "tokens", "github_token.txt")
with open(token_path, "r") as f:
    token = f.read().strip()

headers = {
    "Authorization": f"Bearer {token}",
    "Accept": "application/vnd.github+json",
    "X-GitHub-Api-Version": "2022-11-28",
    "User-Agent": "Dalkco/MinecraftModPublisher/1.0.0",
    "Content-Type": "application/json",
}

# 1. Create repo
print("Creating GitHub repository Dalkco11/RightClickPlants...")
body = json.dumps({
    "name": "RightClickPlants",
    "description": "Right-click mature crops to harvest and auto-replant — Fabric client-side mod",
    "private": False,
    "auto_init": False,
}).encode()

req = urllib.request.Request("https://api.github.com/user/repos", data=body, headers=headers, method="POST")
try:
    with urllib.request.urlopen(req) as r:
        data = json.loads(r.read())
        print(f"  Repo created: {data['html_url']}")
        clone_url = data["clone_url"]
except urllib.error.HTTPError as e:
    err = json.loads(e.read())
    if "already exists" in str(err):
        print("  Repo already exists, continuing...")
        clone_url = "https://github.com/Dalkco11/RightClickPlants.git"
    else:
        print(f"  Error: {err}")
        sys.exit(1)

# 2. Add remote and push
cwd = os.path.dirname(os.path.realpath(__file__))
remote_url = f"https://{token}@github.com/Dalkco11/RightClickPlants.git"

def run(cmd):
    r = subprocess.run(cmd, shell=True, cwd=cwd, capture_output=True, text=True)
    print(f"  $ {cmd}")
    if r.stdout.strip(): print(f"    {r.stdout.strip()}")
    if r.stderr.strip(): print(f"    {r.stderr.strip()}")
    return r.returncode

print("\nPushing code to GitHub...")
run(f'git remote add origin "{remote_url}" 2>nul || git remote set-url origin "{remote_url}"')
run("git branch -M main")
run("git push -u origin main")
print("\nDone! Repo is live at https://github.com/Dalkco11/RightClickPlants")
