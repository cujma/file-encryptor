
#!/usr/bin/env bash

cd "$(dirname "$0")" || exit 1

./gradlew classes || exit 1

exec java -cp build/classes/java/main Main
