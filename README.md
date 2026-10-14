# Todo Summary Assistant — DevOps Project

A full-stack Todo Summary Assistant deployed on AWS EC2 using Docker and Docker Compose, with GitHub Actions for CI/CD and Prometheus and Grafana for monitoring.

## Project Overview

This project demonstrates containerization, automated deployment, cloud hosting, application health checks, infrastructure monitoring, and alert-rule configuration.

### Key Features

- Create and manage todo tasks through a React frontend.
- Spring Boot REST API for backend operations.
- MySQL database for persistent application data.
- Docker and Docker Compose for running application services.
- GitHub Actions workflow for build, validation, and deployment.
- AWS EC2 hosting with deployment commands executed through AWS Systems Manager (SSM).
- Prometheus for collecting application and EC2 metrics.
- Grafana dashboard for visualizing application and infrastructure health.
- Alert rules for backend availability, host-metrics availability, and high CPU usage.

## Technology Stack

| Area | Technologies |
|---|---|
| Frontend | React, JavaScript, Nginx |
| Backend | Java 21, Spring Boot |
| Database | MySQL 8.0 |
| Containers | Docker, Docker Compose |
| CI/CD | GitHub Actions |
| Cloud | AWS EC2, AWS Systems Manager, IAM |
| Monitoring | Prometheus, Grafana, Node Exporter |
| Application metrics | Spring Boot Actuator, Micrometer Prometheus registry |
| Version control | Git, GitHub |

## Architecture

1. Developers push code to GitHub.
2. GitHub Actions runs the configured build and deployment workflow.
3. AWS IAM OIDC authentication allows GitHub Actions to assume the deployment role.
4. GitHub Actions sends deployment commands to the EC2 instance through AWS Systems Manager.
5. Docker Compose runs the frontend, backend, MySQL, Prometheus, Node Exporter, and Grafana.
6. Spring Boot Actuator exposes application health and Prometheus metrics.
7. Prometheus collects backend, host, and self-monitoring metrics and evaluates alert rules.
8. Grafana reads metrics from Prometheus and displays the monitoring dashboard.

## Project Structure

```text
TodoSummaryAssistant-Fresh/
├── Backend/
│   └── todo-summary-assistant/
│       ├── pom.xml
│       └── src/
├── Frontend/
│   ├── package.json
│   └── src/
├── monitoring/
│   ├── prometheus.yml
│   ├── alert-rules.yml
│   └── grafana/
│       ├── dashboards/
│       │   └── todo-monitoring.json
│       └── provisioning/
│           ├── dashboards/
│           │   └── dashboard.yml
│           └── datasources/
│               └── prometheus-datasource.yml
├── .github/
│   └── workflows/
│       └── ci.yml
├── docker-compose.yml
├── .env.example
├── .gitignore
├── README.md
├── FAILURE_AND_ROLLBACK.md
└── MONITORING_AND_OPERATIONS.md
```

## Run the Application Locally

### Prerequisites

Install Git, Docker Desktop with Docker Compose, Java 21, Node.js/npm, and the AWS CLI if you plan to use AWS deployment commands.

### 1. Clone the repository

```bash
git clone https://github.com/shashank030718/TodoSummaryAssistant-Fresh.git
cd TodoSummaryAssistant-Fresh
```

### 2. Configure environment variables

Create a local `.env` file based on `.env.example`, then fill in the required values locally.

Do not commit `.env`, database passwords, API keys, or webhook URLs to GitHub.

### 3. Start the containers

```bash
docker compose config
docker compose up -d --build
docker compose ps
```

View the application in your browser at:

- Frontend: `http://localhost`
- Backend health endpoint, if the backend port mapping is available locally: `http://localhost:8083/actuator/health`

### 4. Inspect logs

```bash
docker compose logs --tail=100
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f db
```

Use `Ctrl+C` to stop following logs. This does not stop the containers.

### 5. Stop the application safely

```bash
docker compose down
```

**Important:** Do not use `docker compose down -v` for routine shutdown. The `-v` option removes Compose-managed volumes and can delete persistent database and monitoring data.

## AWS Deployment

### Current EC2 deployment

