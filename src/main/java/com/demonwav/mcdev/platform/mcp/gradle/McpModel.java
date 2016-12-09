/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.gradle;

import java.util.Set;

public interface McpModel {

    String getMinecraftVersion();

    String getMcpVersion();

    Set<String> getMappingFiles();

}
