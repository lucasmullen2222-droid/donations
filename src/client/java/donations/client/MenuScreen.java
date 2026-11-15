package donations.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MenuScreen extends Screen {
    public MenuScreen() { super(Component.literal("Dono Alerts")); }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 + 28;
        int w = 74, gap = 4;
        double[] amts = {1e6, 5e6, 15e6, 25e6, 50e6, 100e6};
        for (int i = 0; i < 6; i++) {
            final double amt = amts[i];
            int col = i % 3, row = i / 3;
            int x = cx - (w * 3 + gap * 2) / 2 + col * (w + gap);
            addRenderableWidget(Button.builder(Component.literal("Test $" + Alert.format(amt)),
                    b -> AlertManager.push(new Alert("TestUser", amt)))
                .bounds(x, y + row * 24, w, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> this.onClose())
            .bounds(cx - 40, y + 54, 80, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int cx = this.width / 2;
        int top = this.height / 2 - 92;
        int size = 64;

        // skin head with purple border
        g.fill(cx - size / 2 - 3, top - 3, cx + size / 2 + 3, top + size + 3, 0xFFB366FF);
        g.fill(cx - size / 2 - 1, top - 1, cx + size / 2 + 1, top + size + 1, 0xFF000000);
        DonoAlertsClient.drawHead(g, cx - size / 2, top, size);

        // Welcome text at 2x
        String welcome = "Welcome " + DonoAlertsClient.CONFIG.displayName;
        g.pose().pushMatrix();
        g.pose().translate(cx, top + size + 10);
        g.pose().scale(2f, 2f);
        g.text(this.font, welcome, -this.font.width(welcome) / 2, 0, 0xFFFFFFFF, true);
        g.pose().popMatrix();

        String stats = "This session: " + DonoAlertsClient.sessionCount + " payments, $"
            + Alert.format(DonoAlertsClient.sessionTotal);
        g.text(this.font, stats, cx - this.font.width(stats) / 2, this.height / 2 + 8, 0xFFAAAAAA, true);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
