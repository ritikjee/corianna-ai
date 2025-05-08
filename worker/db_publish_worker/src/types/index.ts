export type KAFKA_MESSAGE_METADATA = {
  url: string;
  websiteId: string;
  sectionNo: number;
};

export type KAFKA_MESSAGE = {
  metadata: KAFKA_MESSAGE_METADATA;
  embedding: {
    values: string[];
  }[];
};
