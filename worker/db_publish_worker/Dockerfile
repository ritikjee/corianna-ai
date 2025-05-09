FROM node:23-alpine AS builder

WORKDIR /app

COPY . .

RUN npm install

RUN npm run build

FROM node:23-alpine AS runner

WORKDIR /app

COPY --from=builder /app/dist ./dist
COPY --from=builder /app/package.json ./

RUN npm install --omit=dev

CMD [ "npm","run","start" ]

