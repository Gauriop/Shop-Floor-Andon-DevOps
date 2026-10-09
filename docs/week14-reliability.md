# Week 14: Automated Provisioning and Reliability Validation

Tool: Ansible. Target: Ubuntu node (WSL2). Application: Andon Dashboard jar run as the `andon` systemd service on port 8085.

## Results

| #   | Test               | Command / playbook        | Expected                                   | Result | Evidence                      |
| --- | ------------------ | ------------------------- | ------------------------------------------ | ------ | ----------------------------- |
| 1   | Clean node         | teardown.yml              | no service, no /opt/andon, no andon user   |        | week14-01-teardown.log        |
| 2   | Provision          | playbook.yml              | many changed, failed=0, HTTP 200           |        | week14-02-provision.log       |
| 3   | Idempotency        | playbook.yml (second run) | changed=0, failed=0                        |        | week14-03-idempotency.log     |
| 4   | Good release       | deploy.yml 1.0.1          | health check passes                        |        | week14-04-deploy-good.log     |
| 5   | Bad release        | deploy.yml 1.0.2-bad      | failure, automatic rollback, same jar hash |        | week14-05-auto-rollback.log   |
| 6   | Manual rollback    | rollback.yml              | back on previous version, HTTP 200         |        | week14-06-manual-rollback.log |
| 7   | Crash recovery     | SIGKILL the service       | systemd restarts it, HTTP 200              |        | week14-07-recovery.log        |
| 8   | Final health check | ansible uri module        | HTTP 200                                   |        | week14-08-health.log          |

## How rollback works

Before each deploy the running jar is copied to `previous.jar`. The new jar is installed, the service is restarted, and `/events` must return 200. If the port does not open or the health check fails, the rescue section restores `previous.jar`, restarts, and checks again, so a bad release never stays live.

## Limitations

Single local node; no firewall or TLS management; the jar is copied from a local build rather than pulled from a registry.
