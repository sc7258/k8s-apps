# Development Environment Setup

This document describes how to set up the local development environment for the Inventory API, including database connectivity and testing tools.

## 1. Kubernetes Connectivity (Telepresence)

We use **Telepresence** to bridge the local development environment with the Kubernetes cluster. This allows the local Spring Boot application to access cluster-internal services using their Kubernetes DNS names.

### Connection Steps
1. Ensure your `kubectl` context is set to the correct cluster.
2. Connect Telepresence:
   ```bash
   telepresence connect
   ```
3. Run the application with the `k8s` profile:
   ```bash
   SPRING_PROFILES_ACTIVE=k8s ./gradlew bootRun
   ```

## 2. Local Container Engine (Docker)

To support integration testing with **Testcontainers**, a local Docker Engine is required.

### Installation (Ubuntu/Debian)

If Docker is not installed, follow these official steps:

1. **Set up Docker's apt repository**:
   ```bash
   sudo apt-get update
   sudo apt-get install ca-certificates curl gnupg
   sudo install -m 0755 -d /etc/apt/keyrings
   curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
   sudo chmod a+r /etc/apt/keyrings/docker.gpg

   echo \
     "deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
     "$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | \
     sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
   ```

2. **Install Docker Engine**:
   ```bash
   sudo apt-get update
   sudo apt-get install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
   ```

3. **Start Service and Set Permissions**:
   ```bash
   sudo systemctl start docker
   sudo systemctl enable docker
   sudo chmod 666 /var/run/docker.sock
   ```

## 3. Integration Testing (Testcontainers)

We use **Testcontainers** to run integration tests against a real MariaDB instance.
- **Why?**: It ensures 100% compatibility with the production database, avoiding issues caused by H2's SQL dialect differences.
- **How it works**: JUnit automatically starts a MariaDB container before running tests and stops it afterward.
- **Run Tests**:
  ```bash
  ./gradlew test
  ```
