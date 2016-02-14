# Colony-Wars
Reprogramming of popular minecraft mini-game Minecraft Colony Wars. Supports all of the (final) original classes
(final means excluding classes that were removed in later versions of the original). Currently has Canyon and
Forest Hills map recreations, and schematics for all the buildings.

# Information
When the game starts, the selected map will be copied to the "playing" folder, and all players teleported into it.
The folder should be deleted when the game ends.

# Setup
### Compiling
Compile the project using maven. The plugin jar file will be outputted to target/Colony_Wars.jar.

### Dependencies
The plugin requires these other utility plugins to be installed:
* [HologramAPI](https://www.spigotmc.org/resources/api-hologramapi-1-7-1-8.6766/)
Required to display holograms for buildings
* [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/)
Required to allow cloakers to be attacked while invisible

### Plugin Setup
* Copy the plugin jar to your server's plugin folder.
* Download maps/schematics from https://files.ithinkrok.com/cw_resources.zip and follow the readme.md to install them
* Create a directory called "maps" in the "plugins/ColonyWars" folder
