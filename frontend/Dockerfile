# Build stage
FROM node:16-alpine as build

WORKDIR /app

# Set environment variables to ignore certificate errors
ENV NODE_TLS_REJECT_UNAUTHORIZED=0

# Copy package files
COPY package.json package-lock.json ./

# Configure npm to ignore certificate errors
RUN npm config set strict-ssl false

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Build the app
RUN npm run build

# Production stage
FROM nginx:alpine

# Copy built assets from build stage
COPY --from=build /app/build /usr/share/nginx/html

# Copy nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port
EXPOSE 3000

# Start nginx
CMD ["nginx", "-g", "daemon off;"]