#!/bin/bash

echo "Starting complete removal of Docker, Jenkins, SonarQube, and Java..."

# Stop all services first
echo "Stopping services..."
sudo systemctl stop jenkins 2>/dev/null || true
sudo systemctl stop docker 2>/dev/null || true
sudo systemctl stop containerd 2>/dev/null || true

# Remove Jenkins completely
echo "Removing Jenkins..."
sudo systemctl disable jenkins 2>/dev/null || true
sudo apt-get remove --purge jenkins -y 2>/dev/null || true
sudo rm -rf /var/lib/jenkins
sudo rm -rf /var/cache/jenkins
sudo rm -rf /var/log/jenkins
sudo rm -rf /etc/systemd/system/jenkins.service.d/
sudo rm -f /etc/apt/sources.list.d/jenkins.list
sudo rm -f /etc/apt/keyrings/jenkins-keyring.asc
sudo userdel -r jenkins 2>/dev/null || true
sudo groupdel jenkins 2>/dev/null || true

# Stop and remove SonarQube Docker container
echo "Removing SonarQube..."
sudo docker stop SonarQube-Server 2>/dev/null || true
sudo docker rm SonarQube-Server 2>/dev/null || true
sudo docker rmi sonarqube:lts-community 2>/dev/null || true

# Remove all Docker containers, images, volumes, and networks
echo "Removing all Docker containers, images, volumes, and networks..."
sudo docker stop $(sudo docker ps -aq) 2>/dev/null || true
sudo docker rm $(sudo docker ps -aq) 2>/dev/null || true
sudo docker rmi $(sudo docker images -q) 2>/dev/null || true
sudo docker volume prune -f 2>/dev/null || true
sudo docker network prune -f 2>/dev/null || true
sudo docker system prune -af 2>/dev/null || true

# Remove Docker packages
echo "Removing Docker packages..."
sudo systemctl disable docker 2>/dev/null || true
sudo systemctl disable containerd 2>/dev/null || true
sudo apt-get remove --purge docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin -y
sudo apt-get remove --purge docker.io docker-doc docker-compose docker-compose-v2 podman-docker containerd runc -y
sudo apt-get autoremove -y

# Remove Docker directories and files
echo "Removing Docker directories and files..."
sudo rm -rf /var/lib/docker
sudo rm -rf /var/lib/containerd
sudo rm -rf /etc/docker
sudo rm -rf /etc/systemd/system/docker.service.d
sudo rm -rf /etc/systemd/system/containerd.service.d
sudo rm -rf ~/.docker

# Remove Docker repository and GPG key
echo "Removing Docker repository and GPG key..."
sudo rm -f /etc/apt/sources.list.d/docker.list
sudo rm -f /etc/apt/keyrings/docker.asc

# Remove user from docker group
echo "Removing user from docker group..."
sudo deluser $USER docker 2>/dev/null || true
sudo groupdel docker 2>/dev/null || true

# Remove Java (OpenJDK 21)
echo "Removing OpenJDK 21..."
sudo apt-get remove --purge openjdk-21-jdk openjdk-21-jre openjdk-21-jre-headless -y
sudo apt-get remove --purge 'openjdk-21-*' -y

# Remove any leftover Java alternatives
sudo update-alternatives --remove-all java 2>/dev/null || true
sudo update-alternatives --remove-all javac 2>/dev/null || true
sudo update-alternatives --remove-all javaws 2>/dev/null || true

# Clean up package cache and dependencies
echo "Cleaning up packages..."
sudo apt-get autoremove -y
sudo apt-get autoclean
sudo apt-get clean

# Update package lists
sudo apt-get update

# Reload systemd daemon
sudo systemctl daemon-reload

# Reset user groups (logout/login required for full effect)
echo "Resetting user groups..."

echo ""
echo "============================================"
echo "REMOVAL COMPLETED SUCCESSFULLY!"
echo "============================================"
echo ""
echo "Removed components:"
echo "- Docker Engine, CLI, and all plugins"
echo "- All Docker containers, images, volumes, and networks"
echo "- Docker repositories and GPG keys"
echo "- Jenkins and all its data"
echo "- SonarQube container"
echo "- OpenJDK 21"
echo "- All configuration files and directories"
echo ""
echo "IMPORTANT NOTES:"
echo "1. Please LOGOUT and LOGIN again (or reboot) to completely reset user groups"
echo "2. Some Docker network interfaces might persist until reboot"
echo "3. If you installed other Java versions, they remain untouched"
echo ""
echo "To verify removal, run these commands after logout/login:"
echo "  docker --version          (should show 'command not found')"
echo "  java -version            (should show 'command not found' or different version)"
echo "  systemctl status jenkins  (should show 'Unit jenkins.service could not be found')"
echo "  groups                   (should not show 'docker' group)"
echo ""
echo "System cleanup completed!"