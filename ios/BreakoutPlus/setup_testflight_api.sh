#!/bin/bash
# TestFlight API Setup Script
# Creates App Store Connect API key for automated uploads

echo "🔑 TestFlight API Setup for Breakout+"
echo "======================================"

# Create private_keys directory
mkdir -p private_keys

echo ""
echo "📝 You need to create an App Store Connect API key:"
echo ""
echo "1. Go to https://appstoreconnect.apple.com/access/api"
echo "2. Click '+' to generate a new API Key"
echo "3. Select 'Developer' role (or higher)"
echo "4. Download the .p8 file"
echo "5. Save it as: private_keys/AuthKey_XXXXXXXXXX.p8"
echo ""
echo "6. Copy the Key ID and Issuer ID from the website"
echo ""

read -p "Enter your API Key ID: " API_KEY_ID
read -p "Enter your Issuer ID: " ISSUER_ID

# Create API key file
cat > private_keys/api_key.json << EOF
{
    "key_id": "$API_KEY_ID",
    "issuer_id": "$ISSUER_ID",
    "key_filepath": "./private_keys/AuthKey_${API_KEY_ID}.p8"
}
EOF

echo ""
echo "✅ API key configuration saved to private_keys/api_key.json"
echo ""
echo "🔄 Next: Place your AuthKey_${API_KEY_ID}.p8 file in the private_keys directory"
echo ""
echo "🚀 Then run: ./upload_testflight.sh"