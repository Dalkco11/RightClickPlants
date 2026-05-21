import os
import shutil
import subprocess
import re
import time

targets = [
    {"mc": "26.1",   "fabric": "0.144.1+26.1"},
    {"mc": "26.1.1", "fabric": "0.145.2+26.1.1"},
    {"mc": "26.1.2", "fabric": "0.149.0+26.1.2"},
]

os.makedirs("BUILDS", exist_ok=True)

with open("gradle.properties", "r", encoding="utf-8") as f:
    gp_orig = f.read()

with open("src/main/resources/fabric.mod.json", "r", encoding="utf-8") as f:
    fmj_orig = f.read()

gp_orig = re.sub(r"mod_version=.*", "mod_version=1.0.0", gp_orig)

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
            env = os.environ.copy()
            env["JAVA_HOME"] = r"d:\git\minecraft-mods\LitematicaEasyPlaceVariants\jdk-25"
            subprocess.run([r".\gradlew.bat", "--stop"], shell=True, env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            time.sleep(2.0)
    print("  WARNING: Could not delete build directory after 10 attempts!")

for target in targets:
    mc_ver = target["mc"]
    fab_ver = target["fabric"]
    print(f"Building for Minecraft {mc_ver}...")

    # Clean build directory before starting
    safe_delete_build_dir()

    gp = re.sub(r"minecraft_version=.*", f"minecraft_version={mc_ver}", gp_orig)
    gp = re.sub(r"fabric_version=.*", f"fabric_version={fab_ver}", gp)
    with open("gradle.properties", "w", encoding="utf-8") as f:
        f.write(gp)

    fmj = re.sub(r'"minecraft":\s*".*?"', f'"minecraft": "~{mc_ver}"', fmj_orig)
    with open("src/main/resources/fabric.mod.json", "w", encoding="utf-8") as f:
        f.write(fmj)

    env = os.environ.copy()
    env["JAVA_HOME"] = r"d:\git\minecraft-mods\LitematicaEasyPlaceVariants\jdk-25"
    result = subprocess.run([r".\gradlew.bat", "build"], shell=True, env=env)
    if result.returncode != 0:
        print(f"Build failed for MC {mc_ver}!")
        exit(1)

    src_jar  = "build/libs/right-click-plants-1.0.0.jar"
    dest_jar = f"BUILDS/right-click-plants-1.0.0-{mc_ver}.jar"
    shutil.copy(src_jar, dest_jar)
    print(f"  -> {dest_jar}")

    # Stop daemon to release locks immediately after build
    subprocess.run([r".\gradlew.bat", "--stop"], shell=True, env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    time.sleep(2.0)

# Final clean
safe_delete_build_dir()

# Restore
gp_final = re.sub(r"minecraft_version=.*", "minecraft_version=26.1.2",      gp_orig)
gp_final = re.sub(r"fabric_version=.*",    "fabric_version=0.149.0+26.1.2", gp_final)
with open("gradle.properties", "w", encoding="utf-8") as f:
    f.write(gp_final)

fmj_final = re.sub(r'"minecraft":\s*".*?"', '"minecraft": ">=26.1"', fmj_orig)
with open("src/main/resources/fabric.mod.json", "w", encoding="utf-8") as f:
    f.write(fmj_final)

print("\nAll builds completed!")
