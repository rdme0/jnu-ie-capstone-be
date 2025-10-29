#!/bin/bash
set -e

echo "🔄 origin main branch pull 중..."
git pull origin main

echo "🔧 ./gradlew 권한 +x로 변경 중..."
chmod +x ./gradlew

echo "🔨 gradle로 Jar 빌드 중"
./gradlew clean build -x test

echo "✅ 배포 완료"