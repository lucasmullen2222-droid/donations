package donations.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DonoAlertsClient implements ClientModInitializer {
    public static final String MOD_ID = "uh_dylan-donations";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static Config CONFIG;
    public static double sessionTotal = 0;
    public static int sessionCount = 0;

    private static final Identifier SKIN = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/skin.png");
    private static KeyMapping menuKey;

    @Override
    public void onInitializeClient() {
        CONFIG = Config.load();

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        menuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.donoalerts.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (menuKey.consumeClick()) {
                if (mc.gui.screen() == null) mc.gui.setScreen(new MenuScreen());
            }
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "alerts"),
            (graphics, deltaTracker) -> AlertManager.render(graphics));

        // Watch chat for "<name> paid you $<amount>"
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            Alert a = PaymentParser.parse(message.getString());
            if (a == null || a.amount < CONFIG.minAmount) return;
            sessionTotal += a.amount;
            sessionCount++;
            AlertManager.push(a);
        });

        // /dono <name> <amount like 5m or 250k>  -> fake payment for testing
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommands.literal("dono")
                .then(ClientCommands.argument("name", StringArgumentType.word())
                    .then(ClientCommands.argument("amount", StringArgumentType.word())
                        .executes(ctx -> {
                            try {
                                double amt = PaymentParser.parseAmountArg(StringArgumentType.getString(ctx, "amount"));
                                AlertManager.push(new Alert(StringArgumentType.getString(ctx, "name"), amt));
                            } catch (Exception e) {
                                ctx.getSource().sendFeedback(Component.literal("Use: /dono <name> <amount>  e.g. /dono Bob 25m"));
                            }
                            return 1;
                        })))));
    }

    /** Draws the face + hat layer of skin.png as a size x size square. */
    public static void drawHead(GuiGraphicsExtractor g, int x, int y, int size) {
        g.blit(RenderPipelines.GUI_TEXTURED, SKIN, x, y, 8f, 8f, size, size, 8, 8, 64, 64);   // face
        g.blit(RenderPipelines.GUI_TEXTURED, SKIN, x, y, 40f, 8f, size, size, 8, 8, 64, 64);  // hat layer
    }
}
