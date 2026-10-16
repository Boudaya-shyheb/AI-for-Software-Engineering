FROM node:24-alpine
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --legacy-peer-deps
COPY . .
EXPOSE 5173
CMD ["npm", "run", "start"]