| Setting | Value |
|---|---|
| EC2 instance name | `todo-summary-fresh-ec2` |
| Instance ID | `i-0ce15cf6cbb280e75` |
| AWS Region | `ap-south-1` |
| Application URL | `http://52.66.124.37` |
| Deployment method | GitHub Actions + AWS Systems Manager |

The instance must be running and correctly configured for SSM. The AWS CLI must be configured with an authorized identity.

### Deploy through GitHub Actions

The repository's `.github/workflows/ci.yml` workflow contains the build and deployment jobs.

To trigger a deployment after making a change:

```bash
git add .
git commit -m "Update project documentation"
git push origin HEAD:main
```

Then open the repository on GitHub and select **Actions**. Check that the latest workflow run completes successfully.

The workflow uses the configured AWS IAM role and SSM to deploy to the EC2 instance. It does not require SSH-based deployment when the configured SSM workflow is working.

### Check the application on EC2

Run commands through an authorized SSM session or the configured SSM deployment mechanism:

```bash
sudo docker ps
sudo docker compose -f /home/ubuntu/TodoSummaryAssistant-Fresh/docker-compose.yml ps
curl http://127.0.0.1:8083/actuator/health
```

Expected backend health response:

```json
{"status":"UP"}
```

## Monitoring with Prometheus and Grafana

The monitoring stack contains:

| Component | Purpose |
|---|---|
| Prometheus | Collects and stores metrics, evaluates alert rules |
| Grafana | Visualizes metrics in dashboards |
| Node Exporter | Exposes EC2 host metrics |
| Spring Boot Actuator | Exposes application health and metrics |
| Micrometer Prometheus registry | Publishes Spring Boot metrics in Prometheus format |

### Monitoring endpoints on EC2

These services are bound to the EC2 loopback interface rather than being publicly exposed:

| Service | EC2-local URL |
|---|---|
| Prometheus | `http://127.0.0.1:9090` |
| Grafana | `http://127.0.0.1:3000` |
| Backend health | `http://127.0.0.1:8083/actuator/health` |
| Backend metrics | `http://127.0.0.1:8083/actuator/prometheus` |

### Prerequisites for remote monitoring access

- AWS CLI installed and configured.
- Permission to start an SSM session with the EC2 instance.
- SSM Agent running and the EC2 instance configured for Systems Manager.
- AWS Session Manager plugin installed on Windows.

### Open Grafana from Windows

**1. Start the port-forwarding session** in Command Prompt or PowerShell:

```bash
aws ssm start-session --target i-0ce15cf6cbb280e75 --document-name AWS-StartPortForwardingSession --parameters "portNumber=3000,localPortNumber=3000" --region ap-south-1
```

**2. Keep the terminal open** while you use Grafana.

**3. Open this URL in your browser:**

