# Failure Handling and Rollback Guide

## 1. Purpose

This document describes how to identify common application, deployment, database, and monitoring failures in the Todo Summary Assistant and how to recover safely.

The goal is to restore service while protecting persistent data and avoiding unnecessary infrastructure changes.

## 2. Initial Troubleshooting Checklist

When the application or monitoring dashboard is unavailable:

1. Confirm that the EC2 instance is running.
2. Confirm that the instance is connected to AWS Systems Manager.
3. Check the latest GitHub Actions workflow run.
4. Inspect Docker container status.
5. Check application and monitoring logs.
6. Verify backend health and Prometheus scrape targets.
7. Make one corrective change at a time and recheck service health.

Run on EC2:

```bash
sudo docker ps -a
```

Check the Compose services:

```bash
cd /home/ubuntu/TodoSummaryAssistant-Fresh
sudo docker compose ps
```

## 3. Failure: GitHub Actions Deployment Fails

### Symptoms

- A GitHub Actions workflow run fails.
- Deployment commands return errors.
- The EC2 application continues running an older version.

### Investigation

1. Open the repository's GitHub Actions page.
2. Select the failed workflow run and inspect the failing job.
3. Check AWS IAM role permissions and the OIDC trust configuration.
4. Check the SSM command status and its output.
5. Confirm that the EC2 instance is online and the repository directory is accessible.

### Recovery

- Fix the actual build, permission, configuration, or deployment error.
- Commit and push the corrected change.
- Re-run the workflow.
- Confirm that the workflow succeeds and the application health endpoint responds.

Do not delete the EC2 instance or database as a first response to a deployment failure.

## 4. Failure: Application Is Unavailable

### Symptoms

- The frontend does not load.
- API requests fail.
- The backend health endpoint does not return `UP`.

### Investigation

Check containers:

```bash
sudo docker ps -a
```

Inspect backend logs:

```bash
sudo docker logs --tail 100 todo-fresh-compose-backend
```

Check backend health:

```bash
curl -i http://127.0.0.1:8083/actuator/health
```

Inspect frontend logs:

```bash
sudo docker logs --tail 100 todo-fresh-compose-frontend
```

### Recovery

1. Identify the failing service and the error in its logs.
2. Correct the relevant application or environment configuration.
3. Use the normal GitHub Actions deployment workflow after fixing the issue.
4. Verify frontend access, backend health, and database connectivity.

Avoid restarting every service when only one service is affected.

## 5. Failure: MySQL Is Unavailable

### Symptoms

- Backend logs show database connection errors.
- The application cannot read or save todos.
- The database container is unhealthy or repeatedly restarting.

### Investigation

Check the database container:

```bash
sudo docker ps -a
```

Inspect MySQL logs:

```bash
sudo docker logs --tail 100 todo-fresh-compose-db
```

Check the database health status:

```bash
sudo docker inspect --format='{{.State.Health.Status}}' todo-fresh-compose-db
```

### Recovery

1. Check that the MySQL container is running and healthy.
2. Verify the database name, username, password configuration, and Docker network.
3. Check whether the disk is full.
4. Correct the configuration or resource issue.
5. Recheck database health and backend connectivity.

**Data protection:** The current deployment stores MySQL data in the `todo_fresh_mysql_data` Docker volume. Do not remove this volume during troubleshooting.

Never run the following command as a routine recovery step:

```bash
docker compose down -v
```

Removing volumes can permanently delete database data.

The previous Amazon RDS instance was deleted. If RDS is restored or recreated, validate the new endpoint, security-group rules, credentials, and application connection separately.

## 6. Failure: Prometheus Targets Are Down

### Symptoms

- Grafana panels display missing data.
- Prometheus reports a target as `DOWN`.
- Backend or EC2 metrics stop updating.

### Investigation

Inspect Prometheus targets:

```bash
curl -s http://127.0.0.1:9090/api/v1/targets
```

Inspect Prometheus logs:

```bash
sudo docker logs --tail 100 todo-fresh-prometheus
```

Check the backend and Node Exporter containers:

```bash
sudo docker ps -a
```

### Recovery

1. Identify the failing target in Prometheus.
2. Check whether the target container is running.
3. Verify the Compose service name and port.
4. Check the configured scrape path and Docker network.
5. Correct the configuration and redeploy.
6. Verify that the target returns to `UP`.

