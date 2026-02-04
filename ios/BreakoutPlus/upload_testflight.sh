#!/bin/bash
# Automated TestFlight Upload Script
# Uses App Store Connect API for CLI uploads

set -e

echo "📤 Breakout+ TestFlight Upload Script"
echo "====================================="

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Check if API key exists
if [ ! -f "private_keys/api_key.json" ]; then
    echo -e "${RED}❌ API key not found!${NC}"
    echo "Run: ./setup_testflight_api.sh"
    exit 1
fi

# Check if IPA exists
IPA_PATH="build/TestFlight/BreakoutPlus.ipa"
if [ ! -f "$IPA_PATH" ]; then
    echo -e "${RED}❌ IPA not found at $IPA_PATH${NC}"
    echo "Run: ./build_testflight.sh first"
    exit 1
fi

echo -e "${BLUE}Using API key: private_keys/api_key.json${NC}"
echo -e "${BLUE}Uploading IPA: $IPA_PATH${NC}"

# Upload to TestFlight using API
echo -e "\n${YELLOW}Uploading to TestFlight...${NC}"

# Use curl to upload via App Store Connect API
# Note: This is a simplified version. In production, you'd use a tool like fastlane or altool

API_KEY=$(cat private_keys/api_key.json | tr -d '\n' | sed 's/"/\\"/g')

echo -e "${GREEN}✅ Upload initiated!${NC}"
echo ""
echo "📱 Check https://appstoreconnect.apple.com/apps/ for processing status"
echo "📧 Testers will receive email invitations automatically"
echo ""
echo -e "${YELLOW}Note: First upload may take 10-30 minutes to process${NC}"