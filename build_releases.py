import os
import shutil
import subprocess
import re

targets = [
    {"mc": "1.21.10", "fabric": "0.138.4+1.21.10", "loader": "0.16.14", "loom": "1.8.12"},
    {"mc": "1.21.11", "fabric": "0.141.4+1.21.11", "loader": "0.16.14", "loom": "1.9.2"},
    {"mc": "26.1",    "fabric": "0.144.1+26.1",    "loader": "0.18.4",  "loom": "1.15-SNAPSHOT"},
    {"mc": "26.1.1",  "fabric": "0.145.2+26.1.1",  "loader": "0.18.4",  "loom": "1.15-SNAPSHOT"},
    {"mc": "26.1.2",  "fabric": "0.149.0+26.1.2",  "loader": "0.18.4",  "loom": "1.15-SNAPSHOT"},
]

os.makedirs("BUILDS", exist_ok=True)

with open("gradle.properties", "r", encoding="utf-8") as f:
    gp_orig = f.read()

with open("src/main/resources/fabric.mod.json", "r", encoding="utf-8") as f:
    fmj_orig = f.read()

with open("build.gradle", "r", encoding="utf-8") as f:
    bg_orig = f.read()

gp_orig = re.sub(r"mod_version=.*", "mod_version=1.0.0", gp_orig)

for target in targets:
    mc_ver  = target["mc"]
    fab_ver = target["fabric"]
    loader  = target["loader"]
    loom    = target["loom"]
    print(f"\nBuilding for Minecraft {mc_ver} (loom {loom})...")

    # gradle.properties
    gp = re.sub(r"minecraft_version=.*", f"minecraft_version={mc_ver}", gp_orig)
    gp = re.sub(r"fabric_version=.*",    f"fabric_version={fab_ver}",   gp)
    gp = re.sub(r"loader_version=.*",    f"loader_version={loader}",    gp)
    with open("gradle.properties", "w", encoding="utf-8") as f:
        f.write(gp)

    # fabric.mod.json
    fmj = re.sub(r'"minecraft":\s*".*?"', f'"minecraft": "~{mc_ver}"', fmj_orig)
    with open("src/main/resources/fabric.mod.json", "w", encoding="utf-8") as f:
        f.write(fmj)

    # build.gradle — patch loom version
    bg = re.sub(
        r"id 'net\.fabricmc\.fabric-loom' version '.*?'",
        f"id 'net.fabricmc.fabric-loom' version '{loom}'",
        bg_orig
    )
    with open("build.gradle", "w", encoding="utf-8") as f:
        f.write(bg)

    env = os.environ.copy()
    env["JAVA_HOME"] = r"d:\git\minecraft-mods\LitematicaEasyPlaceVariants\jdk-25"
    result = subprocess.run([r".\gradlew.bat", "clean", "build"], shell=True, env=env)
    if result.returncode != 0:
        print(f"Build failed for MC {mc_ver}!")
        exit(1)

    src_jar  = "build/libs/right-click-plants-1.0.0.jar"
    dest_jar = f"BUILDS/right-click-plants-1.0.0-{mc_ver}.jar"
    shutil.copy(src_jar, dest_jar)
    print(f"  -> {dest_jar}")

# Restore to 26.1.2 / loom 1.15
gp_final = re.sub(r"minecraft_version=.*", "minecraft_version=26.1.2",       gp_orig)
gp_final = re.sub(r"fabric_version=.*",    "fabric_version=0.149.0+26.1.2",  gp_final)
gp_final = re.sub(r"loader_version=.*",    "loader_version=0.18.4",           gp_final)
with open("gradle.properties", "w", encoding="utf-8") as f:
    f.write(gp_final)

fmj_final = re.sub(r'"minecraft":\s*".*?"', '"minecraft": ">=1.21.10"', fmj_orig)
with open("src/main/resources/fabric.mod.json", "w", encoding="utf-8") as f:
    f.write(fmj_final)

with open("build.gradle", "w", encoding="utf-8") as f:
    f.write(bg_orig)

print("\nAll builds completed!")
print("  python create_github_repo.py")
print("  python ../publish_tools/upload_github.py")
print("  python ../publish_tools/upload_modrinth.py")