Expected scrape targets include:

- `backend:8080/actuator/prometheus`
- `node-exporter:9100/metrics`
- `prometheus:9090/metrics`

## 7. Failure: Grafana Is Unavailable

### Symptoms

- The browser cannot open Grafana.
- Grafana shows an error page.
- Dashboards display no data.

### Investigation

Check Grafana health:

```bash
curl http://127.0.0.1:3000/api/health
```

Inspect logs:

```bash
sudo docker logs --tail 100 todo-fresh-grafana
```

Check whether Prometheus is ready:

```bash
curl http://127.0.0.1:9090/-/ready
```

### Recovery

1. Confirm that the Grafana container is running.
2. Confirm that the local port-forwarding session is active.
3. Verify the Grafana datasource points to `http://prometheus:9090`.
4. Confirm that Prometheus is collecting data.
5. Check Grafana provisioning files and logs before changing dashboard configuration.

For browser access from Windows, start the Grafana SSM port-forwarding session:

```bash
aws ssm start-session --target i-0ce15cf6cbb280e75 --document-name AWS-StartPortForwardingSession --parameters "portNumber=3000,localPortNumber=3000" --region ap-south-1
```

Keep the terminal open and visit `http://localhost:3000`.

## 8. Failure: EC2 CPU, Memory, or Disk Pressure

### Symptoms

- The application responds slowly.
- Containers restart or become unhealthy.
- Grafana shows high CPU, memory, or disk utilization.

### Investigation

Check filesystem capacity:

```bash
df -h
```

Check memory:

```bash
free -h
```

Check running containers and resource usage:

```bash
sudo docker ps
sudo docker stats --no-stream
```

### Recovery

1. Identify the service consuming the resources.
2. Inspect its logs and workload.
3. Remove only confirmed unnecessary files or unused resources.
4. Preserve database and monitoring volumes.
5. Consider resizing EC2 if sustained resource pressure requires more capacity.
6. Confirm that application and monitoring health have recovered.

Do not delete database volumes or snapshots to free space without first verifying their contents and recovery requirements.

## 9. Rollback Procedure

Use rollback when a new application release introduces a confirmed regression and a known-good version is available.

### Step 1: Identify the last known-good version

Inspect the Git history:

```bash
git log --oneline -10
```

Identify the last working commit and the deployment associated with it.

### Step 2: Restore the intended version safely

The safest approach is to revert the problematic commit on the working branch:

```bash
git revert <bad-commit-sha>
git push origin HEAD:main
```

Replace `<bad-commit-sha>` with the actual commit hash.

Review the generated revert commit before pushing. The GitHub Actions workflow should deploy the reverted version if its configured trigger and jobs succeed.

Do not force-push or reset the shared `main` branch as a routine rollback method.

### Step 3: Verify recovery

On EC2:

```bash
sudo docker ps
curl -i http://127.0.0.1:8083/actuator/health
curl http://127.0.0.1:9090/-/ready
curl http://127.0.0.1:3000/api/health
```

Then verify:

- The latest GitHub Actions run succeeded.
- The frontend loads.
- Backend health returns `UP`.
- MySQL remains healthy and existing data is preserved.
- Prometheus targets return to `UP`.
- Grafana dashboards display current metrics.

### Important rollback considerations

- A Git revert does not automatically restore a database to an earlier state.
- Database schema changes may not be backward compatible.
- Do not delete or recreate persistent volumes during application rollback.
- Restore database backups or snapshots only after checking the recovery plan and data-loss implications.
- Keep the last known-good commit and backup references documented.

## 10. Post-Recovery Checklist

- [ ] Application is reachable.
- [ ] Backend health returns `UP`.
- [ ] MySQL container is healthy.
- [ ] Existing todo data is preserved.
- [ ] GitHub Actions deployment succeeded.
- [ ] Prometheus targets are healthy.
- [ ] Grafana dashboard displays metrics.
- [ ] Relevant alert states have recovered.
- [ ] The cause of failure and corrective action are documented.

## 11. Current Limitations

- The current deployment uses MySQL in a Docker container on EC2.
- Private Amazon RDS provisioning or restoration and application connectivity validation remain outstanding.
- Prometheus alert rules are configured, but external email or Slack notifications have not been verified.
- Rollback procedures must be validated against the actual release and database-change history before being relied upon in production.
