#!/bin/bash
# .kilo/auth-gh.sh
# Usage: ./auth-gh.sh <GITHUB_TOKEN>

if [ -z "$1" ]; then
  echo "Error: GITHUB_TOKEN is required."
  exit 1
fi

echo "$1" | gh auth login --with-token
if [ $? -eq 0 ]; then
  echo "Successfully authenticated with GitHub."
else
  echo "Failed to authenticate."
  exit 1
fi
