# Monitoring and Operations Guide

## 1. Overview

The Todo Summary Assistant uses Prometheus and Grafana to monitor application health and EC2 infrastructure metrics.

### Monitoring Components

| Component | Purpose |
|---|---|
| Prometheus | Collects and stores metrics and evaluates alert rules |
| Grafana | Visualizes metrics through dashboards |
| Node Exporter | Exposes EC2 host-level metrics |
| Spring Boot Actuator | Exposes application health and metrics |
| Micrometer Prometheus Registry | Makes Spring Boot metrics available to Prometheus |
| Docker | Runs the application and monitoring containers |

## 2. Monitoring Architecture

1. Spring Boot Actuator exposes application metrics.
2. Node Exporter exposes EC2 CPU, memory, disk, and other host metrics.
3. Prometheus scrapes these metrics at 15-second intervals.
4. Prometheus evaluates configured alert rules.
5. Grafana queries Prometheus and displays dashboards.
6. Administrators access Grafana and Prometheus through AWS Systems Manager port forwarding.

## 3. Accessing Grafana

### Prerequisites

- AWS CLI installed and configured.
- Permission to start an SSM session with the EC2 instance.
- EC2 instance running and connected to AWS Systems Manager.
- AWS Session Manager plugin installed on Windows.

### Start the Grafana port-forwarding session

Run the following command in Windows Command Prompt or PowerShell:

```bash
aws ssm start-session --target i-0ce15cf6cbb280e75 --document-name AWS-StartPortForwardingSession --parameters "portNumber=3000,localPortNumber=3000" --region ap-south-1
```

Keep the terminal open while using Grafana.

Open the following URL in your browser:

http://localhost:3000

Log in using username `admin` and the password configured in the EC2 `.env` file.

Do not commit the real password or other secrets to GitHub.

### Open the dashboard

Navigate to:

`Dashboards → Todo Application → Todo Application and EC2 Monitoring`

The dashboard includes:

- Backend availability
- Backend request rate
- JVM memory usage
- EC2 CPU usage
- EC2 memory usage
- EC2 root filesystem usage

## 4. Accessing Prometheus

Open a second terminal and run:

```bash
aws ssm start-session --target i-0ce15cf6cbb280e75 --document-name AWS-StartPortForwardingSession --parameters "portNumber=9090,localPortNumber=9090" --region ap-south-1
```

Keep the terminal open and visit:

http://localhost:9090

### Check scrape targets

Open:

`Status → Targets`

The configured targets are:

| Job | Target | Purpose |
|---|---|---|
| `spring-boot-backend` | `backend:8080/actuator/prometheus` | Application metrics |
| `ec2-node` | `node-exporter:9100/metrics` | EC2 host metrics |
| `prometheus` | `prometheus:9090/metrics` | Prometheus self-monitoring |

Targets were verified healthy during deployment validation. Check the live target status whenever troubleshooting.

## 5. Useful Prometheus Queries

Enter these expressions in the Prometheus expression browser.

| Query | Purpose |
|---|---|
| `up` | Checks whether targets are reachable |
| `up{job="spring-boot-backend"}` | Checks backend scrape availability |
| `up{job="ec2-node"}` | Checks Node Exporter availability |
| `sum(rate(http_server_requests_seconds_count[5m]))` | Estimates request rate when matching request metrics are available |
| `100 - (avg by (instance) (rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)` | Calculates EC2 CPU usage percentage |

A target value of `1` means Prometheus can scrape the target; `0` means the scrape is failing.

## 6. Configured Alert Rules

Prometheus evaluates alert rules every 15 seconds.

| Alert | Condition | Duration | Severity |
|---|---|---|---|
| `BackendDown` | Backend scrape target is down | 1 minute | Critical |
| `EC2NodeExporterDown` | Node Exporter scrape target is down | 1 minute | Warning |
| `HighEC2CPUUsage` | EC2 CPU usage exceeds 85% | 5 minutes | Warning |

### Inspect alerts

Open the Prometheus interface and navigate to **Alerts**.

You can also query the alerts API from EC2:

```bash
curl -s http://127.0.0.1:9090/api/v1/alerts
```

Alert rules are configured, but external email or Slack notifications have not been verified. Notification delivery requires a separately configured and tested notification receiver.

## 7. Health Checks on EC2

Run these commands through an authorized SSM session or another approved administrative method.

### Check running containers

