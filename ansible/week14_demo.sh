#!/usr/bin/env bash
# Week 14 guided demo. Run from Ubuntu in the project root:  bash ansible/week14_demo.sh
set -u
cd "$(dirname "$0")/.." || exit 1

INV="ansible/inventory.ini"
LOGDIR="docs"
JAR="$PWD/target/andon-dashboard-0.1.0.jar"
PORT=8085
URL="http://127.0.0.1:${PORT}/events"

if [ ! -f "$JAR" ]; then
  echo "Jar not found: $JAR"
  echo "Build it in Windows PowerShell first:  mvn -B package -DskipTests"
  exit 1
fi
mkdir -p "$LOGDIR"

# Ask for the sudo password once; hand it to Ansible through a private temp file.
read -r -s -p "Ubuntu sudo password (asked once): " PW
echo
VARS="$(mktemp)"
chmod 600 "$VARS"
python3 -c 'import json,sys; print(json.dumps({"ansible_become_password": sys.argv[1]}))' "$PW" > "$VARS"
trap 'rm -f "$VARS"' EXIT

pause()  { echo; read -r -p ">>> Take your screenshot, then press Enter to continue... " _; echo; }
banner() { echo; echo "=========================================================="; echo "$1"; echo "=========================================================="; }

# Poll the app until it answers 200 (or give up after N tries, 3s apart)
wait_health() {
  local tries="${1:-20}" code="000" i
  for i in $(seq 1 "$tries"); do
    code=$(curl -s -o /dev/null -w "%{http_code}" "$URL" || true)
    if [ "$code" = "200" ]; then echo "HTTP 200"; return 0; fi
    sleep 3
  done
  echo "HTTP ${code} (not healthy after $((tries * 3))s)"
  return 1
}

# pb <playbook> <log file> [extra args]
pb() {
  local play="$1" log="$2"
  shift 2
  ansible-playbook -i "$INV" "ansible/$play" -e "@$VARS" "$@" 2>&1 | tee "$LOGDIR/$log"
}

banner "STEP 1 - TEARDOWN: return the node to a clean state"
pb teardown.yml week14-01-teardown.log
{
  echo "--- verifying the node is clean ---"
  systemctl status andon --no-pager 2>&1 | head -3
  ls /opt/andon 2>&1
  id andon 2>&1
  ss -ltn | grep ":${PORT} " || echo "port ${PORT}: nothing is listening"
} 2>&1 | tee -a "$LOGDIR/week14-01-teardown.log"
pause

banner "STEP 2 - PROVISION the clean node from scratch (expect many 'changed')"
pb playbook.yml week14-02-provision.log -e "andon_jar_src=$JAR"
{
  echo "--- health check ---"; wait_health 20
  echo "--- jar fingerprint of the stable release ---"
  sha256sum /opt/andon/app/andon-dashboard.jar
} 2>&1 | tee -a "$LOGDIR/week14-02-provision.log"
pause

banner "STEP 3 - IDEMPOTENCY: same playbook again (expect changed=0)"
pb playbook.yml week14-03-idempotency.log -e "andon_jar_src=$JAR"
pause

banner "STEP 4 - GOOD RELEASE 1.0.1 (health check must pass)"
pb deploy.yml week14-04-deploy-good.log -e release_version=1.0.1 -e "release_jar=$JAR"
{
  echo "--- release records ---"
  echo -n "current:  "; cat /opt/andon/releases/current.version
  echo -n "previous: "; cat /opt/andon/releases/previous.version
  ls -l /opt/andon/releases
} 2>&1 | tee -a "$LOGDIR/week14-04-deploy-good.log"
pause

banner "STEP 5 - BAD RELEASE 1.0.2-bad (corrupt jar) -> AUTOMATIC ROLLBACK (about 1.5 minutes)"
echo "this is not a valid jar file" > /tmp/andon-bad.jar
pb deploy.yml week14-05-auto-rollback.log -e release_version=1.0.2-bad -e release_jar=/tmp/andon-bad.jar
{
  echo "--- after the automatic rollback ---"; wait_health 20
  echo -n "current version: "; cat /opt/andon/releases/current.version
  echo "jar fingerprint (must equal the one from step 2):"
  sha256sum /opt/andon/app/andon-dashboard.jar
} 2>&1 | tee -a "$LOGDIR/week14-05-auto-rollback.log"
echo "(The red FAILED line above is expected. Look for ROLLBACK COMPLETE and rescued=1.)"
pause

banner "STEP 6 - MANUAL ROLLBACK: deploy 1.0.3, then go back to the previous stable release"
pb deploy.yml week14-06a-deploy-1.0.3.log -e release_version=1.0.3 -e "release_jar=$JAR"
pb rollback.yml week14-06-manual-rollback.log
{
  echo -n "current version after rollback: "; cat /opt/andon/releases/current.version
  wait_health 20
} 2>&1 | tee -a "$LOGDIR/week14-06-manual-rollback.log"
pause

banner "STEP 7 - CRASH RECOVERY: kill the service, systemd restarts it by itself"
{
  echo "--- before ---"; systemctl show andon -p MainPID,NRestarts,ActiveState
  echo "--- killing the process with SIGKILL ---"
  printf '%s\n' "$PW" | sudo -S -p '' systemctl kill -s SIGKILL andon
  echo "--- waiting for systemd to restart it ---"; sleep 8
  wait_health 30
  echo "--- after ---"; systemctl show andon -p MainPID,NRestarts,ActiveState
} 2>&1 | tee "$LOGDIR/week14-07-recovery.log"
pause

banner "STEP 8 - FINAL HEALTH CHECK through Ansible"
ansible -i "$INV" andon_nodes -m uri -a "url=$URL status_code=200" 2>&1 | tee "$LOGDIR/week14-08-health.log"

echo
echo "Done. Logs saved in $LOGDIR/ :"
ls -1 "$LOGDIR"/week14-*.log