#!/bin/bash

# Complete Fresh Installation Script for Docker, SonarQube, and Jenkins
set -e

# Function to log messages
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# Function to wait for service to be ready
wait_for_service() {
    local url=$1
    local service_name=$2
    local max_attempts=${3:-30}
    local sleep_time=${4:-10}
    
    log "Waiting for $service_name to be ready..."
    for i in $(seq 1 $max_attempts); do
        if curl -f "$url" >/dev/null 2>&1; then
            log "$service_name is ready!"
            return 0
        fi
        log "$service_name starting... ($i/$max_attempts)"
        sleep $sleep_time
    done
    log "ERROR: $service_name failed to start after $max_attempts attempts"
    return 1
}

log "Starting complete fresh installation of Docker, SonarQube, and Jenkins..."

# Set non-interactive mode
export DEBIAN_FRONTEND=noninteractive

# Update system
log "Updating system packages..."
sudo apt-get update -qq
sudo apt-get upgrade -y -qq

# Install essential packages
log "Installing essential packages..."
sudo apt-get install -y ca-certificates curl gnupg lsb-release software-properties-common apt-transport-https wget jq net-tools

###########################################
# DOCKER INSTALLATION
###########################################
log "Installing Docker and Docker Compose..."

# Remove old Docker packages
for pkg in docker.io docker-doc docker-compose docker-compose-v2 podman-docker containerd runc; do 
    sudo apt-get remove -y $pkg 2>/dev/null || true
done

# Add Docker's official GPG key
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

# Add Docker repository
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update -qq

# Install Docker
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Configure Docker
sudo groupadd -f docker
sudo usermod -aG docker $USER
sudo systemctl enable docker
sudo systemctl start docker

# Verify Docker
if systemctl is-active --quiet docker; then
    log "Docker installed successfully: $(docker --version)"
else
    log "ERROR: Docker installation failed"
    exit 1
fi

###########################################
# JAVA INSTALLATION
###########################################
log "Installing OpenJDK 21..."
sudo apt-get install -y openjdk-21-jdk

# Verify Java
if java -version >/dev/null 2>&1; then
    log "Java installed successfully: $(java -version 2>&1 | head -n 1)"
else
    log "ERROR: Java installation failed"
    exit 1
fi

###########################################
# SONARQUBE INSTALLATION
###########################################
log "Installing SonarQube..."

# Start SonarQube container
sudo docker run -d --name sonarqube-server -p 9000:9000 sonarqube:lts-community

# Wait for SonarQube to be ready
log "Waiting for SonarQube to start..."
sleep 30

# Wait for health check
for i in {1..20}; do
    if curl -s -u admin:admin "http://localhost:9000/api/system/health" | grep -q '"health":"GREEN"'; then
        log "SonarQube is healthy!"
        break
    fi
    log "Waiting for SonarQube health... ($i/20)"
    sleep 15
done

# Generate SonarQube token
log "Generating SonarQube token..."
SONAR_TOKEN=""
for attempt in {1..5}; do
    TOKEN_RESPONSE=$(curl -s -u admin:admin -X POST "http://localhost:9000/api/user_tokens/generate" \
        -d "name=jenkins-token-$(date +%s)" 2>/dev/null || echo "")
    
    if [ -n "$TOKEN_RESPONSE" ]; then
        SONAR_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.token' 2>/dev/null || echo "")
        if [ -n "$SONAR_TOKEN" ] && [[ "$SONAR_TOKEN" =~ ^squ_.+ ]]; then
            log "SonarQube token generated: ${SONAR_TOKEN:0:10}..."
            break
        fi
    fi
    log "Token generation attempt $attempt failed, retrying..."
    sleep 5
done

if [ -z "$SONAR_TOKEN" ]; then
    log "Warning: Could not generate SonarQube token. Will need manual configuration."
fi

###########################################
# JENKINS INSTALLATION
###########################################
log "Installing Jenkins..."

# Add Jenkins repository
sudo wget -O /etc/apt/keyrings/jenkins-keyring.asc \
  https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key

echo "deb [signed-by=/etc/apt/keyrings/jenkins-keyring.asc]" \
  https://pkg.jenkins.io/debian-stable binary/ | sudo tee \
  /etc/apt/sources.list.d/jenkins.list > /dev/null

sudo apt-get update -qq
sudo apt-get install -y jenkins