```bash
sudo docker ps
```

### Check Docker Compose service status

```bash
cd /home/ubuntu/TodoSummaryAssistant-Fresh
sudo docker compose ps
```

### Check backend health

```bash
curl http://127.0.0.1:8083/actuator/health
```

Expected response:

```json
{"status":"UP"}
```

### Check Prometheus readiness

```bash
curl http://127.0.0.1:9090/-/ready
```

### Check Grafana health

```bash
curl http://127.0.0.1:3000/api/health
```

### Check Prometheus targets

```bash
curl -s http://127.0.0.1:9090/api/v1/targets
```

## 8. Container Logs

### Backend

```bash
sudo docker logs --tail 100 todo-fresh-compose-backend
```

### MySQL

```bash
sudo docker logs --tail 100 todo-fresh-compose-db
```

### Prometheus

```bash
sudo docker logs --tail 100 todo-fresh-prometheus
```

### Grafana

```bash
sudo docker logs --tail 100 todo-fresh-grafana
```

### Node Exporter

```bash
sudo docker logs --tail 100 todo-fresh-node-exporter
```

To follow a container's logs continuously, use:

```bash
sudo docker logs -f todo-fresh-compose-backend
```

Press `Ctrl+C` to stop following logs; this does not stop the container.

## 9. Operational Precautions

- Keep `.env` out of version control.
- Do not expose database passwords, API keys, or Grafana credentials in logs or documentation.
- Keep Prometheus and Grafana bound to the EC2 loopback interface.
- Use SSM port forwarding for remote monitoring access.
- Inspect container status and logs before restarting services.
- Check application health and Prometheus targets after deployment.
- Monitor EC2 disk space and memory capacity.
- Preserve the Docker database and monitoring volumes during routine operations.
- Do not use `docker compose down -v` for routine shutdown or deployment.

## 10. Current Deployment Limitations

- The currently running application uses MySQL in a Docker container on EC2.
- The previous Amazon RDS instance was deleted; provisioning or restoring a private RDS MySQL instance and validating application connectivity remain outstanding.
- Prometheus and Grafana run on the same EC2 instance as the application.
- Alert rules are configured, but external email or Slack notification delivery has not been verified.
# Monitoring and Operations Guide

## 1. Overview

The Todo Summary Assistant uses Prometheus and Grafana to monitor application health and EC2 infrastructure metrics.

### Monitoring Components

| Component | Purpose |
|---|---|
| Prometheus | Collects and stores metrics and evaluates alert rules |
| Grafana | Visualizes metrics through dashboards |
| Node Exporter | Exposes EC2 host-level metrics |
| Spring Boot Actuator | Exposes application health and metrics |
| Micrometer Prometheus Registry | Makes Spring Boot metrics available to Prometheus |
| Docker | Runs the application and monitoring containers |

## 2. Monitoring Architecture

1. Spring Boot Actuator exposes application metrics.
2. Node Exporter exposes EC2 CPU, memory, disk, and other host metrics.
3. Prometheus scrapes these metrics at 15-second intervals.
4. Prometheus evaluates configured alert rules.
5. Grafana queries Prometheus and displays dashboards.
6. Administrators access Grafana and Prometheus through AWS Systems Manager port forwarding.

## 3. Accessing Grafana

### Prerequisites

- AWS CLI installed and configured.
- Permission to start an SSM session with the EC2 instance.
- EC2 instance running and connected to AWS Systems Manager.
- AWS Session Manager plugin installed on Windows.

### Start the Grafana port-forwarding session

Run the following command in Windows Command Prompt or PowerShell:

```bash
aws ssm start-session --target i-0ce15cf6cbb280e75 --document-name AWS-StartPortForwardingSession --parameters "portNumber=3000,localPortNumber=3000" --region ap-south-1
```

Keep the terminal open while using Grafana.

Open the following URL in your browser:

http://localhost:3000

Log in using username `admin` and the password configured in the EC2 `.env` file.

Do not commit the real password or other secrets to GitHub.

### Open the dashboard

Navigate to:

`Dashboards → Todo Application → Todo Application and EC2 Monitoring`

The dashboard includes:

- Backend availability
- Backend request rate
- JVM memory usage
- EC2 CPU usage
- EC2 memory usage
- EC2 root filesystem usage

## 4. Accessing Prometheus

Open a second terminal and run:

