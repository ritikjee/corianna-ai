import "dotenv/config";

import { KafkaClient } from "./services/kafka";
import { KAFKA_MESSAGE } from "./types";
import { ChromaDB } from "./services/chromadb";

async function main() {
  const KAFKA_BROKER_URL = process.env.KAFKA_BROKER_URL;

  if (!KAFKA_BROKER_URL) {
    throw new Error("KAFKA_BROKER_URL is not defined");
  }

  const client = new KafkaClient([KAFKA_BROKER_URL]);
  const chromaClient = new ChromaDB();

  await client.connect();
  await chromaClient.init();

  console.log("Connected to Kafka and ChromaDB");

  const messageBuffer: KAFKA_MESSAGE[] = [];
  const MAX_BATCH_SIZE = 100;
  const FLUSH_INTERVAL_MS = 30_000;

  let flushTimeout: NodeJS.Timeout;

  const flushMessages = async () => {
    if (messageBuffer.length === 0) return;

    const batch = messageBuffer.splice(0, messageBuffer.length); // clear buffer
    await chromaClient.addDocuments(batch);
    console.log(`Flushed ${batch.length} messages to ChromaDB`);
  };

  const scheduleFlush = () => {
    clearTimeout(flushTimeout); // reset timer
    flushTimeout = setTimeout(async () => {
      await flushMessages();
      scheduleFlush(); // reschedule
    }, FLUSH_INTERVAL_MS);
  };

  scheduleFlush();

  await client.init(async (payload) => {
    const {
      message: { value },
    } = payload;

    if (!value || !value.toString()) {
      console.log("No message received");
      return;
    }

    try {
      const parsed: KAFKA_MESSAGE = JSON.parse(value.toString());
      console.log("Received message:", parsed?.embedding[0]?.values?.length);

      messageBuffer.push(parsed);

      if (messageBuffer.length >= MAX_BATCH_SIZE) {
        console.log("Batch size reached, flushing messages");
        await flushMessages();
        console.log("Flushing messages");
        scheduleFlush();
      }
    } catch (err) {
      console.error("Invalid message format:", err);
    }
  });
}

main();
