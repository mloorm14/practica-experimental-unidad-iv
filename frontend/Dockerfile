# syntax=docker/dockerfile:1

# ---- Stage 1: build ----
# Angular CLI 22 exige Node ^22.22.3 || ^24.15.0 || >=26.0.0 (node:20 no alcanza).
FROM node:24-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build -- --configuration production

# ---- Stage 2: runtime ----
FROM nginx:alpine
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
