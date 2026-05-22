import os
import shutil
import subprocess
import re
import time

MOD_VERSION = "1.0.1"

targets = [
    {
        "mc": "26.1",
        "fabric": "0.144.1+26.1",
        "loader": "0.16.14",
    },
    {
        "mc": "26.1.1",
        "fabric": "0.145.2+26.1.1",
        "loader": "0.16.14",
    },
    {
        "mc": "26.1.2",
        "fabric": "0.149.0+26.1.2",
        "loader": "0.16.14",
    },
]

os.makedirs("BUILDS", exist_ok=True)

with open("gradle.properties", "r", encoding="utf-8") as f:
    gp_orig = f.read()

with open("src/main/resources/fabric.mod.json", "r", encoding="utf-8") as f:
    fmj_orig = f.read()

# Force the correct mod version in template
gp_orig = re.sub(r"mod_version=.*", f"mod_version={MOD_VERSION}", gp_orig)

ENV = os.environ.copy()
ENV["JAVA_HOME"] = r"d:\git\minecraft-mods\LitematicaEasyPlaceVariants\jdk-25"


def safe_delete_build_dir():
    build_dir = "build"
    if not os.path.exists(build_dir):
        return
    for i in range(10):
        try:
            shutil.rmtree(build_dir)
            print("  Successfully deleted build directory.")
            return
        except Exception as e:
            print(f"  Failed to delete build directory (attempt {i+1}): {e}")
            subprocess.run([r".\gradlew.bat", "--stop"], shell=True, env=ENV,
                           stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            time.sleep(2.0)
    print("  WARNING: Could not delete build directory after 10 attempts!")


for target in targets:
    mc_ver   = target["mc"]
    fab_ver  = target["fabric"]
    ldr_ver  = target["loader"]

    print(f"Building for Minecraft {mc_ver}...")

    safe_delete_build_dir()

    gp = gp_orig
    gp = re.sub(r"minecraft_version=.*", f"minecraft_version={mc_ver}",  gp)
    gp = re.sub(r"fabric_version=.*",    f"fabric_version={fab_ver}",    gp)
    gp = re.sub(r"loader_version=.*",    f"loader_version={ldr_ver}",    gp)
    with open("gradle.properties", "w", encoding="utf-8") as f:
        f.write(gp)

    fmj = re.sub(r'"minecraft":\s*".*?"', f'"minecraft": "~{mc_ver}"', fmj_orig)
    with open("src/main/resources/fabric.mod.json", "w", encoding="utf-8") as f:
        f.write(fmj)

    result = subprocess.run([r".\gradlew.bat", "build"], shell=True, env=ENV)
    if result.returncode != 0:
        print(f"Build failed for MC {mc_ver}!")
        exit(1)

    src_jar  = f"build/libs/right-click-plants-{MOD_VERSION}.jar"
    dest_jar = f"BUILDS/right-click-plants-{MOD_VERSION}-{mc_ver}.jar"
    shutil.copy(src_jar, dest_jar)
    print(f"  -> {dest_jar}")

    subprocess.run([r".\gradlew.bat", "--stop"], shell=True, env=ENV,
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    time.sleep(2.0)

# Final clean
safe_delete_build_dir()

# Restore gradle.properties and fabric.mod.json to default (26.1.2)
gp_final = gp_orig
gp_final = re.sub(r"minecraft_version=.*", "minecraft_version=26.1.2",      gp_final)
gp_final = re.sub(r"fabric_version=.*",    "fabric_version=0.149.0+26.1.2", gp_final)
gp_final = re.sub(r"loader_version=.*",    "loader_version=0.16.14",         gp_final)
with open("gradle.properties", "w", encoding="utf-8") as f:
    f.write(gp_final)

fmj_final = re.sub(r'"minecraft":\s*".*?"', '"minecraft": ">=26.1"', fmj_orig)
with open("src/main/resources/fabric.mod.json", "w", encoding="utf-8") as f:
    f.write(fmj_final)

print(f"\nAll builds completed! Version: {MOD_VERSION}")
