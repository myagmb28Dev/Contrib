FROM node:22-alpine AS dependencies
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

FROM node:22-alpine AS build
WORKDIR /app
ARG NEXT_PUBLIC_API_BASE_URL
ARG API_PROXY_TARGET
ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL
ENV API_PROXY_TARGET=$API_PROXY_TARGET
RUN test -n "$NEXT_PUBLIC_API_BASE_URL" || (echo "NEXT_PUBLIC_API_BASE_URL build argument is required" >&2; exit 1)
COPY --from=dependencies /app/node_modules ./node_modules
COPY . .
RUN npm run build

FROM node:22-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
ENV HOSTNAME=0.0.0.0
ENV PORT=3000
RUN addgroup -S contrib && adduser -S contrib -G contrib
COPY --from=build --chown=contrib:contrib /app/.next/standalone ./
COPY --from=build --chown=contrib:contrib /app/.next/static ./.next/static
USER contrib
EXPOSE 3000
CMD ["node", "server.js"]
