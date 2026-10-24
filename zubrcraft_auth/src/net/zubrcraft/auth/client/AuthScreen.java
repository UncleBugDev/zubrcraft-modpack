package net.zubrcraft.auth.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.zubrcraft.auth.ZubrCraftAuth;
import net.zubrcraft.auth.network.AuthLeavePacket;
import net.zubrcraft.auth.network.AuthSubmitPacket;

public class AuthScreen extends Screen {
    private int mode; // 0 = REGISTER, 1 = LOGIN
    private String statusMessage;

    private EditBox passwordBox;
    private EditBox confirmBox;
    private Button submitButton;
    private Button leaveButton;
    private Button toggleShowButton;
    private boolean showPassword = false;

    public AuthScreen(int mode, String message) {
        super(Component.m_237113_("ZubrCraft Auth"));
        this.mode = mode;
        this.statusMessage = (message != null && !message.isEmpty()) ? message : (mode == 0 ? "§7Придумайте пароль для защиты аккаунта:" : "§7Введите ваш пароль для входа:");
    }

    public void updatePrompt(int mode, String message) {
        this.mode = mode;
        if (message != null && !message.isEmpty()) {
            this.statusMessage = message;
        }
        this.m_7856_();
    }

    public void setStatusMessage(String message) {
        this.statusMessage = message != null ? message : "";
    }

    @Override
    protected void m_7856_() { // init
        this.m_169413_(); // clearWidgets

        int centerX = this.f_96543_ / 2;
        int centerY = this.f_96544_ / 2;
        int totalW = 200;
        int boxH = 20;

        int startY = centerY - (this.mode == 0 ? 82 : 62);

        // Password input box (width 150)
        this.passwordBox = new EditBox(this.f_96547_, centerX - totalW / 2, startY + 50, 148, boxH, Component.m_237113_("Пароль"));
        this.passwordBox.m_94199_(64);
        setupMasking(this.passwordBox);
        this.m_142416_(this.passwordBox);

        // Toggle show password button (width 48, text "Показ" / "Скрыть")
        this.toggleShowButton = Button.m_253074_(Component.m_237113_(this.showPassword ? "Скрыть" : "Показ"), b -> {
            this.showPassword = !this.showPassword;
            b.m_93666_(Component.m_237113_(this.showPassword ? "Скрыть" : "Показ"));
        }).m_252987_(centerX - totalW / 2 + 152, startY + 50, 48, 20).m_253136_();
        this.m_142416_(this.toggleShowButton);

        int currentY = startY + 50 + boxH + 8;

        if (this.mode == 0) { // REGISTER: confirm password box (full 200 width)
            this.confirmBox = new EditBox(this.f_96547_, centerX - totalW / 2, currentY + 12, totalW, boxH, Component.m_237113_("Повторите пароль"));
            this.confirmBox.m_94199_(64);
            setupMasking(this.confirmBox);
            this.m_142416_(this.confirmBox);
            currentY += 12 + boxH + 12;
        } else {
            this.confirmBox = null;
            currentY += 12;
        }

        // Submit button
        Component submitText = Component.m_237113_(this.mode == 0 ? "Зарегистрироваться" : "Войти на сервер");
        this.submitButton = Button.m_253074_(submitText, b -> doSubmit()).m_252987_(centerX - totalW / 2, currentY, totalW, boxH).m_253136_();
        this.m_142416_(this.submitButton);
        currentY += boxH + 6;

        // Leave button
        this.leaveButton = Button.m_253074_(Component.m_237113_("Отключиться"), b -> doLeave()).m_252987_(centerX - totalW / 2, currentY, totalW, boxH).m_253136_();
        this.m_142416_(this.leaveButton);

        // Initial focus on passwordBox
        this.passwordBox.m_93692_(true);
        this.m_7522_(this.passwordBox);
    }

    private void setupMasking(EditBox box) {
        box.m_94149_((text, cursor) -> {
            if (this.showPassword) {
                return FormattedCharSequence.m_13714_(text, Style.f_131099_);
            } else {
                return FormattedCharSequence.m_13714_("*".repeat(text.length()), Style.f_131099_);
            }
        });
    }

    private void doSubmit() {
        String pass = this.passwordBox.m_94155_();
        String conf = this.confirmBox != null ? this.confirmBox.m_94155_() : "";

        if (pass == null || pass.trim().isEmpty()) {
            this.statusMessage = "§cВведите пароль!";
            return;
        }
        if (this.mode == 0) {
            if (pass.length() < 4) {
                this.statusMessage = "§cПароль должен быть от 4 символов!";
                return;
            }
            if (!pass.equals(conf)) {
                this.statusMessage = "§cПароли не совпадают!";
                return;
            }
        }
        this.statusMessage = "§eПроверка...";
        ZubrCraftAuth.CHANNEL.sendToServer(new AuthSubmitPacket(this.mode, pass, conf));
    }

