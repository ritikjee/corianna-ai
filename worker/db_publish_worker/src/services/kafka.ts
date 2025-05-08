import { Consumer, Kafka as kafkajs, EachMessagePayload } from "kafkajs";
import { KAFKA_DEFAULT_TOPIC } from "../constants";

export class KafkaClient {
  private kafka: kafkajs;
  private consumer: Consumer;

  constructor(brokers: string[]) {
    this.kafka = new kafkajs({
      clientId: "my-app",
      brokers,
    });
    this.consumer = this.kafka.consumer({ groupId: KAFKA_DEFAULT_TOPIC });
  }

  async connect() {
    try {
      await this.consumer.connect();
    } catch (error) {
      console.error("Error connecting to Kafka:", error);
      process.exit(1);
    }
  }

  async init(onMessage: (payload: EachMessagePayload) => Promise<void>) {
    await this.consumer.subscribe({
      topic: KAFKA_DEFAULT_TOPIC,
      fromBeginning: true,
    });

    console.log(`Subscribed to topic ${KAFKA_DEFAULT_TOPIC}`);
    console.log("Waiting for messages...");

    await this.consumer.run({
      eachMessage: async (payload) => {
        console.log(
          `Received message from topic ${payload.topic} and partition ${payload.partition}`,
        );

        await onMessage(payload);
      },
    });
  }
}