[http://localhost:3000](http://localhost:3000)

Sign in using username `admin` and the Grafana password configured in the EC2 `.env` file. Do not store the real password in this repository.

**4. Open the dashboard:**

`Dashboards → Todo Application → Todo Application and EC2 Monitoring`

The dashboard includes:

- Backend availability
- Backend request rate
- JVM memory usage
- EC2 CPU usage
- EC2 memory usage
- EC2 root filesystem usage

### Open Prometheus from Windows

Open a second terminal and run:

```bash
aws ssm start-session --target i-0ce15cf6cbb280e75 --document-name AWS-StartPortForwardingSession --parameters "portNumber=9090,localPortNumber=9090" --region ap-south-1
```

Keep that terminal open, then visit:

[http://localhost:9090](http://localhost:9090)

### Useful Prometheus checks

In the Prometheus expression browser, try these queries:

| Query | What it checks |
|---|---|
| `up` | Whether monitored targets are reachable |
| `up{job="spring-boot-backend"}` | Backend scrape status |
| `up{job="ec2-node"}` | Node Exporter scrape status |
| `sum(rate(http_server_requests_seconds_count[5m]))` | Backend request rate, when matching request metrics are present |
| `100 - (avg by (instance) (rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)` | EC2 CPU usage percentage |

To inspect configured alerts, open **Alerts** in the Prometheus interface.

### Monitoring commands on EC2

Check running containers:

```bash
sudo docker ps
```

Check application health:

```bash
curl http://127.0.0.1:8083/actuator/health
```

Check Prometheus readiness:

```bash
curl http://127.0.0.1:9090/-/ready
```

Check Grafana health:

```bash
curl http://127.0.0.1:3000/api/health
```

Check Prometheus scrape targets:

```bash
curl -s http://127.0.0.1:9090/api/v1/targets
```

Check active alerts:

```bash
curl -s http://127.0.0.1:9090/api/v1/alerts
```

Check monitoring logs:

```bash
sudo docker logs --tail 100 todo-fresh-prometheus
sudo docker logs --tail 100 todo-fresh-grafana
sudo docker logs --tail 100 todo-fresh-node-exporter
```

Check backend logs:

```bash
sudo docker logs --tail 100 todo-fresh-compose-backend
```

### Configured alert rules

The Prometheus configuration includes these rules:

| Alert | Condition | Severity |
|---|---|---|
| `BackendDown` | Backend target unavailable for 1 minute | Critical |
| `EC2NodeExporterDown` | Host metrics target unavailable for 1 minute | Warning |
| `HighEC2CPUUsage` | EC2 CPU usage above 85% for 5 minutes | Warning |

Prometheus evaluates rules at 15-second intervals. Alert rules define conditions and states; external email or Slack notification delivery requires additional notification configuration and must be tested separately.

## Security and Operations

- Keep secrets in `.env` or an appropriate secrets-management system, never in source code.
- Keep `.env` out of Git using `.gitignore`.
- Restrict database access to authorized application infrastructure.
- Keep Prometheus and Grafana bound to localhost on EC2 and access them through SSM port forwarding.
- Restrict EC2 security-group inbound rules to the required ports and trusted sources.
- Preserve Docker volumes during routine shutdown and deployment.
- Review container health, logs, Prometheus targets, disk space, and memory during troubleshooting.
- Verify the GitHub Actions run and application health after deployment.

## Current Deployment Status and Limitations

The application, Docker-based MySQL, Prometheus, Node Exporter, and Grafana were running and passed health checks during deployment validation.

**Amazon RDS:** The previous RDS instance was deleted. The currently running deployment uses MySQL in a Docker container on EC2. Provisioning or restoring a private Amazon RDS MySQL instance and validating application connectivity remains necessary to satisfy the assignment's RDS requirement.

**Alert notifications:** Prometheus alert rules are configured. External email/Slack notification delivery has not been verified.

## Troubleshooting

### Grafana does not open

1. Check that EC2 is running and SSM access works.
2. Start the Grafana SSM port-forwarding command.
3. Keep the terminal open.
4. Check the Grafana container and logs on EC2.

### Prometheus targets are DOWN

1. Open Prometheus and select **Status → Targets**.
2. Check whether the backend and Node Exporter containers are running.
3. Inspect Prometheus and backend logs.
4. Confirm the relevant Compose service names and monitoring configuration.

### Deployment fails

1. Open GitHub Actions and inspect the failed job logs.
2. Check AWS role permissions and SSM command status.
3. Inspect EC2 container status and logs.
4. Verify the app's health endpoint before considering a rollback.

## Documentation

- `MONITORING_AND_OPERATIONS.md` — monitoring access, health checks, logs, and operations.
- `FAILURE_AND_ROLLBACK.md` — failure handling and rollback procedures.
- `aws/aws-setup.md` — AWS setup and deployment notes.
- `monitoring/prometheus.yml` — Prometheus scrape configuration.
- `monitoring/alert-rules.yml` — Prometheus alert rules.
- `monitoring/grafana/` — Grafana datasource, dashboard provisioning, and dashboard JSON.

## Learning Outcomes

This project provided hands-on experience with:

- Containerizing a multi-service application.
- Managing services and persistent volumes with Docker Compose.
- Automating build and deployment tasks using GitHub Actions.
- Using AWS IAM OIDC and Systems Manager for deployment access.
- Exposing Spring Boot health and application metrics.
- Collecting and querying metrics using Prometheus.
- Building Grafana dashboards and defining alert rules.
- Troubleshooting container health, application deployment, and monitoring services.