    private void doLeave() {
        ZubrCraftAuth.CHANNEL.sendToServer(new AuthLeavePacket());
    }

    @Override
    public boolean m_5534_(char codePoint, int modifiers) { // charTyped
        if (this.passwordBox != null && this.passwordBox.m_93696_()) {
            return this.passwordBox.m_5534_(codePoint, modifiers);
        }
        if (this.confirmBox != null && this.confirmBox.m_93696_()) {
            return this.confirmBox.m_5534_(codePoint, modifiers);
        }
        return super.m_5534_(codePoint, modifiers);
    }

    @Override
    public boolean m_7933_(int keyCode, int scanCode, int modifiers) { // keyPressed
        if (keyCode == 257 || keyCode == 335) { // Enter or NumPad Enter
            doSubmit();
            return true;
        }
        if (keyCode == 258) { // Tab
            if (this.mode == 0 && this.confirmBox != null) {
                if (this.passwordBox.m_93696_()) {
                    this.passwordBox.m_93692_(false);
                    this.confirmBox.m_93692_(true);
                    this.m_7522_(this.confirmBox);
                } else {
                    this.confirmBox.m_93692_(false);
                    this.passwordBox.m_93692_(true);
                    this.m_7522_(this.passwordBox);
                }
                return true;
            }
        }
        if (keyCode == 256) { // Esc
            return true; // Ignore Esc to keep screen open
        }
        if (this.passwordBox != null && this.passwordBox.m_93696_()) {
            if (this.passwordBox.m_7933_(keyCode, scanCode, modifiers)) return true;
        }
        if (this.confirmBox != null && this.confirmBox.m_93696_()) {
            if (this.confirmBox.m_7933_(keyCode, scanCode, modifiers)) return true;
        }
        return super.m_7933_(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean m_6913_() { // shouldCloseOnEsc
        return false;
    }

    @Override
    public void m_86600_() { // tick
        super.m_86600_();
        if (this.passwordBox != null) this.passwordBox.m_94120_();
        if (this.confirmBox != null) this.confirmBox.m_94120_();
    }

    @Override
    public void m_88315_(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) { // render
        this.m_280273_(guiGraphics); // renderBackground

        int centerX = this.f_96543_ / 2;
        int centerY = this.f_96544_ / 2;
        int cardW = 240;
        int cardH = this.mode == 0 ? 210 : 170;
        int cardX = centerX - cardW / 2;
        int cardY = centerY - cardH / 2;

        // Card background (dark semi-transparent slate)
        guiGraphics.m_280509_(cardX, cardY, cardX + cardW, cardY + cardH, 0xEE121318);
        // Card golden top accent line
        guiGraphics.m_280509_(cardX, cardY, cardX + cardW, cardY + 2, 0xFFFFB300);
        // Card border (1px outline)
        guiGraphics.m_280509_(cardX, cardY + 2, cardX + 1, cardY + cardH, 0x44FFFFFF);
        guiGraphics.m_280509_(cardX + cardW - 1, cardY + 2, cardX + cardW, cardY + cardH, 0x44FFFFFF);
        guiGraphics.m_280509_(cardX, cardY + cardH - 1, cardX + cardW, cardY + cardH, 0x44FFFFFF);

        // Header Title
        guiGraphics.m_280137_(this.f_96547_, "★ ZubrCraft ★", centerX, cardY + 10, 0xFFFFB300);

        // Mode Title
        String modeTitle = this.mode == 0 ? "РЕГИСТРАЦИЯ АККАУНТА" : "АВТОРИЗАЦИЯ";
        guiGraphics.m_280137_(this.f_96547_, modeTitle, centerX, cardY + 22, 0xFFFFFFFF);

        // Status message
        if (this.statusMessage != null && !this.statusMessage.isEmpty()) {
            guiGraphics.m_280137_(this.f_96547_, this.statusMessage, centerX, cardY + 34, 0xFFEEEEEE);
        }

        // Labels
        int startY = centerY - (this.mode == 0 ? 82 : 62);
        guiGraphics.m_280056_(this.f_96547_, "Пароль:", centerX - 100, startY + 39, 0xFFAAAAAA, false);

        if (this.mode == 0) {
            guiGraphics.m_280056_(this.f_96547_, "Повторите пароль:", centerX - 100, startY + 79, 0xFFAAAAAA, false);
        }

        super.m_88315_(guiGraphics, mouseX, mouseY, partialTick);
    }
}
