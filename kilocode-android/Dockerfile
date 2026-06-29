# Use an official Node.js image
FROM node:18-slim

# Install necessary dependencies if needed (e.g., git or build tools)
RUN apt-get update && apt-get install -y git && rm -rf /var/lib/apt/lists/*

# Install Kilo CLI globally
RUN npm install -g @kilocode/cli

# Create a directory for your application data
WORKDIR /app

# Run the Kilo server
# Ensure you map the port to Render's required PORT environment variable
CMD ["sh", "-c", "kilo serve --port $PORT"]