```bash
aws ssm start-session --target i-0ce15cf6cbb280e75 --document-name AWS-StartPortForwardingSession --parameters "portNumber=9090,localPortNumber=9090" --region ap-south-1
```

Keep the terminal open and visit:

http://localhost:9090

### Check scrape targets

Open:

`Status → Targets`

The configured targets are:

| Job | Target | Purpose |
|---|---|---|
| `spring-boot-backend` | `backend:8080/actuator/prometheus` | Application metrics |
| `ec2-node` | `node-exporter:9100/metrics` | EC2 host metrics |
| `prometheus` | `prometheus:9090/metrics` | Prometheus self-monitoring |

Targets were verified healthy during deployment validation. Check the live target status whenever troubleshooting.

## 5. Useful Prometheus Queries

Enter these expressions in the Prometheus expression browser.

| Query | Purpose |
|---|---|
| `up` | Checks whether targets are reachable |
| `up{job="spring-boot-backend"}` | Checks backend scrape availability |
| `up{job="ec2-node"}` | Checks Node Exporter availability |
| `sum(rate(http_server_requests_seconds_count[5m]))` | Estimates request rate when matching request metrics are available |
| `100 - (avg by (instance) (rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)` | Calculates EC2 CPU usage percentage |

A target value of `1` means Prometheus can scrape the target; `0` means the scrape is failing.

## 6. Configured Alert Rules

Prometheus evaluates alert rules every 15 seconds.

| Alert | Condition | Duration | Severity |
|---|---|---|---|
| `BackendDown` | Backend scrape target is down | 1 minute | Critical |
| `EC2NodeExporterDown` | Node Exporter scrape target is down | 1 minute | Warning |
| `HighEC2CPUUsage` | EC2 CPU usage exceeds 85% | 5 minutes | Warning |

### Inspect alerts

Open the Prometheus interface and navigate to **Alerts**.

You can also query the alerts API from EC2:

```bash
curl -s http://127.0.0.1:9090/api/v1/alerts
```

Alert rules are configured, but external email or Slack notifications have not been verified. Notification delivery requires a separately configured and tested notification receiver.

## 7. Health Checks on EC2

Run these commands through an authorized SSM session or another approved administrative method.

### Check running containers

```bash
sudo docker ps
```

### Check Docker Compose service status

```bash
cd /home/ubuntu/TodoSummaryAssistant-Fresh
sudo docker compose ps
```

### Check backend health

```bash
curl http://127.0.0.1:8083/actuator/health
```

Expected response:

```json
{"status":"UP"}
```

### Check Prometheus readiness

```bash
curl http://127.0.0.1:9090/-/ready
```

### Check Grafana health

```bash
curl http://127.0.0.1:3000/api/health
```

### Check Prometheus targets

```bash
curl -s http://127.0.0.1:9090/api/v1/targets
```

## 8. Container Logs

### Backend

```bash
sudo docker logs --tail 100 todo-fresh-compose-backend
```

### MySQL

```bash
sudo docker logs --tail 100 todo-fresh-compose-db
```

### Prometheus

```bash
sudo docker logs --tail 100 todo-fresh-prometheus
```

### Grafana

```bash
sudo docker logs --tail 100 todo-fresh-grafana
```

### Node Exporter

```bash
sudo docker logs --tail 100 todo-fresh-node-exporter
```

To follow a container's logs continuously, use:

```bash
sudo docker logs -f todo-fresh-compose-backend
```

Press `Ctrl+C` to stop following logs; this does not stop the container.

## 9. Operational Precautions

- Keep `.env` out of version control.
- Do not expose database passwords, API keys, or Grafana credentials in logs or documentation.
- Keep Prometheus and Grafana bound to the EC2 loopback interface.
- Use SSM port forwarding for remote monitoring access.
- Inspect container status and logs before restarting services.
- Check application health and Prometheus targets after deployment.
- Monitor EC2 disk space and memory capacity.
- Preserve the Docker database and monitoring volumes during routine operations.
- Do not use `docker compose down -v` for routine shutdown or deployment.

## 10. Current Deployment Limitations

- The currently running application uses MySQL in a Docker container on EC2.
- The previous Amazon RDS instance was deleted; provisioning or restoring a private RDS MySQL instance and validating application connectivity remain outstanding.
- Prometheus and Grafana run on the same EC2 instance as the application.
- Alert rules are configured, but external email or Slack notification delivery has not been verified.
