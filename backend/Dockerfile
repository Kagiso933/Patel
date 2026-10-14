FROM node:22-alpine AS base
WORKDIR /app

# Install native build tools needed by better-sqlite3
RUN apk add --no-cache python3 make g++

COPY package*.json ./
RUN npm ci --omit=dev

# ── Production image ──────────────────────────────────────────────────────────
FROM node:22-alpine
WORKDIR /app

RUN addgroup -S rynotec && adduser -S rynotec -G rynotec

COPY --from=base /app/node_modules ./node_modules
COPY src ./src
COPY package.json .

# Data volume for SQLite — mount externally in production
RUN mkdir -p /app/data && chown rynotec:rynotec /app/data
VOLUME ["/app/data"]

USER rynotec

ENV NODE_ENV=production
EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s \
  CMD wget -qO- http://localhost:3000/health || exit 1

CMD ["node", "src/app.js"]
