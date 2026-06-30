#!/bin/bash
# scripts/adb-test-loop.sh

echo "Starting test loop..."
for i in {1..3}; do
    echo "Iteration $i"
    adb -s localhost:5555 shell uiautomator dump /sdcard/ui.xml
    adb -s localhost:5555 pull /sdcard/ui.xml /tmp/ui.xml
    adb -s localhost:5555 exec-out screencap -p > "/tmp/screen_$i.png"
    sleep 2
done
echo "Test loop complete."
