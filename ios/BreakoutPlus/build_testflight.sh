#!/bin/bash
# Breakout+ TestFlight Build Script
# CLI-only TestFlight deployment

set -e

echo "🎮 Breakout+ TestFlight Build Script"
echo "===================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$PROJECT_DIR/build"
ARCHIVE_PATH="$BUILD_DIR/BreakoutPlus.xcarchive"
EXPORT_PATH="$BUILD_DIR/TestFlight"

echo -e "${BLUE}Project Directory: $PROJECT_DIR${NC}"
echo -e "${BLUE}Build Directory: $BUILD_DIR${NC}"

# Create build directory
mkdir -p "$BUILD_DIR"

echo -e "\n${YELLOW}Step 1: Cleaning previous builds...${NC}"
xcodebuild clean -project BreakoutPlus.xcodeproj -scheme BreakoutPlus

echo -e "\n${YELLOW}Step 2: Building archive...${NC}"
xcodebuild archive \
    -project BreakoutPlus.xcodeproj \
    -scheme BreakoutPlus \
    -configuration Release \
    -archivePath "$ARCHIVE_PATH" \
    -destination "generic/platform=iOS"

echo -e "\n${YELLOW}Step 3: Exporting for TestFlight...${NC}"
xcodebuild -exportArchive \
    -archivePath "$ARCHIVE_PATH" \
    -exportOptionsPlist "$PROJECT_DIR/fastlane/ExportOptions.plist" \
    -exportPath "$EXPORT_PATH"

IPA_PATH="$EXPORT_PATH/BreakoutPlus.ipa"

if [ -f "$IPA_PATH" ]; then
    echo -e "\n${GREEN}✅ Build successful!${NC}"
    echo -e "${BLUE}IPA located at: $IPA_PATH${NC}"

    echo -e "\n${YELLOW}Next steps for TestFlight upload:${NC}"
    echo "1. Go to https://appstoreconnect.apple.com/"
    echo "2. Navigate to My Apps → Breakout+"
    echo "3. Go to TestFlight tab"
    echo "4. Click '+' to add a new build"
    echo "5. Upload the IPA file: $IPA_PATH"
    echo ""
    echo -e "${RED}Note: You need Apple Developer Program membership ($99/year)${NC}"
    echo -e "${RED}And proper code signing certificates set up${NC}"
else
    echo -e "\n${RED}❌ Build failed - IPA not found${NC}"
    exit 1
fi