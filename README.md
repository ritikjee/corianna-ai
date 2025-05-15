# Corianna AI: Modular Microservice AI Chatbot Platform

Corianna AI is a full-stack, containerized AI chatbot platform with modular microservices, streaming pipelines, and embedding/vector-based search — designed to be embeddable in user websites.

---

## ✨ Features

* User Authentication and Device Management (`auth_service`)
* Interactive Web Dashboard (`app_service`)
* Embeddable AI Chatbot with Contextual Website Responses (`bot_service`)
* Asynchronous Workers:

  * `scraper`: Crawls websites and extracts data
  * `chat_worker`: Handles embedding and context generation
  * `ai_worker`: Generates AI responses
  * `db_publish_worker`: Pushes processed data to ChromaDB
  * `webhook_worker`: (planned) Listens to external integrations

---

## 📂 Project Structure

```
ritikjee-corianna-ai/
├── docker-compose.yaml           # Core infrastructure (Kafka, Redis, ChromaDB, RabbitMQ)
├── services/
│   ├── app_service/              # Spring Boot app dashboard service
│   ├── auth_service/             # Spring Boot authentication and device session manager
│   └── bot-service/              # API for chatbot interaction and Kafka production
└── worker/
    ├── ai_worker/                # Consumes embeddings and generates AI responses
    ├── chat_worker/              # Embeds questions + pulls context from ChromaDB
    ├── db_publish_worker/        # Persists batches of vector data to ChromaDB
    └── scraper/                  # Scrapes and sends content for embedding
```

---

## ⚙️ Infrastructure

**Compose Services:**

* `kafka`, `zookeeper` — For event streaming and worker pipelines
* `rabbitmq` — For initial task distribution (e.g. scraping)
* `chromadb` — Vector database for similarity search
* `postgres`, `pgadmin` — Primary RDBMS
* `redis`, `redis_insights` — Caching + rate limiting per worker

---

## ⚡ Workflows

### 1. User Adds Site (via `app_service`)

* Authenticated user submits: URL, prompt examples, and private paths.
* Data is saved and queued via RabbitMQ.

### 2. Scraping + Embedding

* `scraper` crawls the site and sends clean HTML/text
* `chat_worker` embeds this and pushes to `db_publish_worker`
* `db_publish_worker` saves it in ChromaDB

### 3. Bot Interaction

* User's chatbot question hits `bot_service`
* Message goes to `chat_worker` (Kafka topic)
* Embedding + context lookup happens
* `ai_worker` generates the response
* Response is sent back and stored in Redis for polling

---

## 🛠️ Rate Limiting

* Each worker (e.g. `chat_worker`) uses Redis key (e.g. `chat_worker_rate_limit`)
* Embeddings and AI calls are rate-controlled using Redis atomic counters
* Background process resets rate limits every minute

---

## 🚀 Running the System

```bash
docker-compose up --build
```

To run an individual service:

```bash
cd worker/chat_worker
pnpm install && pnpm dev
```

---

## 🔹 Development Notes

* Java services use Maven with Spring Boot 3.4.x
* TypeScript workers use `ts-node`, `nodemon`, and `kafkajs`
* ChromaDB runs in container mode with exposed port 8000
* Redis is used for:

  * Rate limiting
  * Message caching

---

## 🚪 Future Services (Planned)

* `webhook_worker`: For third-party integrations (Slack, Discord, etc.)
* `analytics_service`: Token usage, billing, user stats
* `feedback_collector`: Human feedback on generated answers

---

## ✊ Contributing

PRs and feedback are welcome. Please format with Prettier, use semantic commits, and follow existing folder conventions.

---

## ❤️ License

MIT License (c) 2025 Corianna AI