log "Jenkins package installed"

# Configure Jenkins before starting
log "Configuring Jenkins..."

# Stop Jenkins if it's running
sudo systemctl stop jenkins || true

# Clean Jenkins directory
sudo rm -rf /var/lib/jenkins/*

# Create Jenkins directories
sudo mkdir -p /var/lib/jenkins/init.groovy.d/

# Set Jenkins to skip setup wizard
sudo mkdir -p /var/lib/jenkins
echo "2.440.3" | sudo tee /var/lib/jenkins/jenkins.install.InstallUtil.lastExecVersion > /dev/null

# Create simple initialization script
sudo tee /var/lib/jenkins/init.groovy.d/01-basic-security.groovy <<'EOF'
#!groovy
import jenkins.model.*
import hudson.security.*

def instance = Jenkins.getInstance()

if (!(instance.getSecurityRealm() instanceof HudsonPrivateSecurityRealm)) {
    def hudsonRealm = new HudsonPrivateSecurityRealm(false)
    hudsonRealm.createAccount("admin", "password")
    instance.setSecurityRealm(hudsonRealm)
    
    def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
    strategy.setAllowAnonymousRead(false)
    instance.setAuthorizationStrategy(strategy)
    
    instance.save()
    println "Jenkins security configured: admin/password"
}
EOF

# Add credentials in Jenkins
sudo tee /var/lib/jenkins/init.groovy.d/02-credentials.groovy <<'EOF'
#!groovy
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.impl.*
import hudson.util.Secret

Credentials c = new UsernamePasswordCredentialsImpl(
  CredentialsScope.GLOBAL,
  "dockerhub-cred-id",  // ID to use in pipelines
  "DockerHub credentials",
  "ritikjha1245",  // Username
  "your-dockerhub-password"  // Password - change this to your DockerHub password
)

SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), c)
println "--> DockerHub credentials added to Jenkins"
EOF

# Configure systemd for Jenkins
sudo mkdir -p /etc/systemd/system/jenkins.service.d/
sudo tee /etc/systemd/system/jenkins.service.d/override.conf <<EOF
[Service]
Environment="JAVA_OPTS=-Djava.awt.headless=true -Djenkins.install.runSetupWizard=false -Xmx2g -Xms512m -XX:MaxMetaspaceSize=256m"
TimeoutStartSec=300
Restart=no
EOF

# Set proper permissions
sudo chown -R jenkins:jenkins /var/lib/jenkins/
sudo systemctl daemon-reload

# Start Jenkins
log "Starting Jenkins..."
sudo systemctl enable jenkins
sudo systemctl start jenkins

# Wait for Jenkins to start
log "Waiting for Jenkins to be ready..."
JENKINS_READY=false
for i in {1..60}; do
    if systemctl is-active --quiet jenkins; then
        if curl -u admin:password http://localhost:8080 >/dev/null 2>&1; then
            log "Jenkins is ready!"
            JENKINS_READY=true
            break
        fi
    fi
    log "Jenkins starting... ($i/60)"
    sleep 5
done

if [ "$JENKINS_READY" = false ]; then
    log "ERROR: Jenkins failed to start properly"
    log "Jenkins service status:"
    sudo systemctl status jenkins --no-pager
    log "Recent Jenkins logs:"
    sudo journalctl -u jenkins -n 20 --no-pager
    exit 1
fi

# Install Jenkins plugins
log "Installing Jenkins plugins..."
sleep 10

# Download Jenkins CLI
sudo wget -O /tmp/jenkins-cli.jar http://localhost:8080/jnlpJars/jenkins-cli.jar

# Install essential plugins
PLUGINS=(
    "workflow-aggregator"          # Pipeline plugin
    "git"                         # Git support
    "github"                      # GitHub integration
    "docker-plugin"               # Docker plugin
    "docker-workflow"             # Docker pipeline steps
    "sonar"                       # SonarQube integration
    "credentials-binding"         # Credentials binding
    "timestamper"                 # Build timestamps
    "ws-cleanup"                  # Workspace cleanup
    "junit"                       # JUnit test results
    "blueocean"                   # Blue Ocean UI
)

log "Installing plugins with Jenkins CLI..."
for plugin in "${PLUGINS[@]}"; do
    log "Installing: $plugin"
    java -jar /tmp/jenkins-cli.jar -s http://localhost:8080 -auth admin:password install-plugin "$plugin" || log "Warning: Failed to install $plugin"
    sleep 2
done

# Restart Jenkins to activate plugins
log "Restarting Jenkins to activate plugins..."
sudo systemctl restart jenkins

# Wait for restart
sleep 20
for i in {1..60}; do curl -u admin:password http://localhost:8080 && break || sleep 5; done


# Configure credentials if SonarQube token is available
if [ -n "$SONAR_TOKEN" ]; then
    log "Configuring SonarQube credentials..."
    
    sudo tee /var/lib/jenkins/init.groovy.d/sonar-credentials.groovy <<'EOF'
#!groovy
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import org.jenkinsci.plugins.plaincredentials.impl.*
import hudson.util.Secret
import jenkins.model.*

def store = SystemCredentialsProvider.getInstance().getStore()

// Check if credential already exists
def existingCreds = store.getCredentials(Domain.global())
if (!existingCreds.any { it.id == "sonar-token" }) {
    def sonarCred = new StringCredentialsImpl(
        CredentialsScope.GLOBAL,
        "sonar-token",
        "SonarQube Token",
        Secret.fromString("$SONAR_TOKEN")
    )
    store.addCredentials(Domain.global(), sonarCred)
    Jenkins.getInstance().save()
    println("SonarQube credential configured")
}
EOF

    sudo chown jenkins:jenkins /var/lib/jenkins/init.groovy.d/sonar-credentials.groovy
    
    # Restart again to load credentials
    log "Restarting Jenkins to load credentials..."
    sudo systemctl restart jenkins
    sleep 15
    for i in {1..60}; do curl -u admin:password http://localhost:8080 && break || sleep 5; done

fi

# Create SonarQube webhook
log "Creating SonarQube webhook..."
WEBHOOK_RESPONSE=$(curl -s -u admin:admin -X POST "http://localhost:9000/api/webhooks/create" \
    -d "name=Jenkins-Webhook" \
    -d "url=http://localhost:8080/sonarqube-webhook/" 2>/dev/null || echo "failed")

if [[ "$WEBHOOK_RESPONSE" != "failed" && "$WEBHOOK_RESPONSE" != *"error"* ]]; then
    log "SonarQube webhook created successfully"
else
    log "Note: SonarQube webhook needs to be created manually"
fi

# Cleanup
sudo rm -f /tmp/jenkins-cli.jar

# Final verification
log "Performing final verification..."

# Check all services
DOCKER_STATUS=$(systemctl is-active docker || echo "inactive")
JENKINS_STATUS=$(systemctl is-active jenkins || echo "inactive")
SONARQUBE_STATUS=$(docker ps --filter name=sonarqube-server --format "{{.Status}}" | head -n1)

log ""
log "============================================"
log "INSTALLATION COMPLETED!"
log "============================================"
log ""
log "Service Status:"
log "- Docker: $DOCKER_STATUS"
log "- Jenkins: $JENKINS_STATUS"
log "- SonarQube: $SONARQUBE_STATUS"
log ""
log "Access URLs:"
log "- Jenkins: http://localhost:8080"
log "  Username: admin"
log "  Password: password"
log ""
log "- SonarQube: http://localhost:9000"
log "  Username: admin"
log "  Password: admin"
log ""
log "Installed Jenkins Plugins:"
for plugin in "${PLUGINS[@]}"; do
    log "  ✓ $plugin"
done
log ""

if [ -n "$SONAR_TOKEN" ]; then
    log "SonarQube Integration:"
    log "  ✓ Token generated and configured"
    log "  ✓ Credentials added to Jenkins"
    log "  Token ID: sonar-token"
    log ""
fi

log "Next Steps:"
log "1. Access Jenkins at http://localhost:8080 (admin/password)"
log "2. Access SonarQube at http://localhost:9000 (admin/admin)"
log "3. In Jenkins, go to Manage Jenkins > Configure System"
log "4. Add SonarQube server:"
log "   - Name: SonarQube"
log "   - Server URL: http://localhost:9000"
log "   - Server authentication token: sonar-token"
log "5. Create your first pipeline!"
log ""
log "Documentation:"
log "- Jenkins: https://www.jenkins.io/doc/"
log "- SonarQube: https://docs.sonarqube.org/"
log "- Docker: https://docs.docker.com/"
log ""
log "Installation completed successfully!"