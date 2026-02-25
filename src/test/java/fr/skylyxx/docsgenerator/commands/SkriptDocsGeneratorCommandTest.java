package fr.skylyxx.docsgenerator.commands;

import ch.njol.skript.Skript;
import fr.skylyxx.docsgenerator.SkriptDocsGenerator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.PluginCommandUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.skriptlang.skript.addon.SkriptAddon;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkriptDocsGeneratorCommandTest {

    private ServerMock server;
    private PlayerMock player;
    private SkriptDocsGenerator plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer();
        plugin = Mockito.mock(SkriptDocsGenerator.class);
        Mockito.when(plugin.getColored(Mockito.anyString()))
                .thenAnswer(invocation -> ChatColor.translateAlternateColorCodes('&', invocation.getArgument(0, String.class)));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void commandShowsRegistrationInProgressMessage() {
        try (MockedStatic<Skript> skriptMock = Mockito.mockStatic(Skript.class)) {
            skriptMock.when(Skript::isAcceptRegistrations).thenReturn(true);

            PluginCommand command = createCommand();
            boolean handled = command.execute(player, "skriptdocsgenerator", new String[]{"anything"});

            assertTrue(handled);
            assertTrue(nextMessage().contains("Skript hasn't finished addon registration"));
        }
    }

    @Test
    void commandShowsNotFoundMessageForUnknownAddon() {
        org.skriptlang.skript.Skript skriptInstance = Mockito.mock(org.skriptlang.skript.Skript.class);
        Mockito.when(skriptInstance.name()).thenReturn("Skript");
        Mockito.doReturn(SkriptDocsGenerator.class).when(skriptInstance).source();
        Mockito.when(skriptInstance.addons()).thenReturn(Collections.emptyList());

        try (MockedStatic<Skript> skriptMock = Mockito.mockStatic(Skript.class)) {
            skriptMock.when(Skript::isAcceptRegistrations).thenReturn(false);
            skriptMock.when(Skript::instance).thenReturn(skriptInstance);

            PluginCommand command = createCommand();
            boolean handled = command.execute(player, "skriptdocsgenerator", new String[]{"unknown-addon"});

            assertTrue(handled);
            assertTrue(nextMessage().contains("No addon with name unknown-addon was found"));
        }
    }

    @Test
    void commandGeneratesDocumentationForKnownAddon() {
        SkriptAddon addon = Mockito.mock(SkriptAddon.class);
        org.skriptlang.skript.Skript skriptInstance = Mockito.mock(org.skriptlang.skript.Skript.class);
        Mockito.when(addon.name()).thenReturn("MyAddon");
        Mockito.doReturn(SkriptDocsGeneratorCommandTest.class).when(addon).source();
        Mockito.when(skriptInstance.name()).thenReturn("Skript");
        Mockito.doReturn(SkriptDocsGenerator.class).when(skriptInstance).source();
        Mockito.when(skriptInstance.addons()).thenReturn(List.of(addon));

        try (MockedStatic<Skript> skriptMock = Mockito.mockStatic(Skript.class)) {
            skriptMock.when(Skript::isAcceptRegistrations).thenReturn(false);
            skriptMock.when(Skript::instance).thenReturn(skriptInstance);

            PluginCommand command = createCommand((mainClass, addonInfo) -> 42);
            boolean handled = command.execute(player, "skriptdocsgenerator", new String[]{"MyAddon"});

            assertTrue(handled);
            assertTrue(nextMessage().contains("Documentation generated for MyAddon (42 syntaxes)"));
        }
    }

    private PluginCommand createCommand() {
        return createCommand((mainClass, addon) -> {
            throw new IllegalStateException("Default test doc generator should not be called in this scenario");
        });
    }

    private PluginCommand createCommand(SkriptDocsGeneratorCommand.AddonDocGenerator docGenerator) {
        PluginMock owner = MockBukkit.createMockPlugin();
        PluginCommand command = PluginCommandUtils.createPluginCommand("skriptdocsgenerator", owner);
        command.setExecutor(new SkriptDocsGeneratorCommand(plugin, docGenerator));
        command.setTabCompleter(new SkriptDocsGeneratorCommand(plugin, docGenerator));
        return command;
    }

    private String nextMessage() {
        Component message = player.nextComponentMessage();
        assertNotNull(message);
        return PlainTextComponentSerializer.plainText().serialize(message);
    }
}
