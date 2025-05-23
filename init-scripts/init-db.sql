SELECT 'CREATE DATABASE auth_service'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'auth_service'
)\gexec

SELECT 'CREATE DATABASE app_service'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'app_service'
)\gexec

SELECT 'CREATE DATABASE auth_service'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'integration_service'
)\gexec
