# Shared Boss Uniques

Spring Boot sync server plus RuneLite plugin for sharing boss collection-log uniques between participating OSRS players.

## Start the server

```powershell
./mvnw spring-boot:run
```

Server default URL:

```text
http://localhost:8080
```

## Build the RuneLite plugin

```powershell
cd runelite-plugin
.\gradlew.bat shadowJar
```

Plugin jar:

```text
runelite-plugin/build/libs/gim-boss-uniques-plugin-1.0-SNAPSHOT-all.jar
```

## Use it

1. Start the server and open `http://localhost:8080`.
2. Create a private group and note the generated group code.
3. Install the plugin jar in RuneLite.
4. Set the same group code on every participating player.
5. Set the sync server URL in the plugin config.
6. Open boss pages in the Collection Log on each account.
7. Refresh the plugin panel or visit `http://localhost:8080/groups/<group-code>`.

## Notes

- The plugin tracks boss collection-log uniques, not live bank ownership.
- A boss only becomes known after that boss page has been opened in the Collection Log on that account.
- The shared server currently stores data in memory.
- Groups are isolated by generated group code, so one group's uploads are not visible to another group.
