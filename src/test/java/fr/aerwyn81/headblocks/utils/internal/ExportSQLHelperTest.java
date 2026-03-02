package fr.aerwyn81.headblocks.utils.internal;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.databases.EnumTypeDatabase;
import fr.aerwyn81.headblocks.services.StorageService;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ExportSQLHelperTest {

    @TempDir
    Path tempDir;

    @Test
    void generateFile_createsFileWithHeaderAndInstructions() throws Exception {
        try (MockedStatic<HeadBlocks> hbStatic = mockStatic(HeadBlocks.class)) {
            HeadBlocks plugin = mock(HeadBlocks.class);
            hbStatic.when(HeadBlocks::getInstance).thenReturn(plugin);
            when(plugin.getName()).thenReturn("HeadBlocks");
            when(plugin.getDataFolder()).thenReturn(tempDir.toFile());

            PluginDescriptionFile desc = mock(PluginDescriptionFile.class);
            when(plugin.getDescription()).thenReturn(desc);
            when(desc.getVersion()).thenReturn("3.0.0");

            ServiceRegistry registry = mock(ServiceRegistry.class);
            StorageService storageService = mock(StorageService.class);
            when(registry.getStorageService()).thenReturn(storageService);

            ArrayList<String> instructions = new ArrayList<>(Arrays.asList(
                    "CREATE TABLE players (id TEXT);",
                    "INSERT INTO players VALUES ('test');"
            ));
            when(storageService.getInstructionsExport(EnumTypeDatabase.SQLite)).thenReturn(instructions);

            ExportSQLHelper.generateFile(registry, EnumTypeDatabase.SQLite, "test-export.sql");

            File exportedFile = new File(tempDir.toFile(), "test-export.sql");
            assertThat(exportedFile).exists();

            String content = Files.readString(exportedFile.toPath());
            assertThat(content).contains("HeadBlocks 3.0.0");
            assertThat(content).contains("CREATE TABLE players (id TEXT);");
            assertThat(content).contains("INSERT INTO players VALUES ('test');");
            assertThat(content).contains("UTF-8");
        }
    }

    @Test
    void generateFile_overwritesExistingFile() throws Exception {
        try (MockedStatic<HeadBlocks> hbStatic = mockStatic(HeadBlocks.class)) {
            HeadBlocks plugin = mock(HeadBlocks.class);
            hbStatic.when(HeadBlocks::getInstance).thenReturn(plugin);
            when(plugin.getName()).thenReturn("HeadBlocks");
            when(plugin.getDataFolder()).thenReturn(tempDir.toFile());

            PluginDescriptionFile desc = mock(PluginDescriptionFile.class);
            when(plugin.getDescription()).thenReturn(desc);
            when(desc.getVersion()).thenReturn("3.0.0");

            ServiceRegistry registry = mock(ServiceRegistry.class);
            StorageService storageService = mock(StorageService.class);
            when(registry.getStorageService()).thenReturn(storageService);
            when(storageService.getInstructionsExport(EnumTypeDatabase.SQLite)).thenReturn(new ArrayList<>());

            // Create pre-existing file
            File existing = new File(tempDir.toFile(), "overwrite.sql");
            Files.writeString(existing.toPath(), "old content");

            ExportSQLHelper.generateFile(registry, EnumTypeDatabase.SQLite, "overwrite.sql");

            String content = Files.readString(existing.toPath());
            assertThat(content).doesNotContain("old content");
            assertThat(content).contains("HeadBlocks");
        }
    }

    @Test
    void generateFile_emptyInstructions_onlyWritesHeader() throws Exception {
        try (MockedStatic<HeadBlocks> hbStatic = mockStatic(HeadBlocks.class)) {
            HeadBlocks plugin = mock(HeadBlocks.class);
            hbStatic.when(HeadBlocks::getInstance).thenReturn(plugin);
            when(plugin.getName()).thenReturn("HeadBlocks");
            when(plugin.getDataFolder()).thenReturn(tempDir.toFile());

            PluginDescriptionFile desc = mock(PluginDescriptionFile.class);
            when(plugin.getDescription()).thenReturn(desc);
            when(desc.getVersion()).thenReturn("3.0.0");

            ServiceRegistry registry = mock(ServiceRegistry.class);
            StorageService storageService = mock(StorageService.class);
            when(registry.getStorageService()).thenReturn(storageService);
            when(storageService.getInstructionsExport(EnumTypeDatabase.MySQL)).thenReturn(new ArrayList<>());

            ExportSQLHelper.generateFile(registry, EnumTypeDatabase.MySQL, "empty.sql");

            File file = new File(tempDir.toFile(), "empty.sql");
            String content = Files.readString(file.toPath());
            assertThat(content).contains("-- ");
            assertThat(content).contains("HeadBlocks 3.0.0");
        }
    }
}
